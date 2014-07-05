package crawler;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.dispatch.OnComplete;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import akka.util.Timeout;
import crawler.messages.DomainFinished;
import crawler.messages.ProcessNext;
import crawler.messages.StartCrawl;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.net.URI;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;

/**
 *
 */
public class DomainCrawler extends AbstractActor {
    public static final int MAX_URLS = 100;
    private final LoggingAdapter log = Logging.getLogger(context().system(), this);
    private LinkedList<URI> queue = new LinkedList<>();
    private HashSet<URI> knownUrls = new HashSet<>();
    private int urlsQueued = 0;

    static Props props(ActorRef crawlerManager) {
        return Props.create(DomainCrawler.class, () -> new DomainCrawler(crawlerManager));
    }

    public DomainCrawler(ActorRef crawlerManager) {
        final ActorRef downloader = context().actorOf(Props.create(LinkExtractor.class, crawlerManager), "extractor");
        receive(ReceiveBuilder
                .match(URI.class, uri -> {
                    if (urlsQueued > MAX_URLS) {
                        return;
                    }

                    if (!knownUrls.contains(uri)) {
                        knownUrls.add(uri);
                        queue.addLast(uri);
                        urlsQueued++;
                    }
                })
                .match(ProcessNext.class, msg -> {
                    if (queue.isEmpty()) {
                        return;
                    }

                    final Future<Object> f = ask(downloader, queue.removeFirst(),
                            new Timeout(Duration.create(30, TimeUnit.SECONDS)));
                    f.onComplete(new OnComplete<Object>() {
                        @Override
                        public void onComplete(Throwable throwable, Object o) throws Throwable {
                            if (throwable != null) {
                                log.warning("Couldn't download page: " + throwable);
                            }

                            if (queue.isEmpty()) {
                                log.info("Finished crawling " + self());
                                crawlerManager.tell(new DomainFinished(), self());
                            } else {
                                // Schedule next page crawl
                                context().system().scheduler().scheduleOnce(Duration.create(1, TimeUnit.SECONDS),
                                        self(), new ProcessNext(), context().system().dispatcher(), null);
                            }
                        }
                    }, context().system().dispatcher());
                })
                .match(StartCrawl.class, m -> {
                    next();
                })
                .matchAny(this::unhandled).build());
    }

    private void next() {
        self().tell(new ProcessNext(), self());
    }
}

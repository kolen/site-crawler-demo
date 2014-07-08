package crawler;

import akka.actor.*;
import akka.dispatch.OnComplete;
import akka.japi.pf.DeciderBuilder;
import akka.japi.pf.ReceiveBuilder;
import akka.util.Timeout;
import crawler.messages.DomainFinished;
import crawler.messages.ProcessNext;
import crawler.messages.ReadyForNext;
import crawler.messages.StartCrawl;
import org.jsoup.HttpStatusException;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;

/**
 *
 */
public class DomainCrawler extends AbstractLoggingActor {
    public static final int MAX_URLS = 100;
    private LinkedList<URI> queue = new LinkedList<>();
    private HashSet<URI> knownUrls = new HashSet<>();
    private int urlsQueued = 0;

    private SupervisorStrategy strategy = new OneForOneStrategy(3, Duration.create("10 seconds"),
            DeciderBuilder
                    .match(HttpStatusException.class, e -> {
                        if (e.getStatusCode() >= 500 && e.getStatusCode() <= 599) {
                            return SupervisorStrategy.restart();
                        } else {
                            return SupervisorStrategy.stop();
                        }
                    })
                    .match(SocketTimeoutException.class, e -> SupervisorStrategy.restart())
                    .match(Throwable.class, e -> {
                        log().error("Page crawler error: " + e);
                        return SupervisorStrategy.restart();
                    }).build());

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    static Props props(ActorRef crawlerManager) {
        return Props.create(DomainCrawler.class, () -> new DomainCrawler(crawlerManager));
    }

    public DomainCrawler(ActorRef crawlerManager) {
        final ActorRef downloader = context().actorOf(Props.create(LinkExtractor.class, crawlerManager), "extractor");
        receive(ReceiveBuilder
                // URI received to add to crawl queue
                .match(URI.class, uri -> {
                    if (urlsQueued > MAX_URLS) {
                        // Tell again that it is finished even it may already know it
                        crawlerManager.tell(new DomainFinished(), self());
                        return;
                    }

                    if (!knownUrls.contains(uri)) {
                        knownUrls.add(uri);
                        queue.addLast(uri);
                        urlsQueued++;
                    }
                })
                // Last page is finished, ready to crawl next page (after delay) or to finish if queue is empty
                .match(ReadyForNext.class, msg -> {
                    if (queue.isEmpty()) {
                        log().info("Finished crawling " + self());
                        crawlerManager.tell(new DomainFinished(), self());
                    } else {
                        // Schedule next page crawl
                        context().system().scheduler().scheduleOnce(Duration.create(1, TimeUnit.SECONDS),
                                self(), new ProcessNext(), context().system().dispatcher(), null);
                    }
                })
                // Going to crawl next page
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
                                log().warning("Couldn't download page: " + throwable);
                            }
                            self().tell(new ReadyForNext(), self());
                        }
                    }, context().system().dispatcher());
                })
                // Start crawl if not yet started
                .match(StartCrawl.class, m -> {
                    next();
                })
                .matchAny(this::unhandled).build());
    }

    private void next() {
        self().tell(new ProcessNext(), self());
    }
}

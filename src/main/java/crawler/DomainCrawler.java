package crawler;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.dispatch.OnComplete;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import akka.util.Timeout;
import crawler.messages.FoundLink;
import crawler.messages.ProcessNext;
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
    private final LoggingAdapter log = Logging.getLogger(context().system(), this);
    private LinkedList<URI> queue = new LinkedList<>();
    private HashSet<URI> knownUrls = new HashSet<>();

    static Props props(ActorRef linkRegistry) {
        return Props.create(DomainCrawler.class, () -> new DomainCrawler(linkRegistry));
    }

    public DomainCrawler(ActorRef linkRegistry) {
        final ActorRef downloader = context().actorOf(Props.create(PageDownloader.class, linkRegistry));
        receive(ReceiveBuilder
                .match(FoundLink.class, msg -> {
                    final URI found_uri = msg.getUri();
                    if (!knownUrls.contains(found_uri)) {
                        knownUrls.add(found_uri);
                        queue.addLast(found_uri);
                    }
                })
                .match(ProcessNext.class, msg -> {
                    final Future<Object> f = ask(downloader, queue.removeFirst(),
                            new Timeout(Duration.create(30, TimeUnit.SECONDS)));
                    f.onComplete(new OnComplete<Object>() {
                        @Override
                        public void onComplete(Throwable throwable, Object o) throws Throwable {
                            if (throwable != null) {
                                log.warning("Couldn't download page: "+throwable);
                            }
                            context().system().scheduler().scheduleOnce(Duration.create(1, TimeUnit.SECONDS),
                                    self(), new ProcessNext(), context().system().dispatcher(), null);
                        }
                    }, context().system().dispatcher());
                })
                .matchAny(this::unhandled).build());

        self().tell(new ProcessNext(), self());
    }
}

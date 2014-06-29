package crawler;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;

import java.net.URI;
import java.util.HashMap;

/**
 *
 */
public class CrawlerManager extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(context().system(), this);
    private HashMap<String, ActorRef> domainCrawlers = new HashMap<>();
    private ActorRef linkRegistry;

    public CrawlerManager() {
        linkRegistry = context().actorOf(Props.create(LinkRegistry.class));

        receive(ReceiveBuilder
                .match(URI.class, uri -> {
                    final String host = uri.getHost();
                    if (!domainCrawlers.containsKey(host)) {
                        ActorRef crawler = context().actorOf(Props.create(DomainCrawler.class, self()), host);
                        crawler.tell(uri, self());
                        domainCrawlers.put(host, crawler);
                    }
                    linkRegistry.tell(uri, self());
                })
                .matchAny(this::unhandled).build());
    }
}

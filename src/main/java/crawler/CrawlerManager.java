package crawler;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import akka.util.Timeout;
import crawler.messages.AddDomain;
import crawler.messages.DomainFinished;
import crawler.messages.DumpLinks;
import crawler.messages.LinksList;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;

/**
 *
 */
public class CrawlerManager extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(context().system(), this);
    private HashMap<String, ActorRef> domainCrawlers = new HashMap<>();
    private HashSet<ActorRef> workingCrawlers = new HashSet<>();
    private ActorRef linkRegistry;

    public CrawlerManager() {
        linkRegistry = context().actorOf(Props.create(LinkRegistry.class));

        receive(ReceiveBuilder
                .match(AddDomain.class, addDomain -> {
                    final String host = addDomain.getDomain();
                    final URI initialUri = new URI("http", addDomain.getDomain(), "", "");
                    if (!domainCrawlers.containsKey(host)) {
                        ActorRef crawler = context().actorOf(Props.create(DomainCrawler.class, self()), host);
                        crawler.tell(initialUri, self());
                        domainCrawlers.put(host, crawler);
                        workingCrawlers.add(crawler);
                    }
                })
                .match(URI.class, uri -> {
                    final String host = uri.getHost();
                    ActorRef crawler = domainCrawlers.get(host);
                    if (crawler != null) {
                        workingCrawlers.add(crawler);
                        crawler.tell(uri, self());
                    }
                })
                .match(DomainFinished.class, m -> {
                    workingCrawlers.remove(sender());
                    if (workingCrawlers.isEmpty()) {
                        log.info("All domains finished");
                        final Future<Object> ask = ask(linkRegistry, new DumpLinks(),
                                new Timeout(Duration.create(1, TimeUnit.MINUTES)));
                        ask.onSuccess(new OnSuccess<Object>() {
                            @Override
                            public void onSuccess(Object o) throws Throwable {
                                System.out.println(((LinksList)o).getLinks());
                            }
                        }, context().system().dispatcher());
                    }
                })
                .matchAny(this::unhandled).build());
    }
}

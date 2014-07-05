package crawler;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import akka.util.Timeout;
import crawler.messages.AddDomain;
import crawler.messages.DomainFinished;
import crawler.messages.DumpLinks;
import crawler.messages.StartCrawl;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;
import static akka.pattern.Patterns.pipe;

/**
 *
 */
public class CrawlerManager extends AbstractActor {
    public static final FiniteDuration LINK_EXTRACTOR_WAIT_DELAY = Duration.create(10, TimeUnit.SECONDS);
    public static final FiniteDuration DUMP_LINKS_TIMEOUT = Duration.create(1, TimeUnit.MINUTES);
    private final LoggingAdapter log = Logging.getLogger(context().system(), this);
    private HashMap<String, ActorRef> domainCrawlers = new HashMap<>();
    private HashSet<ActorRef> workingCrawlers = new HashSet<>();
    private ActorRef linkRegistry;
    private ActorRef startInitiator; // Actor that initiated crawl start

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
                    linkRegistry.tell(uri, self());
                })
                .match(DomainFinished.class, m -> {
                    workingCrawlers.remove(sender());
                    log.info("Crawlers left: "+workingCrawlers.size());
                    if (workingCrawlers.isEmpty()) {
                        log.info("All domains finished, waiting for extractors to finish");

                        context().system().scheduler().scheduleOnce(LINK_EXTRACTOR_WAIT_DELAY,
                                () -> {
                                    log.info("Dumping links");
                                    final Future<Object> ask = ask(linkRegistry, new DumpLinks(),
                                            new Timeout(DUMP_LINKS_TIMEOUT));
                                    pipe(ask, context().system().dispatcher()).to(startInitiator);
                                }, context().system().dispatcher());
                    }
                })
                .match(StartCrawl.class, m -> {
                    startInitiator = sender();
                    for (ActorRef crawler : domainCrawlers.values()) {
                        crawler.tell(new StartCrawl(), self());
                    }
                })
                .matchAny(this::unhandled).build());
    }
}

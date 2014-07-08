package crawler;

import akka.actor.*;
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
    public static final FiniteDuration DUMP_LINKS_TIMEOUT = Duration.create(1, TimeUnit.MINUTES);
    private final LoggingAdapter log = Logging.getLogger(context().system(), this);
    private HashMap<String, ActorRef> domainCrawlers = new HashMap<>();
    private HashSet<ActorRef> workingCrawlers = new HashSet<>();
    private ActorRef linkCollector;
    private ActorRef startInitiator; // Actor that initiated crawl start

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return SupervisorStrategy.stoppingStrategy();
    }

    public CrawlerManager() {
        linkCollector = context().actorOf(Props.create(LinkCollector.class));

        receive(ReceiveBuilder
                .match(AddDomain.class, addDomain -> {
                    final String host = addDomain.getDomain();
                    final URI initialUri = new URI("http", addDomain.getDomain(), "", "");
                    if (!domainCrawlers.containsKey(host)) {
                        log.info("Creating crawler for domain " + host);
                        ActorRef crawler = context().actorOf(Props.create(DomainCrawler.class, self()), host);
                        context().watch(crawler);
                        crawler.tell(initialUri, self());
                        domainCrawlers.put(host, crawler);
                        workingCrawlers.add(crawler);
                    }
                })
                .match(URI.class, uri -> {
                    final String host = uri.getHost();
                    ActorRef crawler = domainCrawlers.get(host);
                    if (crawler != null) {
//                        workingCrawlers.add(crawler);
                        crawler.tell(uri, self());
                    }
                    linkCollector.tell(uri, self());
                })
                .match(DomainFinished.class, m -> {
                    final ActorRef finishedDomainCrawler = sender();
                    log.info("Finished from " + finishedDomainCrawler);
                    domainFinished(finishedDomainCrawler);
                })
                .match(StartCrawl.class, m -> {
                    startInitiator = sender();
                    for (ActorRef crawler : domainCrawlers.values()) {
                        crawler.tell(new StartCrawl(), self());
                    }
                })
                .match(Terminated.class, t -> {
                    log.error("Domain crawler crashed: "+t);
                    domainFinished(t.actor());
                })
                .matchAny(this::unhandled).build());
    }

    private void domainFinished(ActorRef finishedDomainCrawler) {
        workingCrawlers.remove(finishedDomainCrawler);
        log.info("Crawlers left: " + workingCrawlers.size());
        if (workingCrawlers.size() < 3) {
            for (ActorRef workingCrawler : workingCrawlers) {
                log.info("Crawler: " + workingCrawler);
            }
        }

        if (workingCrawlers.isEmpty()) {
            log.info("Dumping links");
            final Future<Object> ask = ask(linkCollector, new DumpLinks(),
                    new Timeout(DUMP_LINKS_TIMEOUT));
            pipe(ask, context().dispatcher()).to(startInitiator);
        }
    }
}

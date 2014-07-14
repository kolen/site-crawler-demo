package crawler;

import akka.actor.*;
import akka.dispatch.OnComplete;
import akka.japi.pf.DeciderBuilder;
import akka.japi.pf.ReceiveBuilder;
import akka.util.Timeout;
import crawler.messages.*;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.net.URI;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;
import static crawler.Utils.truncateFragment;

/**
 *
 */
public class DomainCrawler extends AbstractLoggingActor {
    public static final int MAX_URLS = 100;
    public static final int EXTRACTOR_REPLY_TIMEOUT = 30;
    private final LinkedList<URI> queue = new LinkedList<>();
    private final HashSet<URI> knownUrls = new HashSet<>();
    private int urlsQueued = 0;
    private int pagesCrawled = 0;
    private int pagesSuccessful = 0;

    private enum Status {
        JUST_CREATED, PROCESSING_FIRST_PAGE, WAITING_BEFORE_NEXT_PAGE, PROCESSING_PAGE, IDLE
    }
    private Status status = Status.JUST_CREATED;

    private SupervisorStrategy strategy = new OneForOneStrategy(3, Duration.create("10 seconds"),
            DeciderBuilder
                    .match(Throwable.class, e -> {
                        log().error("Page crawler error: " + e);
                        return SupervisorStrategy.restart();
                    }).build());

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    static Props props(ActorRef crawlerManager, String domain) {
        return Props.create(DomainCrawler.class, () -> new DomainCrawler(crawlerManager, domain));
    }

    public DomainCrawler(ActorRef crawlerManager, String domain) {
        receive(ReceiveBuilder
                // URI received to add to crawl queue
                .match(URI.class, uri -> {
                    if (urlsQueued >= MAX_URLS) {
                        return;
                    }

                    if (!knownUrls.contains(uri)) {
                        URI uri_without_fragment = truncateFragment(uri);
                        knownUrls.add(uri_without_fragment);
                        queue.addLast(uri_without_fragment);
                        urlsQueued++;
                    }
                })
                // URI after all redirects found after crawl
                .match(ActualURI.class, a -> {
                    knownUrls.add(a.getActualURI());
                })
                // Last page is finished, ready to crawl next page (after delay) or to finish if queue is empty
                .match(ReadyForNext.class,
                        msg -> status == Status.PROCESSING_PAGE || status == Status.PROCESSING_FIRST_PAGE, msg -> {
                    pagesCrawled++;
                    if (msg.isLastSuccess()) {
                        pagesSuccessful++;
                    }

                    if (queue.isEmpty()) {
                        log().info("Finished crawling " + self());
                        CrawlResult.DomainSummary summary = new CrawlResult.DomainSummary(
                                domain, pagesCrawled, pagesSuccessful);
                        crawlerManager.tell(new DomainFinished(summary), self());
                        status = Status.IDLE;
                    } else {
                        // Schedule next page crawl
                        context().system().scheduler().scheduleOnce(Duration.create(1, TimeUnit.SECONDS),
                                self(), new ProcessNext(), context().system().dispatcher(), null);
                        status = Status.WAITING_BEFORE_NEXT_PAGE;
                    }
                })
                // Going to crawl next page
                .match(ProcessNext.class, msg -> status == Status.WAITING_BEFORE_NEXT_PAGE, msg -> {
                    if (queue.isEmpty()) {
                        return;
                    }

                    final ActorRef downloader = context().actorOf(Props.create(LinkExtractor.class, crawlerManager));
                    final Future<Object> f = ask(downloader, new CrawlPage(queue.removeFirst()),
                            new Timeout(Duration.create(EXTRACTOR_REPLY_TIMEOUT, TimeUnit.SECONDS)));

                    f.onComplete(new OnComplete<Object>() {
                        @Override
                        public void onComplete(Throwable throwable, Object o) throws Throwable {
                            if (throwable != null) {
                                // TODO: currently only triggers on timeout, should trigger instantly on error
                                log().warning("Couldn't download page: " + throwable);
                            }
                            self().tell(new ActualURI(((FinishedDownloading)o).getActualURI()), self());
                            self().tell(new ReadyForNext(throwable == null), self());
                        }
                    }, context().system().dispatcher());
                    status = Status.PROCESSING_PAGE;
                })
                // Start crawl if not yet started
                .match(StartCrawl.class, msg -> status == Status.JUST_CREATED, m -> {
                    URI startingUri = new URI("http", domain, "", "");

                    final ActorRef downloader = context().actorOf(Props.create(LinkExtractor.class, crawlerManager));
                    final Future<Object> f = ask(downloader, new CrawlPage(startingUri, true),
                            new Timeout(Duration.create(EXTRACTOR_REPLY_TIMEOUT, TimeUnit.SECONDS)));

                    ActorRef self = self();
                    f.onComplete(new OnComplete<Object>() {
                        @Override
                        public void onComplete(Throwable throwable, Object o) throws Throwable {
                            if (throwable != null) {
                                log().error("Couldn't download starting page for " + domain + ": " + throwable);
                                crawlerManager.tell(new DomainFinished(new CrawlResult.DomainSummary(domain, 1, 0)),
                                        self);
                                self().tell(new ActualURI(((FinishedDownloading)o).getActualURI()), self());
                            } else {
                                self.tell(new ReadyForNext(true), self);
                            }
                        }
                    }, context().dispatcher());

                    status = Status.PROCESSING_FIRST_PAGE;
                    urlsQueued++;
                })
                .matchAny(this::unhandled).build());
    }
}

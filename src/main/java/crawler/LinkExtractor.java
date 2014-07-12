package crawler;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import crawler.messages.CrawlPage;
import crawler.messages.FinishedDownloading;
import crawler.messages.SynonymFound;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.net.URISyntaxException;

/**
 *
 */
public class LinkExtractor extends AbstractActor {
    /** "request timeouts (connect and read)" */
    public static final int HTTP_TIMEOUT = 1500;
    private final LoggingAdapter log = Logging.getLogger(context().system(), this);

    static Props props(ActorRef crawlerManager) {
        return Props.create(LinkExtractor.class, () -> new LinkExtractor(crawlerManager));
    }

    public LinkExtractor(ActorRef crawlerManager) {
        receive(ReceiveBuilder
                .match(CrawlPage.class, cp -> {
                    Document doc;
                    try {
                        doc = Jsoup.connect(cp.getUri().toString()).timeout(HTTP_TIMEOUT).get();
                    } catch (HttpStatusException e) {
                        // Non-transient HTTP errors - immediately signal failure
                        if (e.getStatusCode() < 500) {
                            crawlerManager.tell(new Status.Failure(e), self());
                            self().tell(PoisonPill.getInstance(), self());
                            return;
                        } else {
                            // Re-throw other errors so supervision will handle it
                            throw e;
                        }
                    }
                    log.info("Downloaded " + doc.location());
                    final Elements links = doc.select("a");

                    if (cp.isFirst()) {
                        URI newLocation = new URI(doc.location());
                        final String oldHost = cp.getUri().getHost();
                        final String newHost = newLocation.getHost();
                        if (!newHost.equals(oldHost)) {
                            crawlerManager.tell(new SynonymFound(oldHost, newHost), self());
                            log.info("Found synonym for "+oldHost+": "+newHost);
                        }
                    }

                    for (Element a : links) {
                        final String href = a.attr("abs:href");
                        if (href != null && !href.equals("")) {
                            try {
                                final URI found_uri = new URI(href);
                                if (found_uri.getScheme().equals("http") || found_uri.getScheme().equals("https")) {
                                    crawlerManager.tell(found_uri, self());
                                }
                            } catch (URISyntaxException e) {
                                // ignore bad urls
                                // TODO: urls such as https://maps.google.com/maps?q=Zhytomyr,+10014,+Kyivska st.+47
                                // parsed as bad, should not be ignored
                                log.warning("Invalid URL: "+href);
                            }
                        }
                    }
                    sender().tell(new FinishedDownloading(), self());
                    self().tell(PoisonPill.getInstance(), self());
                })
                .matchAny(this::unhandled).build());
    }
}


package crawler;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Status;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import crawler.messages.FinishedDownloading;
import crawler.messages.PageContent;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import scala.Option;

import java.net.URI;
import java.net.URISyntaxException;

/**
 *
 */
public class LinkExtractor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(context().system(), this);

    static Props props(ActorRef crawlerManager) {
        return Props.create(LinkExtractor.class, () -> new LinkExtractor(crawlerManager));
    }

    public LinkExtractor(ActorRef crawlerManager) {
        receive(ReceiveBuilder
                .match(URI.class, uri -> {
                    final Document doc = Jsoup.connect(uri.toString()).get();
                    log.info("Downloaded " + doc.location());
                    final Elements links = doc.select("a");
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
                })
                .matchAny(this::unhandled).build());
    }

    @Override
    public void preRestart(Throwable reason, Option<Object> message) throws Exception {
        if (!(message.isEmpty()) && message.get() instanceof URI) {
            // Re-schedule current page on restart
            sender().tell(new Status.Failure(reason), self());
            sender().tell(message, self());
        }
        super.preRestart(reason, message);
    }
}


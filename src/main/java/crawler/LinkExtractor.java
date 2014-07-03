package crawler;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import crawler.messages.PageContent;
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
    private final LoggingAdapter log = Logging.getLogger(context().system(), this);

    static Props props(ActorRef crawlerManager) {
        return Props.create(LinkExtractor.class, () -> new LinkExtractor(crawlerManager));
    }

    public LinkExtractor(ActorRef crawlerManager) {
        receive(ReceiveBuilder
                .match(PageContent.class, msg -> {
                    final Document doc = Jsoup.parse(msg.getPageContent(), msg.getBaseURI().toString());
                    final Elements links = doc.select("a");
                    for (Element a : links) {
                        final String href = a.attr("abs:href");
                        if (href != null && !href.equals("")) {
                            try {
                                final URI uri = new URI(href);
                                if (uri.getScheme().equals("http") || uri.getScheme().equals("https")) {
                                    crawlerManager.tell(uri, self());
                                }
                            } catch (URISyntaxException e) {
                                // ignore bad urls
                                // TODO: urls such as https://maps.google.com/maps?q=Zhytomyr,+10014,+Kyivska st.+47
                                // parsed as bad, should not be ignored
                                log.warning("Invalid URL: "+href);
                            }
                        }
                    }
                })
                .matchAny(this::unhandled).build());
    }
}

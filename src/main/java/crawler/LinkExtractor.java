package crawler;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import crawler.messages.PageContent;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;

/**
 *
 */
public class LinkExtractor extends AbstractActor {
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
                            crawlerManager.tell(new URI(href), self());
                        }
                    }
                })
                .matchAny(this::unhandled).build());
    }
}

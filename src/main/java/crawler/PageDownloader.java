package crawler;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import crawler.messages.PageContent;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.net.URI;

/**
 *
 */
public class PageDownloader extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(context().system(), this);

    static Props props(ActorRef linkRegistry) {
        return Props.create(PageDownloader.class, () -> new PageDownloader(linkRegistry));
    }

    public PageDownloader(ActorRef linkRegistry) {
        final ActorRef extractor = getContext().actorOf(Props.create(LinkExtractor.class, linkRegistry));

        receive(ReceiveBuilder
                .match(URI.class, uri -> {
                    final CloseableHttpClient httpClient = HttpClients.createDefault();
                    final HttpGet httpGet = new HttpGet(uri);
                    final CloseableHttpResponse response = httpClient.execute(httpGet);
                    log.info("Get page "+response);
                    final String s = EntityUtils.toString(response.getEntity());
                    response.close();
                    extractor.tell(new PageContent(s), self());
                })
                .matchAny(this::unhandled).build()
        );
    }
}

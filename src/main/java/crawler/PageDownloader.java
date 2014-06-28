package crawler;

import akka.actor.AbstractActor;
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

    public PageDownloader() {
        receive(ReceiveBuilder.
                        match(URI.class, uri -> {
                            final CloseableHttpClient httpClient = HttpClients.createDefault();
                            final HttpGet httpGet = new HttpGet(uri);
                            final CloseableHttpResponse response = httpClient.execute(httpGet);
                            log.info("Get page "+response);
                            final String s = EntityUtils.toString(response.getEntity());
                            response.close();
                            sender().tell(new PageContent(s), self());
                        }).
                        matchAny(this::unhandled).build()
        );
    }
}

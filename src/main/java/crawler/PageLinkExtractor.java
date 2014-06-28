package crawler;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import crawler.messages.FoundLink;

import java.net.URI;
import java.util.HashSet;
import java.util.LinkedList;

/**
 *
 */
public class PageLinkExtractor extends UntypedActor {
    private final LoggingAdapter log = Logging.getLogger(context().system(), this);
    private LinkedList<URI> queue = new LinkedList<>();
    private HashSet<URI> knownUrls = new HashSet<>();
    private ActorRef downloader = getContext().actorOf(Props.create(PageDownloader.class));

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof FoundLink) {
            final URI found_uri = ((FoundLink)message).getUri();
            if (!knownUrls.contains(found_uri)) {
                knownUrls.add(found_uri);
                queue.addLast(found_uri);
            }
        } else {
            unhandled(message);
        }
    }
}

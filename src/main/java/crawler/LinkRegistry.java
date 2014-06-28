package crawler;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import crawler.messages.DumpLinks;
import crawler.messages.FoundLink;
import crawler.messages.LinksList;

import java.net.URI;
import java.util.HashSet;
import java.util.LinkedList;

/**
 *
 */
public class LinkRegistry extends UntypedActor {
    LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    HashSet<URI> links = new HashSet<>();

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof FoundLink) {
            log.debug("Received FoundLink: "+(FoundLink)message);
            links.add(((FoundLink) message).getUri());
        } else if (message instanceof DumpLinks) {
            LinkedList<URI> links_list = new LinkedList<>();
            links_list.addAll(links);
            getSender().tell(new LinksList(links_list), getSelf());
        } else {
            unhandled(message);
        }
    }
}

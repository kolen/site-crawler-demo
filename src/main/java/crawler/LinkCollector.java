package crawler;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import crawler.messages.DumpLinks;

import java.net.URI;
import java.util.HashSet;
import java.util.LinkedList;

/**
 *
 */
public class LinkCollector extends AbstractActor {
    LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    HashSet<URI> links = new HashSet<>();

    public LinkCollector() {
        receive(ReceiveBuilder
        .match(URI.class, uri -> {
            log.debug("Received uri: "+ uri);
            URI uriWithoutFragment = Utils.truncateFragment(uri);
            links.add(uriWithoutFragment);
        })
        .match(DumpLinks.class, msg -> {
            LinkedList<URI> links_list = new LinkedList<>();
            links_list.addAll(links);
            sender().tell(links_list, self());
        })
        .matchAny(this::unhandled).build());
    }
}

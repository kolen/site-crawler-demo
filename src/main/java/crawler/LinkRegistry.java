package crawler;

import akka.actor.AbstractActor;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import crawler.messages.DumpLinks;
import crawler.messages.FoundLink;
import crawler.messages.LinksList;

import java.net.URI;
import java.util.HashSet;
import java.util.LinkedList;

/**
 *
 */
public class LinkRegistry extends AbstractActor {
    LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    HashSet<URI> links = new HashSet<>();

    public LinkRegistry() {
        receive(ReceiveBuilder
        .match(FoundLink.class, msg -> {
            log.debug("Received FoundLink: "+ msg);
            links.add(msg.getUri());
        })
        .match(DumpLinks.class, msg -> {
            LinkedList<URI> links_list = new LinkedList<>();
            links_list.addAll(links);
            sender().tell(new LinksList(links_list), self());
        })
        .matchAny(this::unhandled).build());
    }
}

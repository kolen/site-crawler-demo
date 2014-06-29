package crawler;

import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;
import crawler.messages.PageContent;

/**
 *
 */
public class LinkExtractor extends AbstractActor {
    public LinkExtractor() {
        receive(ReceiveBuilder
                .match(PageContent.class, msg -> {

                })
                .matchAny(this::unhandled).build());
    }
}

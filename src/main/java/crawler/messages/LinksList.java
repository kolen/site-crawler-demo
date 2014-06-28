package crawler.messages;

import java.net.URI;
import java.util.LinkedList;

/**
 *
 */
public class LinksList {
    private LinkedList<URI> links;
    public LinksList(LinkedList<URI> links) {
        this.links = links;
    }

    public LinkedList<URI> getLinks() {
        return links;
    }
}

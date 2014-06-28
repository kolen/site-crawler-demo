package crawler.messages;

import java.net.URI;

/**
 * Message about new link discovered during crawl
 */
public class FoundLink {
    private URI uri;

    public URI getUri() {
        return uri;
    }

    public FoundLink(URI uri) {
        this.uri = uri;
    }
}

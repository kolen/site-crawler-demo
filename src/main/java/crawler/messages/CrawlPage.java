package crawler.messages;

import java.net.URI;

/**
 *
 */
public class CrawlPage {
    private URI uri;
    private boolean isFirst;

    public CrawlPage(URI uri, boolean isFirst) {
        this.uri = uri;
        this.isFirst = isFirst;
    }

    public CrawlPage(URI uri) {
        this.uri = uri;
        this.isFirst = false;
    }

    public URI getUri() {
        return uri;
    }

    /**
     * Is this page first to crawl on domain?
     */
    public boolean isFirst() {
        return isFirst;
    }
}

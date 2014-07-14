package crawler.messages;

import java.net.URI;

/**
 *
 */
public class FinishedDownloading {
    private URI actualURI;

    public URI getActualURI() {
        return actualURI;
    }

    public FinishedDownloading(URI actualURI) {
        this.actualURI = actualURI;
    }
}

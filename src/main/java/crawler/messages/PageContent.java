package crawler.messages;

import java.net.URI;

/**
 *
 */
public class PageContent {
    private String pageContent;
    private URI baseURI;

    public String getPageContent() {
        return pageContent;
    }

    public PageContent(String pageContent, URI baseURI) {
        this.pageContent = pageContent;
        this.baseURI = baseURI;
    }

    public URI getBaseURI() {
        return baseURI;
    }
}

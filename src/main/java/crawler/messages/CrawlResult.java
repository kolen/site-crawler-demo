package crawler.messages;

import java.net.URI;
import java.util.LinkedList;

/**
 *
 */
public class CrawlResult{
    private LinkedList<URI> links;
    private LinkedList<DomainSummary> domainSummaries;

    public CrawlResult(LinkedList<URI> links, LinkedList<DomainSummary> domainSummaries) {
        this.links = links;
        this.domainSummaries = domainSummaries;
    }

    public LinkedList<URI> getLinks() {
        return links;
    }

    public LinkedList<DomainSummary> getDomainSummaries() {
        return domainSummaries;
    }

    public static class DomainSummary {
        private String domain;
        private int pagesCrawled;
        private int pagesSuccessful;

        public DomainSummary(String domain, int pagesCrawled, int pagesSuccessful) {
            this.domain = domain;
            this.pagesCrawled = pagesCrawled;
            this.pagesSuccessful = pagesSuccessful;
        }

        public String getDomain() {
            return domain;
        }

        public int getPagesCrawled() {
            return pagesCrawled;
        }

        public int getPagesSuccessful() {
            return pagesSuccessful;
        }
    }
}

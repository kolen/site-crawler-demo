package crawler.messages;

/**
 *
 */
public class DomainFinished {
    private CrawlResult.DomainSummary summary;

    public CrawlResult.DomainSummary getSummary() {
        return summary;
    }

    public DomainFinished(CrawlResult.DomainSummary summary) {
        this.summary = summary;
    }
}

package crawler.messages;

/**
 *
 */
public class SynonymFound {
    private String originalDomain;
    private String domainSynonym;

    public SynonymFound(String originalDomain, String domainSynonym) {
        this.originalDomain = originalDomain;
        this.domainSynonym = domainSynonym;
    }

    public String getOriginalDomain() {
        return originalDomain;
    }

    public String getDomainSynonym() {
        return domainSynonym;
    }
}

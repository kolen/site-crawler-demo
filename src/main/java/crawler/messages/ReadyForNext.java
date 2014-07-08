package crawler.messages;

/**
 *
 */
public class ReadyForNext {
    private boolean lastSuccess;

    public boolean isLastSuccess() {
        return lastSuccess;
    }

    public ReadyForNext(boolean lastSuccess) {
        this.lastSuccess = lastSuccess;
    }
}

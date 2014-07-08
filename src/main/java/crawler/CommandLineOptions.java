package crawler;

import com.beust.jcommander.Parameter;

/**
 *
 */
public class CommandLineOptions {
    @Parameter(names = {"--domains", "-d"}, description = "File with domains to crawl (one per line)", required = true)
    private String inputDomainsFile;

    @Parameter(names = {"--output", "-o"}, description = "Links output file", required = true)
    private String outputFile;

    @Parameter(names = {"--summary", "-s"}, description = "Summary output file")
    private String summaryOutputFile;

    @Parameter(names = "--help", help = true)
    private boolean help;

    public String getInputDomainsFile() {
        return inputDomainsFile;
    }

    public void setInputDomainsFile(String inputDomainsFile) {
        this.inputDomainsFile = inputDomainsFile;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    public String getSummaryOutputFile() {
        return summaryOutputFile;
    }

    public void setSummaryOutputFile(String summaryOutputFile) {
        this.summaryOutputFile = summaryOutputFile;
    }
}

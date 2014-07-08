package crawler;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.util.Timeout;
import com.beust.jcommander.JCommander;
import crawler.messages.AddDomain;
import crawler.messages.CrawlResult;
import crawler.messages.StartCrawl;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static akka.pattern.Patterns.ask;

/**
 * 
 */
public class Main {
    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("crawler");
        ActorRef manager = system.actorOf(Props.create(CrawlerManager.class), "manager");
        LoggingAdapter log = Logging.getLogger(system, Main.class);

        CommandLineOptions options = new CommandLineOptions();
        new JCommander(options, args);

        try {
            final Stream<String> domains = Files.lines(Paths.get(options.getInputDomainsFile()))
                    .map(String::trim).filter(f -> !f.isEmpty());
            domains.forEach(domain -> manager.tell(new AddDomain(domain), null));
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        final Future<Object> result = ask(manager, new StartCrawl(), new Timeout(Duration.create(1, TimeUnit.HOURS)));
        result.onSuccess(new OnSuccess<Object>() {
            @Override
            public void onSuccess(Object o) throws Throwable {
                LinkedList<URI> l = ((CrawlResult) o).getLinks();
                log.info("Number of links discovered: " + l.size());

                Files.write(Paths.get(options.getOutputFile()),
                        (Iterable<String>)l.stream().map(URI::toString)::iterator, StandardCharsets.UTF_8);
                system.shutdown();
            }
        }, system.dispatcher());

    }
}

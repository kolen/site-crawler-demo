package crawler;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.OnSuccess;
import akka.util.Timeout;
import crawler.messages.AddDomain;
import crawler.messages.LinksList;
import crawler.messages.StartCrawl;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.net.URI;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;

/**
 * 
 */
public class Main {
    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("crawler");
        ActorRef manager = system.actorOf(Props.create(CrawlerManager.class), "manager");

        manager.tell(new AddDomain("example.com"), null);
        final Future<Object> result = ask(manager, new StartCrawl(), new Timeout(Duration.create(1, TimeUnit.HOURS)));
        result.onSuccess(new OnSuccess<Object>() {
            @Override
            public void onSuccess(Object o) throws Throwable {
                LinkedList<URI> l = ((LinksList) o).getLinks();
                for (URI uri : l) {
                    System.out.println(l);
                }
            }
        }, system.dispatcher());

    }
}

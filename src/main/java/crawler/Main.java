package crawler;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * 
 */
public class Main {
    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("crawler");
        ActorRef manager = system.actorOf(Props.create(CrawlerManager.class), "manager");
        try {
            manager.tell(new URI("http://www.ru/"), null);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}

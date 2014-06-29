package crawler;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import crawler.messages.AddDomain;

/**
 * 
 */
public class Main {
    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("crawler");
        ActorRef manager = system.actorOf(Props.create(CrawlerManager.class), "manager");

        manager.tell(new AddDomain("www.ru"), null);
    }
}

package crawler;

import akka.actor.ActorSystem;

import java.lang.System;

/**
 * 
 */
public class Main {
    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("crawler");
        System.out.println("Lol");
    }
}

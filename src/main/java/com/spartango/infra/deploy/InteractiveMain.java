package com.spartango.infra.deploy;

import com.spartango.infra.interactive.SimulationServer;

/**
 * Author: spartango
 * Date: 3/5/16
 * Time: 11:17.
 */
public class InteractiveMain {
    public static void main(String[] args){
        SimulationServer server = new SimulationServer();
        System.out.println("Starting server on 8080");
        server.start();

        synchronized (server){
            try {
                server.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
                server.stop();
            }
        }

        SimulationServer.cleanUp();
    }
}

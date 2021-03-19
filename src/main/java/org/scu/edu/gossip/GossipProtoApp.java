package org.scu.edu.gossip;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.scu.edu.gossip.configs.GossipProperty;
import org.scu.edu.gossip.services.NodeGossiper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;

public class GossipProtoApp {
    private static final String KEY_HOST_NAME = "-hostname";
    private static final String KEY_PORT = "-port";
    private static final Logger log = Logger.getLogger(GossipProtoApp.class);

    public static void main(String[] args) {

        //Initialising Logger Configuration
        initLogger(Integer.parseInt(args[3]));

        //Seed is used for starting initial communication
        int seed = 0;
        
        //NodeGossiper handles maintaining membership, failure detection, sending/recieving chat messages
        NodeGossiper initialNodeGossiper;
        
        if (args.length < 4 || !args[0].equals(KEY_HOST_NAME) || !args[2].equals(KEY_PORT)) {
            System.out.println("Please make sure you pass the required arguments in below format\n" +
                    "-hostname {hostname} -port {port-number} {seed}");
            System.exit(-1);
        }
        String hostname = args[1];
        int port = Integer.parseInt(args[3]);
        if (args.length > 4){
            seed = Integer.parseInt(args[4]);
        }        
        //Get default gossip configurations
        GossipProperty gossipProperty = buildGossipProperty();
        InetSocketAddress primaryNodeAddress = new InetSocketAddress(hostname, port);

        if (seed == 0) {

            initialNodeGossiper = new NodeGossiper(primaryNodeAddress, gossipProperty);

        } else {
            initialNodeGossiper = new NodeGossiper(primaryNodeAddress, new InetSocketAddress(hostname, seed), gossipProperty);

        }
        System.out.println("Node Started- "+primaryNodeAddress.getAddress() +"::"+primaryNodeAddress.getPort());
        initialNodeGossiper.start();

    }

    private static GossipProperty buildGossipProperty() {
        return new GossipProperty(
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                Duration.ofMillis(500),
                Duration.ofMillis(500),
                4
        );
    }

    //Configuring Logger for creating log files
    public static void initLogger(int port) {
        Logger rootLogger = Logger.getRootLogger();
        rootLogger.setLevel(Level.ALL);

        //Define log pattern layout
        PatternLayout layout = new PatternLayout("%d{ISO8601} [%t] %-5p %c %x - %m%n");

        //Adding console appender to root logger
        // rootLogger.addAppender(new ConsoleAppender(layout));
        try {
            //Define file appender with layout and output log file name
            RollingFileAppender fileAppender = new RollingFileAppender(layout, port + "_logs.log");

            //Add the appender to root logger
            rootLogger.addAppender(fileAppender);
        } catch (IOException e) {
            System.out.println("Failed to add appender !!");
        }
    }
}




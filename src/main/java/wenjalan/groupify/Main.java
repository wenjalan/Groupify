package wenjalan.groupify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.Scanner;

import static wenjalan.groupify.Groupify.PROPERTIES_FILE;

@SpringBootApplication
public class Main {

    // main
    public static void main(String[] args) {
        // start Spring web service for callback
        ApplicationContext c = SpringApplication.run(Main.class, args);

        // start Groupify
        System.out.println(">> Starting Groupify Prototype 2 (12/30/19) <<");
        Groupify g = new Groupify(PROPERTIES_FILE, true);

        // start loop
        Scanner console = new Scanner(System.in);
        boolean quit = false;
        while (!quit) {
            // print out the party
            System.out.println();
            System.out.println(">> Current Party:");
            for (GroupifyUser u : g.getParty()) {
                System.out.println("\t" + u.getDisplayName() + " (" + u.getUserId() + ")");
            }
            System.out.println();
            System.out.println(">> Commands:" +
                    "\n\tADD (adds a new user to the party)" +
                    "\n\tCREATE (create the playlist on the host user's account)" +
                    "\n\tINFO (print information about all users in the party)" +
                    "\n\tPURGE (unfollows all playlists named \"Groupify Playlist\" on the host user's account)" +
                    "\n\tQUIT (quit the application)");
            String response = console.nextLine();
            if (response.equalsIgnoreCase("add")) {
                g.addGuest();
            }
            else if (response.equalsIgnoreCase("create")) {
                g.createPlaylist();
            }
            else if (response.equalsIgnoreCase("info")) {
                for (GroupifyUser u : g.getParty()) {
                    u.printInfo();
                }
            }
            else if (response.equalsIgnoreCase("purge")) {
                g.unfollowOldPlaylists();
            }
            else if (response.equalsIgnoreCase("quit")) {
                quit = true;
            }
            else {
                System.err.println("!! command not recognized: " + response);
            }
        }

        // announce
        System.out.println(">> Groupify shutting down... <<");

        // stop Spring web service
        SpringApplication.exit(c, () -> 0);
    }

}

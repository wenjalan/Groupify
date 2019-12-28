package wenjalan.groupify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import java.util.Scanner;

import static wenjalan.groupify.Groupify.PROPERTIES_FILE;

@SpringBootApplication
public class Main {

    // main
    public static void main(String[] args) {
        // start Spring web service for callback
        ApplicationContext c = SpringApplication.run(Main.class, args);

        // start Groupify
        System.out.println(">> Host User");
        Groupify g = new Groupify(PROPERTIES_FILE);

        Scanner console = new Scanner(System.in);
        boolean shouldContinue = false;
        while (!shouldContinue) {
            System.out.println(">> Add anther user? (Return for no)");
            String response = console.nextLine();
            if (response.isEmpty()) {
                shouldContinue = true;
            }
            else {
                g.addGuest();
            }
        }

        // print out the party
        System.out.println(">> Users in party: " + g.getParty());

        // print out user info
        for (GroupifyUser user : g.getParty()) {
            user.printInfo();
        }

        // create the playlist
        g.createPlaylist();

        // stop Spring web service
        SpringApplication.exit(c, () -> 0);
    }

}

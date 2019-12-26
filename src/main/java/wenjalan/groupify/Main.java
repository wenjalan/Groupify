package wenjalan.groupify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import static wenjalan.groupify.Groupify.PROPERTIES_FILE;

@SpringBootApplication
public class Main {

    // main
    public static void main(String[] args) {
        // start Spring web service for callback
        ApplicationContext c = SpringApplication.run(Main.class, args);

        // start Groupify
        Groupify g = new Groupify(PROPERTIES_FILE);
        g.addGuest();

        // print out the party
        System.out.println(g.getParty());

        // create the playlist
        g.createPlaylist();

        // stop Spring web service
        SpringApplication.exit(c, () -> 0);
    }

}

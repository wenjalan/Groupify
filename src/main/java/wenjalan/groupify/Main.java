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

        // add another user (me again)
        g.addNewUser();

        // print out all the users
        System.out.println(g.getUsers());

        // stop Spring web service
        SpringApplication.exit(c, () -> 0);
    }

}

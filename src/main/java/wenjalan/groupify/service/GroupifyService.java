package wenjalan.groupify.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

// the Groupify service, handles all calls going in and out of the API
@SpringBootApplication
public class GroupifyService {

    // the Spring ApplicationContext
    private ApplicationContext applicationContext;

    // the configuration
    private GroupifyConfiguration configuration;

    // program entrypoint
    public static void main(String[] args) {
        // get the config file path from args
        if (args.length == 0) {
            System.err.println("please specify a properties filepath");
            return;
        }

        // get the config
        GroupifyConfiguration config = GroupifyConfiguration.from(args[0]);

        // start Groupify service
        GroupifyService service = new GroupifyService(config, args);
    }

    // constructor
    public GroupifyService(GroupifyConfiguration config, String[] args) {
        // start Spring
        this.applicationContext = SpringApplication.run(GroupifyService.class, args);

        // save params
        this.configuration = config;
    }

    // creates a new Party
    // post: a new Party object in the PartyManager has been created
    // returns: the Party id of the created Party
    public String createParty() {
        return null;
    }

    // stops the service
    public void stop() {
        System.out.println("stopping GroupifyService...");
        SpringApplication.exit(applicationContext, () -> 0);
    }

    /////////////////////////
    // Spring REST Methods //
    /////////////////////////

    // default constructor (for Spring, don't touch)
    public GroupifyService() {
        // empty
    }

    // callback bean
    @Bean
    public PartyManager partyManager() {
        return PartyManager.getInstance();
    }

}

package wenjalan.groupify.service;

import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

// the Groupify service, handles all calls going in and out of the API
@SpringBootApplication
public class GroupifyService {

    // the list of Authentication Listeners
    public static final List<AuthenticationListener> AUTHENTICATION_LISTENERS = new LinkedList<>();

    // the scopes we're using for host users
    public static final String[] HOST_SCOPES = {
            "playlist-modify-public",
            "playlist-read-collaborative",
            "playlist-read-private",
            "playlist-modify-private",
            "user-top-read",
            "user-library-read",
    };

    // the scopes we're using for the guest users
    public static final String[] GUEST_SCOPES = {
            "playlist-read-collaborative",
            "playlist-read-private",
            "user-top-read",
            "user-library-read",
    };

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

        // run tests
        URI authUri = service.createParty();
        System.out.println(authUri.toString());
    }

    // constructor
    public GroupifyService(GroupifyConfiguration config, String[] args) {
        // start Spring
        this.applicationContext = SpringApplication.run(GroupifyService.class, args);

        // save params
        this.configuration = config;
        GroupifyUser.Factory.setConfiguration(this.configuration);
    }

    // creates a new Party
    // post (before authentication): a valid authentication URI for which the host is to log in with
    // post (after authentication): a new Party object in the PartyManager has been created
    // returns: an authentication URI for the host user of this party
    public URI createParty() {
        // get a new Spotify API instance to create a request with
        SpotifyApi spotify = new SpotifyApi.Builder()
                .setClientId(configuration.CLIENT_ID)
                .setClientSecret(configuration.CLIENT_SECRET)
                .setRedirectUri(configuration.REDIRECT_URI)
                .build();

        // start building a new Party
        final GroupifyParty.Builder partyBuilder = new GroupifyParty.Builder();
        final String partyId = partyBuilder.getId();
        final String startState = partyId + ":" + UUID.randomUUID().toString(); // random uuid to check against

        // create a new authentication request for the host to sign in with
        AuthorizationCodeUriRequest request = spotify.authorizationCodeUri()
                .state(startState)
                .scope(String.join(",", HOST_SCOPES))
                .build();

        // get the uri
        final URI uri = request.execute();

        // register a listener
        AuthenticationListener listener = new AuthenticationListener() {
            @Override
            public void onAuthenticationSuccess(String code, String state) {
                // check that the state makes sense
                if (!startState.equals(state)) {
                    throw new IllegalStateException("state mismatch");
                }

                // once authenticated, get the user and form a new party
                GroupifyUser host = GroupifyUser.Factory.createUser(code, true);
                partyBuilder.build(host);
            }

            @Override
            public void onAuthenticationFailure(String message) {
                System.err.println("error creating party: " + message);
            }

            @Override
            public String partyId() {
                return partyId;
            }

        };
        AUTHENTICATION_LISTENERS.add(listener);

        // return the uri
        return uri;
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

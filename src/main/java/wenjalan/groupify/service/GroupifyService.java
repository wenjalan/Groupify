package wenjalan.groupify.service;

import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.model_objects.specification.Playlist;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import org.apache.http.auth.AUTH;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static wenjalan.groupify.service.GroupifyConfiguration.GUEST_SCOPES;
import static wenjalan.groupify.service.GroupifyConfiguration.HOST_SCOPES;

// the Groupify service, handles all calls going in and out of the API
@SpringBootApplication
public class GroupifyService {

    // the GroupifyService instance
    public static GroupifyService instance = null;

    // the list of Authentication Listeners
    public static final List<AuthenticationListener> AUTHENTICATION_LISTENERS = new LinkedList<>();

    // the Spring ApplicationContext
    private static ApplicationContext applicationContext;

    // the configuration
    private static GroupifyConfiguration configuration;

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
        GroupifyService.start(config, args);
    }

    // starts the Groupify Service
    public static void start(GroupifyConfiguration config, String[] pArgs) {
        // announce
        System.out.println("Starting Groupify prototype 3...");
        configuration = config;

        // create singleton instance
        instance = new GroupifyService();

        // set the user factory's config to our config
        // todo: see if we can do this a better way
        GroupifyUser.Factory.setConfiguration(configuration);

        // start Spring services
        applicationContext = SpringApplication.run(GroupifyService.class, pArgs);
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
                    throw new IllegalStateException("state mismatch when authenticating host user");
                }

                // once authenticated, get the user and form a new party
                GroupifyUser host = GroupifyUser.Factory.createUser(code, true);
                partyBuilder.build(host);

                // detach this listener
                AUTHENTICATION_LISTENERS.remove(this);
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

    // adds a new user to the specified Party
    // post (before authentication): a valid authentication URI for which the guest is to log in with
    // post (after authentication): the Party object is updated with the associated guest
    // returns: an authentication URI for the guest to log in with
    public URI addUserToParty(final GroupifyParty party) {
        // get a Guest Spotify API instance
        SpotifyApi api = new SpotifyApi.Builder()
                .setClientId(configuration.GUEST_ID)
                .setClientSecret(configuration.GUEST_SECRET)
                .setRedirectUri(configuration.REDIRECT_URI)
                .build();

        // create a state to identify this request with
        final String startState = party.getId() + ":" + UUID.randomUUID().toString();

        // ask for an authorization code from it
        AuthorizationCodeUriRequest request = api.authorizationCodeUri()
                .state(startState)
                .scope(String.join(",", GUEST_SCOPES))
                .build();

        // get the URI to return
        final URI uri = request.execute();

        // attach a new listener
        AuthenticationListener listener = new AuthenticationListener() {
            @Override
            public void onAuthenticationSuccess(String code, String state) {
                // if the states don't match, complain
                if (!startState.equalsIgnoreCase(state)) {
                    throw new IllegalStateException("state mismatch when authenticating guest user");
                }

                // once authenticated, add the user to the party
                GroupifyUser user = GroupifyUser.Factory.createUser(code, false);
                party.addUser(user);

                // detach this listener
                AUTHENTICATION_LISTENERS.remove(this);
            }

            @Override
            public void onAuthenticationFailure(String message) {
                System.err.println("error authenticating guest: " + message);
            }

            @Override
            public String partyId() {
                return party.getId();
            }
        };
        AUTHENTICATION_LISTENERS.add(listener);

        // return the URI
        return uri;
    }

    // creates the playlist on the host user's account
    // post: a new Groupify Playlist on the host user's account
    public boolean makePlaylist(GroupifyParty party) {
        // get a Playlist Generator for this Party
        PlaylistGenerator generator = new PlaylistGenerator(party, true);

        // make the playlist
        Playlist playlist = generator.createPlaylist();

        // return if it worked or not
        if (playlist == null) {
            return false;
        }
        else {
            return true;
        }
    }

    // stops the service
    public void stop() {
        System.out.println("stopping Groupify Service...");
        SpringApplication.exit(applicationContext, () -> 0);
    }

    // returns the instance of Groupify Service
    public static GroupifyService getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Groupify Service has not been initialized, use GroupifyService.start()");
        }
        return instance;
    }

    /////////////////////////
    // Spring REST Methods //
    /////////////////////////

    // default constructor
    public GroupifyService() {

    }

    // GroupifyService bean
    @Bean
    public static GroupifyService gs() {
        return getInstance();
    }

}

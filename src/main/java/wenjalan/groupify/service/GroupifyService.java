package wenjalan.groupify.service;

import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.specification.Playlist;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import wenjalan.groupify.service.model.Party;
import wenjalan.groupify.service.model.GroupifyUser;

import java.io.IOException;
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
        System.out.println("Starting GroupifyService Prototype 5 (2/15/2020) ...");
        configuration = config;

        // see if we're in verbose mode
        if (config.VERBOSE) {
            System.out.println("[V] Started in VERBOSE mode...");
        }

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
        final Party.Builder partyBuilder = new Party.Builder();
        final String partyId = partyBuilder.getId();
        final String startState = partyId + ":" + UUID.randomUUID().toString(); // random uuid to check against

        // create a new authentication request for the host to sign in with
        AuthorizationCodeUriRequest request = spotify.authorizationCodeUri()
                .state(startState)
                .scope(String.join(",", HOST_SCOPES))
                .show_dialog(true)
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
                Party p = partyBuilder.build(host);

                // detach this listener
                AUTHENTICATION_LISTENERS.remove(this);

                // log if in verbose mode
                if (configuration.VERBOSE) {
                    System.out.println("[V] Host " + host.getUserId() + " for party " + p.getId() + " authenticated successfully");
                }
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

        // log if in verbose mode
        if (configuration.VERBOSE) {
            System.out.println("[V] Awaiting host authentication for party id " + partyId + "...");
        }

        // return the uri
        return uri;
    }

    // adds a new user to the specified Party
    // post (before authentication): a valid authentication URI for which the guest is to log in with
    // post (after authentication): the Party object is updated with the associated guest
    // returns: an authentication URI for the guest to log in with
    public URI addUserToParty(final Party party) {
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
                .show_dialog(true)
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

                // log if in verbose mode
                if (configuration.VERBOSE) {
                    System.out.println("[V] Guest " + user.getUserId() + " for party " + party.getId() + " authenticated successfully");
                }
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

        // log if in verbose mode
        if (configuration.VERBOSE) {
            System.out.println("[V] Awaiting guest authentication for party " + party.getId() + "...");
        }

        // return the URI
        return uri;
    }

    // removes a user from the specified party
    // post: if a user with the given user id is found, they will be removed from the party
    // returns: whether or not the operation was successful
    // todo: make an api mapping in GroupifyController for this method: REMOVE
    public boolean removeUserFromParty(Party party, String userId) {
        // find the user to remove
        for (GroupifyUser user : party.getUsers()) {
            // if this is the user
            if (user.getUserId().equals(userId)) {
                // remove them from the party
                party.removeUser(user);

                // log if in verbose mode
                if (configuration.VERBOSE) {
                    System.out.println("[V] Removed user " + userId + " from party " + party.getId() + " successfully");
                }

                // return success
                return true;
            }
        }

        // log if in verbose mode
        if (configuration.VERBOSE) {
            System.out.println("[V] Removal of user " + userId + " from party " + party.getId() + " failed");
        }

        // return false, user not found
        return false;
    }

    // removes all users from the current party, except for the host
    // post: the only user in the party is the host
    public void clearParty(Party party) {
        // remove each user, unless they're the host
        for (GroupifyUser user : party.getUsers()) {
            // if they're not the host, remove them
            if (!user.isHost()) {
                party.removeUser(user);
            }
        }
        // log if in verbose
        if (configuration.VERBOSE) {
            System.out.println("[V] Cleared " + party.getId() + " successfully");
        }
    }

    // removes all playlists named Groupify Playlist from the host's account
    // pre: user is a host user
    // post: all playlists named Groupify Playlist are unfollowed on the host's account
    public boolean purgePlaylists(GroupifyUser user) {
        // check that we have a host user
        if (!user.isHost()) {
            // complain
            throw new IllegalArgumentException("user is not a host user: " + user.getUserId());
        }

        // get the all of the user's playlists
        List<Playlist> playlists = user.getPlaylists();

        // if the playlist's name is "Groupify Playlist", unfollow it
        for (Playlist p : playlists) {
            // if the playlist is named Groupify Playlist...
            if (p.getName().equals("Groupify Playlist")) {
                // ... unfollow it
                try {
                    user.getApiInstance().unfollowPlaylist(p.getId()).build().execute();
                } catch (SpotifyWebApiException | IOException e) {
                    System.err.println("error purging playlists from user " + user.getUserId() + "'s account: " + e.getMessage());
                    e.printStackTrace();
                    return false;
                }
            }
        }

        // logging
        if (configuration.VERBOSE) {
            System.out.println("[V] Purged playlists of host user " + user.getUserId() + " successfully");
        }

        return true;
    }

    // creates the playlist on the host user's account
    // post: a new Groupify Playlist on the host user's account
    public boolean makePlaylist(Party party) {
        // get a Playlist Generator for this Party
        PlaylistGenerator generator = new PlaylistGenerator(party, false);

        // make the playlist
        Playlist playlist = generator.createPlaylist();

        // return if it worked or not
        if (playlist == null) {
            return false;
        }
        else {
            if (configuration.VERBOSE) {
                System.out.println("[V] Playlist for party " + party.getId() + " created successfully");
            }
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

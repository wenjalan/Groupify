package wenjalan.groupify;

import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import com.wrapper.spotify.model_objects.specification.Artist;
import com.wrapper.spotify.model_objects.specification.Playlist;
import com.wrapper.spotify.model_objects.specification.Track;
import com.wrapper.spotify.model_objects.specification.User;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.*;

// main program entry point
public class Groupify {

    // the GroupifyUser host
    protected GroupifyUser groupifyHost = null;

    // the Map of GroupifyUser IDs to their authentication codes
    protected Set<GroupifyUser> users = new HashSet<>();

    // the Groupify properties file name
    public static final String PROPERTIES_FILE = "groupify.properties";

    // the maximum number of users in a party
    public static final int MAX_PARTY_SIZE = 99;

    // the Host's Spotify Web API instance
    protected SpotifyApi hostSpotify;

    // the guests' Spotify Web API instance
    protected SpotifyApi guestSpotify;

    // the scopes we're using
    public static final String[] SCOPES = {
            "playlist-modify-public",
            "playlist-read-collaborative",
            "playlist-read-private",
            "playlist-modify-private",
            "user-top-read",
            "user-library-read"
    };

    // Spring constructor (empty)
    @Autowired
    protected Groupify() {}

    // Groupify constructor
    protected Groupify(String propertiesFilePath) {
        // get the configuration from the properties file
        GroupifyConfiguration config = GroupifyConfiguration.from(propertiesFilePath);

        // create a new Spotify Web API instance from the config
        hostSpotify = new SpotifyApi.Builder()
                .setClientId(config.CLIENT_ID)
                .setClientSecret(config.CLIENT_SECRET)
                .setRedirectUri(config.REDIRECT_URI)
                .build();

        // create a new Spotify Web API instance for the guest users
        guestSpotify = new SpotifyApi.Builder()
                .setClientId(config.GUEST_ID)
                .setClientSecret(config.GUEST_SECRET)
                .setRedirectUri(config.REDIRECT_URI)
                .build();

        // request user authentication
        authenticate(hostSpotify, SCOPES);
    }

    // adds a new user to the group
    public void addGuest() {
        // party size limit
        if (users.size() >= MAX_PARTY_SIZE) {
            throw new IllegalStateException("max number of users in a party is " + MAX_PARTY_SIZE);
        }
        authenticate(guestSpotify, SCOPES);
    }

    // requests the user to authenticate Groupify to modify their data given a Spotify Web API instance
    private void authenticate(SpotifyApi api, String[] scopes) {
        // print out authorization URI
        AuthorizationCodeUriRequest request = api.authorizationCodeUri()
                .scope(String.join(" ", scopes))
                .build();
        System.out.println("> authentication URI: " + request.getUri());

        // wait for code to arrive in Groupify Callback and authenticate the API instance
        String code = null;
        try {
            // if it's still null, keep waiting
            int timeout = 5 * 60_000; // 5 * 60 seconds, 5 minutes
            int time = 0;
            int waitIncrement = 100;
            while ((code = GroupifyCallback.getLastCode()) == null) {
                Thread.sleep(waitIncrement);
                time += waitIncrement;
                if (time >= timeout) {
                    throw new InterruptedException("authentication timed out");
                }
            }
        } catch (InterruptedException e) {
            System.err.println("!!! interrupted while waiting for authentication code: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        // authorize the api with the code
        authorize(api, code);

        // if this is the first user being authorized, make them the host
        GroupifyUser user = new GroupifyUser(api);
        if (users == null) {
            groupifyHost = user;
            users = new HashSet<>();
        }

        // add them to the pool of users
        users.add(user);

        // announce
        System.out.println("> added " + user.getDisplayName() + " to the party");
    }

    // authorizes a Spotify Web API instance with an auth code
    private void authorize(SpotifyApi api, String code) {
        AuthorizationCodeRequest request = api.authorizationCode(code).build();
        try {
            AuthorizationCodeCredentials credentials = request.execute();
            api.setAccessToken(credentials.getAccessToken());
            api.setRefreshToken(credentials.getRefreshToken());
        } catch (SpotifyWebApiException | IOException e) {
            System.err.println("!!! error authorizing api: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // creates a new Playlist based off of all the users in the current pool
    public Playlist createPlaylist() {
        // check if we can generate a playlist
        // at least 2 users
        if (this.users.size() < 2) {
            throw new IllegalStateException("can't create playlist, at least 2 users need to be in the party");
        }
        PlaylistGenerator generator = new PlaylistGenerator(hostSpotify, users);
        return generator.createPlaylist();
    }

    // returns the set of users currently registered in the app
    public Set<GroupifyUser> getParty() {
        return new HashSet<>(users);
    }

}

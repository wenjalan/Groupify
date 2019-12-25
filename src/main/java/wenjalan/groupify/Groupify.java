package wenjalan.groupify;

import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import com.wrapper.spotify.model_objects.specification.Artist;
import com.wrapper.spotify.model_objects.specification.Track;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import javafx.scene.effect.Light;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// main program entry point
public class Groupify {

    // the Map of User IDs to their authentication codes
    protected Set<User> users = new HashSet<>();

    // the Groupify properties file name
    public static final String PROPERTIES_FILE = "groupify.properties";

    // the Spotify Web API instance
    protected SpotifyApi spotify;

    // the scopes we're using
    public static final String[] SCOPES = {
            "playlist-modify-public ",
            "playlist-read-collaborative ",
            "playlist-read-private ",
            "playlist-modify-private",
            "user-top-read",
            "user-read-recently-played"
    };

    // Spring constructor (empty)
    @Autowired
    protected Groupify() {}

    // Groupify constructor
    protected Groupify(String propertiesFilePath) {
        // get the configuration from the properties file
        GroupifyConfiguration config = getConfigFromFile(propertiesFilePath);

        // create a new Spotify Web API instance from the config
        spotify = new SpotifyApi.Builder()
                .setClientId(config.CLIENT_ID)
                .setClientSecret(config.CLIENT_SECRET)
                .setRedirectUri(config.REDIRECT_URI)
                .build();

        // request user authentication
        requestAuthentication(SCOPES);
    }

    // adds a new user to the group
    public void addNewUser() {
        requestAuthentication(SCOPES);
    }

    // returns the users in the current group
    public Set<User> getUsers() {
        return this.users;
    }

    // requests the user to authenticate Groupify to modify their data given a Spotify Web API instance
    protected void requestAuthentication(String[] scopes) {
        // print the auth URI
        printAuthUri(this.spotify, scopes);

        // wait for code to arrive in Groupify Callback and authenticate the API instance
        try {
            // if it's still null, keep waiting
            String code;
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
            // if it's not null, take the token and add the user to the pool
            addUser(code);
        } catch (InterruptedException e) {
            System.err.println("!!! interrupted while waiting for authentication code: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    // prints the authentication URI to the console
    protected void printAuthUri(SpotifyApi api, String[] scopes) {
        // print out authorization URI
        AuthorizationCodeUriRequest request = api.authorizationCodeUri()
                .scope(String.join(" ", scopes))
                .build();
        System.out.println("> authentication URI: " + request.getUri());
    }

    // authenticates a Spotify Web API instance given a code
    protected void addUser(String code) {
        // if the api hasn't been initialized, scream
        if (this.spotify == null) {
            throw new IllegalStateException("api instance is null");
        }

        // create a new User object representing this user
        User user = new User(code);

        // if this is the first user, make them the host
        if (users.isEmpty()) {
            user.setHost(true);
        }

        // put them in the set of users
        this.users.add(user);

        // fill in their user data
        switchToUser(user);

        try {
            // user id
            String userId = this.spotify.getCurrentUsersProfile().build().execute().getId();
            user.setUserId(userId);

            // top tracks
            Track[] topTracks = this.spotify.getUsersTopTracks().build().execute().getItems();
            user.setTopTracks(topTracks);

            // top artists
            Artist[] topArtists = this.spotify.getUsersTopArtists().build().execute().getItems();
            user.setTopArtists(topArtists);
        } catch (SpotifyWebApiException | IOException e) {
            System.err.println("! error gathering user info");
            e.printStackTrace();
        }

        // announce that we added a user
        System.out.println("> added new user " + user.getUserId() + " to the group");
    }

    // switches the API's current user to a given user id
    protected void switchToUser(User user) {
        if (!this.users.contains(user)) {
            throw new IllegalArgumentException("user " + user.getUserId() + " is not recognized");
        }
        AuthorizationCodeRequest request = this.spotify.authorizationCode(user.getAuthCode()).build();
        try {
            AuthorizationCodeCredentials credentials = request.execute();
            this.spotify.setAccessToken(credentials.getAccessToken());
            this.spotify.setRefreshToken(credentials.getRefreshToken());
        } catch (SpotifyWebApiException | IOException e) {
            System.err.println("! error switching to user " + user.getUserId());
            e.printStackTrace();
        }
    }

    // returns a GroupifyConfiguration given a properties file path
    protected GroupifyConfiguration getConfigFromFile(String filepath) {
        // find and read the file
        InputStream properties = getClass().getClassLoader().getResourceAsStream(filepath);
        if (properties == null) {
            throw new IllegalArgumentException("properties file not found: " + filepath);
        }
        String clientId = null;
        String clientSecret = null;
        String redirectUri = null;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(properties))) {
            for (String line; (line = br.readLine()) != null;) {
                // client id property
                if (line.startsWith("spotify-client-id=")) {
                    clientId = line.replace("spotify-client-id=", "");
                }
                // client secret property
                else if (line.startsWith("spotify-client-secret=")) {
                    clientSecret = line.replace("spotify-client-secret=", "");
                }
                // redirect uri property
                else if (line.startsWith("spotify-redirect-uri=")) {
                    redirectUri = line.replace("spotify-redirect-uri=", "");
                }
            }
            if (clientId == null || clientSecret == null || redirectUri == null) {
                throw new IOException("error parsing properties");
            }
        } catch (IOException e) {
            System.err.println("! error reading properties file: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        return new GroupifyConfiguration(clientId, clientSecret, URI.create(redirectUri));
    }

}

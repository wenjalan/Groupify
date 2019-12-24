package wenjalan.groupify;

import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import javafx.scene.effect.Light;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Arrays;

// main program entry point
@SpringBootApplication
public class Groupify {

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
    };

    // main
    public static void main(String[] args) {
        // start Spring web service for callback
        SpringApplication.run(Groupify.class, args);

        // start Groupify
        Groupify g = new Groupify(PROPERTIES_FILE);
    }

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
        requestAuthentication(spotify, SCOPES);
    }

    // requests the user to authenticate Groupify to modify their data given a Spotify Web API instance
    protected void requestAuthentication(SpotifyApi api, String[] scopes) {
        // print the auth URI
        printAuthUri(api, scopes);

        // wait for code to arrive in Groupify Callback and authenticate the API instance
        try {
            // if it's still null, keep waiting
            String code;
            int timeout = 60_000;
            int time = 0;
            int waitIncrement = 100;
            while ((code = GroupifyCallback.getLastCode()) == null) {
                Thread.sleep(waitIncrement);
                time += waitIncrement;
                if (time >= timeout) {
                    throw new InterruptedException("authentication timed out");
                }
            }
            // if it's not null, take the token and authenticate
            authenticate(api, code);
        } catch (InterruptedException e) {
            System.err.println("interrupted while waiting for authentication code: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("API instance authenticated successfully");
    }

    // prints the authentication URI to the console
    protected void printAuthUri(SpotifyApi api, String[] scopes) {
        // print out authorization URI
        AuthorizationCodeUriRequest request = api.authorizationCodeUri()
                .scope(String.join(" ", scopes))
                .build();
        System.out.println("Authentication URI: " + request.getUri());
    }

    // authenticates a Spotify Web API instance given a code
    protected void authenticate(SpotifyApi api, String code) {
        if (api == null) {
            throw new IllegalStateException("api instance is null");
        }
        AuthorizationCodeRequest request = this.spotify.authorizationCode(code).build();
        try {
            AuthorizationCodeCredentials credentials = request.execute();
            api.setAccessToken(credentials.getAccessToken());
            api.setRefreshToken(credentials.getRefreshToken());
        } catch (SpotifyWebApiException | IOException e) {
            System.err.println("authentication error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
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
            System.err.println("error reading properties file: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        return new GroupifyConfiguration(clientId, clientSecret, URI.create(redirectUri));
    }

}

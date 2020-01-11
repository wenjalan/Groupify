package wenjalan.groupify.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;

// represents a configuration for the Groupify app
public class GroupifyConfiguration {

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

    public final String CLIENT_ID;
    public final String CLIENT_SECRET;
    public final URI REDIRECT_URI;
    public final String GUEST_ID;
    public final String GUEST_SECRET;
    public final boolean VERBOSE;

    // returns a GroupifyConfiguration given a properties file path
    public static GroupifyConfiguration from(String filepath) {
        // find and read the file
        InputStream properties = ClassLoader.getSystemResourceAsStream(filepath);
        if (properties == null) {
            throw new IllegalArgumentException("properties file not found: " + filepath);
        }
        String clientId = null;
        String clientSecret = null;
        String guestId = null;
        String guestSecret = null;
        String redirectUri = null;
        boolean verbose = false;
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
                // guest id property
                else if (line.startsWith("guest-client-id=")) {
                    guestId = line.replace("guest-client-id=", "");
                }
                // guest secret property
                else if (line.startsWith("guest-client-secret=")) {
                    guestSecret = line.replace("guest-client-secret:=", "");
                }
                // redirect uri property
                else if (line.startsWith("spotify-redirect-uri=")) {
                    redirectUri = line.replace("spotify-redirect-uri=", "");
                }
                // verbose mode
                else if (line.startsWith("verbose-logging=")) {
                    verbose = Boolean.parseBoolean(line.replace("verbose-logging=", "").toLowerCase());
                }
            }
            if (clientId == null || clientSecret == null || guestId == null || guestSecret == null || redirectUri == null) {
                throw new IOException("error parsing properties");
            }
        } catch (IOException e) {
            System.err.println("! error reading properties file: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        return new GroupifyConfiguration(clientId, clientSecret, clientId, clientSecret, URI.create(redirectUri), verbose);
    }

    private GroupifyConfiguration(String clientId, String clientSecret, String guestId, String guestSecret, URI redirectUri, boolean verbose) {
        CLIENT_ID = clientId;
        CLIENT_SECRET = clientSecret;
        GUEST_ID = guestId;
        GUEST_SECRET = guestSecret;
        REDIRECT_URI = redirectUri;
        VERBOSE = verbose;
    }

    @Override
    public String toString() {
        return "GroupifyConfiguration{" +
                "CLIENT_ID='" + CLIENT_ID + '\'' +
                ", CLIENT_SECRET='" + CLIENT_SECRET + '\'' +
                ", REDIRECT_URI=" + REDIRECT_URI +
                ", GUEST_ID='" + GUEST_ID + '\'' +
                ", GUEST_SECRET='" + GUEST_SECRET + '\'' +
                ", VERBOSE=" + VERBOSE +
                '}';
    }
    
}

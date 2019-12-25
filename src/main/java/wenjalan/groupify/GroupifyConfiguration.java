package wenjalan.groupify;

import java.net.URI;

// represents a configuration for the Groupify app
public class GroupifyConfiguration {

    public final String CLIENT_ID;
    public final String CLIENT_SECRET;
    public final URI REDIRECT_URI;

    public GroupifyConfiguration(String clientId, String clientSecret, URI redirectUri) {
        CLIENT_ID = clientId;
        CLIENT_SECRET = clientSecret;
        REDIRECT_URI = redirectUri;
    }

    @Override
    public String toString() {
        return "CLIENT_ID=" + CLIENT_ID + ",CLIENT_SECRET=" + CLIENT_SECRET + ",REDIRECT_URI=" + REDIRECT_URI;
    }

}

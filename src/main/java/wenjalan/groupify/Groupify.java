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

    // the Host user
    protected User host = null;

    // the GroupifyUser host
    protected GroupifyUser groupifyHost = null;

    // the Map of GroupifyUser IDs to their authentication codes
    protected Set<GroupifyUser> users = new HashSet<>();

    // the Groupify properties file name
    public static final String PROPERTIES_FILE = "groupify.properties";

    // the Spotify Web API instance
    protected SpotifyApi spotify;

    // the scopes we're using
    public static final String[] SCOPES = {
            "playlist-modify-public ",
//            "playlist-read-collaborative ",
//            "playlist-read-private ",
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

    // creates a new Playlist based off of all the users in the current pool
    // how songs are selected:
    // 1. a top song is a shared by all users
    // 2. a top song's artist is a top artist of all users
    // 3. a top song's artist is of a top genre for all users
    public Playlist createPlaylist() {
        // swap to the host's context
        switchToUser(this.groupifyHost);
        try {
            // create the playlist
            Playlist playlist = this.spotify.createPlaylist(host.getId(), "Groupify Playlist")
                    .collaborative(false)
                    .description("Created with Groupify, a Spotify helper app for people with friends.")
                    .build()
                    .execute();
            String playlistId = playlist.getId();

            // the list of songs
            List<Track> songs = new ArrayList<>();

            // 1. find top songs shared by all users
            List<Track> sharedTopSongs = getSharedTopSongs(users, users.size());
            songs.addAll(sharedTopSongs);

            // 2. find top songs whose artist is a top artist of all users
            List<Track> sharedArtistSongs = getSharedTopArtistsSongs(users);
            songs.addAll(sharedArtistSongs);

            // 3. find top songs whose artist has genres shared by all users
            List<Track> sharedGenreSongs = getSharedTopGenresSongs(users);
            songs.addAll(sharedGenreSongs);

            // adds the songs to the playlist
            String[] uris = getUris(songs);
            this.spotify.addTracksToPlaylist(playlistId, uris).build().execute();

            // print to console
            System.out.println("> created Groupify playlist with id " + playlistId);

            // return the playlist
            return playlist;
        } catch (SpotifyWebApiException | IOException e) {
            System.err.println("!!! error generating the Groupify playlist: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // returns the shared top songs of a Set of GroupifyUsers
    // threshold: the number of users that must have a song in their top tracks for it to be considered
    public static List<Track> getSharedTopSongs(Set<GroupifyUser> users, int threshold) {
        // keep track of all the songs we've seen (so we don't call the API more times than we have to)
        Map<String, Track> trackLibrary = new HashMap<>();

        // keep track of songs that everyone has
        List<Track> sharedSongs = new ArrayList<>();

        // map everyone's songs to their occurrences
        Map<String, Integer> trackOccurrences = new HashMap<>();
        for (GroupifyUser user : users) {
            // get their top tracks
            Track[] topTracks = user.getTopTracks();
            for (Track t : topTracks) {
                String id = t.getId();
                if (!trackOccurrences.containsKey(id)) {
                    // add their top tracks to the library
                    trackLibrary.put(id, t);
                    // track their occurences
                    trackOccurrences.put(id, 1);
                }
                else {
                    trackOccurrences.put(id, trackOccurrences.get(id) + 1);
                }
            }
        }

        // find the tracks that had threshold or more occurrences and put them into the list
        for (String id : trackOccurrences.keySet()) {
            if (trackOccurrences.get(id) >= threshold) {
                Track t = trackLibrary.get(id);
                sharedSongs.add(t);
            }
        }

        // return the list of tracks
        return sharedSongs;
    }

    // returns the top songs of a Set of GroupifyUsers whose artist is a top artist for all users
    public static List<Track> getSharedTopArtistsSongs(Set<GroupifyUser> users) {
        // TODO: this
        return new ArrayList<>();
    }

    // returns the top songs of a set of GroupifyUsers whose artist contains genres which are top genres for all users
    public static List<Track> getSharedTopGenresSongs(Set<GroupifyUser> users) {
        // TODO: this
        return new ArrayList<>();
    }

    // returns an array of URIs given a list of tracks
    private String[] getUris(List<Track> tracks) {
        String[] uris = new String[tracks.size()];
        for (int i = 0; i < tracks.size(); i++) {
            uris[i] = tracks.get(i).getUri();
        }
        return uris;
    }

    // adds a new user to the group
    public void addNewUser() {
        requestAuthentication(SCOPES);
    }

    // returns the users in the current group
    public Set<GroupifyUser> getUsers() {
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

        // create a new GroupifyUser object representing this user
        GroupifyUser user = new GroupifyUser(code);

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
            Track[] topTracks = this.spotify.getUsersTopTracks().limit(50).build().execute().getItems();
            user.setTopTracks(topTracks);

            // top artists
            Artist[] topArtists = this.spotify.getUsersTopArtists().limit(50).build().execute().getItems();
            user.setTopArtists(topArtists);

            // if this guy is the host, set them as such
            if (user.isHost()) {
                this.host = spotify.getCurrentUsersProfile().build().execute();
                this.groupifyHost = user;
            }
        } catch (SpotifyWebApiException | IOException e) {
            System.err.println("! error gathering user info");
            e.printStackTrace();
        }

        // announce that we added a user
        System.out.println("> added new user " + user.getUserId() + " to the group");
    }

    // switches the API's current user to a given user id
    protected void switchToUser(GroupifyUser user) {
        if (!this.users.contains(user)) {
            throw new IllegalArgumentException("user " + user.getUserId() + " is not recognized");
        }
        String authCode = user.getAuthCode();
        AuthorizationCodeRequest request = this.spotify.authorizationCode(authCode).build();
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

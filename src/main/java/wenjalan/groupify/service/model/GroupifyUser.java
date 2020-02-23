package wenjalan.groupify.service.model;

import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import com.wrapper.spotify.model_objects.specification.*;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import wenjalan.groupify.service.GroupifyConfiguration;

import java.io.IOException;
import java.util.*;

// represents a specific user's taste in music
public class GroupifyUser {

    // the Factory for GroupifyUser objects
    public static class Factory {

        // the configuration this Factory is configured with
        private static GroupifyConfiguration configuration = null;

        // sets the configuration to use when creating GroupifyUsers
        public static void setConfiguration(GroupifyConfiguration config) {
            configuration = config;
        }

        // creates a new GroupifyUser
        public static GroupifyUser createUser(String authCode, boolean isHost) {
            // check if we have a valid config file
            if (configuration == null) {
                throw new IllegalStateException("configuration has not been set, use setConfiguration() with a valid GroupifyConfiguration to set up");
            }

            // gather all their information
            try {
                // get the api to get information with
                SpotifyApi api = authenticate(authCode, isHost);

                // get the user's taste information
                String displayName = api.getCurrentUsersProfile().build().execute().getDisplayName();
                String userId = api.getCurrentUsersProfile().build().execute().getId();
                List<Track> topTracks = Arrays.asList(api.getUsersTopTracks().limit(TOP_TRACKS_TO_RETRIEVE).build().execute().getItems());
                List<Artist> topArtists = Arrays.asList(api.getUsersTopArtists().limit(TOP_ARTISTS_TO_RETRIEVE).build().execute().getItems());
                List<Playlist> playlists = loadPlaylists(api);
                List<SavedTrack> savedTracks = Arrays.asList(api.getUsersSavedTracks().limit(50).build().execute().getItems());
                List<String> topGenres = generateTopGenres(topArtists);


                // return a new GroupifyUser object with that information
                return new GroupifyUser(
                        api,
                        isHost,
                        displayName,
                        userId,
                        topTracks,
                        topArtists,
                        topGenres,
                        playlists,
                        savedTracks
                );
            } catch (SpotifyWebApiException | IOException e) {
                System.out.println();
                System.err.println("!!! error initializing GroupifyUser: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }

        // returns a newly authenticated API instance
        // authCode: the authorization code to authenticate with
        // scopes: the scopes to authenticate with
        private static SpotifyApi authenticate(String authCode, boolean isHost) throws SpotifyWebApiException, IOException {
            // create APi instance
            SpotifyApi.Builder builder = SpotifyApi.builder()
                    .setRedirectUri(configuration.REDIRECT_URI);

            // host user
            if (isHost) {
                builder.setClientId(configuration.CLIENT_ID);
                builder.setClientSecret(configuration.CLIENT_SECRET);
            }

            // guest user
            else {
                builder.setClientId(configuration.GUEST_ID);
                builder.setClientSecret(configuration.GUEST_SECRET);
            }

            // create the api
            SpotifyApi api = builder.build();

            // authorize it
            AuthorizationCodeRequest request = api.authorizationCode(authCode).build();
            AuthorizationCodeCredentials credentials = request.execute();
            api.setAccessToken(credentials.getAccessToken());
            api.setRefreshToken(credentials.getRefreshToken());

            // return it
            return api;
        }

        // returns a list of a user's playlists
        public static List<Playlist> loadPlaylists(SpotifyApi api) {
            try {
                List<Playlist> playlists = new ArrayList<>();
                PlaylistSimplified[] playlistsSimplified = api.getListOfCurrentUsersPlaylists().build().execute().getItems();
                for (PlaylistSimplified ps : playlistsSimplified) {
                    playlists.add(api.getPlaylist(ps.getId()).build().execute());
                }
                return playlists;
            } catch (SpotifyWebApiException | IOException e) {
                System.err.println("error loading playlists: " + e.getMessage());
                e.printStackTrace();
                return Collections.emptyList();
            }
        }

        // returns the top genres of a user given their top artists
        private static List<String> generateTopGenres(List<Artist> topArtists) {
            // create a map of genres to their occurrences
            Map<String, Integer> genres = new TreeMap<>();

            // for their top artists, add their genres to the
            for (Artist a : topArtists) {
                for (String genre : a.getGenres()) {
                    if (!genres.keySet().contains(genre)) {
                        genres.put(genre, 1);
                    }
                    else {
                        genres.put(genre, genres.get(genre) + 1);
                    }
                }
            }

            // reverse the map, having it sorted by number of occurrences
            Map<Integer, List<String>> occurrenceMap = new TreeMap<>();
            for (String genre : genres.keySet()) {
                int occurrences = genres.get(genre);
                if (!occurrenceMap.containsKey(occurrences)) {
                    occurrenceMap.put(occurrences, new LinkedList<>());
                }
                occurrenceMap.get(occurrences).add(genre);
            }

            // create a new list with the genres in sorted order
            // more occurrences = lower index
            LinkedList<String> topGenres = new LinkedList<>();
            for (int occ : occurrenceMap.keySet()) {
                List<String> ls = occurrenceMap.get(occ);
                for (String s : ls) {
                    topGenres.addFirst(s);
                }
            }
            return topGenres;
        }

    }

    // the number of top tracks to retrieve
    public static final int TOP_TRACKS_TO_RETRIEVE = 50;

    // the number of top artists to retrieve
    public static final int TOP_ARTISTS_TO_RETRIEVE = 50;

    // the Spotify API instance of this user
    private final SpotifyApi apiInstance;

    // whether or not this user is a host user
    private final boolean isHost;

    // the display name of this user
    private String displayName;

    // the user id this Taste belongs to
    private String userId;

    // this user's top tracks
    private List<Track> topTracks;

    // this user's top artists
    private List<Artist> topArtists;

    // this user's top genres, based on both their top artists
    // lower index means greater affinity
    private List<String> topGenres;

    // this user's playlists
    private List<Playlist> playlists;

    // this user's liked songs
    private List<SavedTrack> savedTracks;

    // constructor
    private GroupifyUser(SpotifyApi api, boolean isHost, String displayName, String userId, List<Track> topTracks, List<Artist> topArtists, List<String> topGenres, List<Playlist> playlists, List<SavedTrack> savedTracks) {
        this.apiInstance = api;
        this.isHost = isHost;
        this.displayName = displayName;
        this.userId = userId;
        this.topTracks = topTracks;
        this.topArtists = topArtists;
        this.topGenres = topGenres;
        this.playlists = playlists;
        this.savedTracks = savedTracks;
    }

    // apiInstance
    public SpotifyApi getApiInstance() {
        return this.apiInstance;
    }

    // displayName
    public String getDisplayName() {
        return displayName;
    }

    // userId
    public String getUserId() {
        return userId;
    }

    // topTracks
    public Track[] getTopTracks() {
        return topTracks.toArray(new Track[0]);
    }

    // topArtists
    public Artist[] getTopArtists() {
        return topArtists.toArray(new Artist[0]);
    }

    // topGenres
    public List<String> getTopGenres() {
        return topGenres;
    }

    // playlists
    public List<Playlist> getPlaylists() {
        return this.playlists;
    }

    // saved tracks
    public List<SavedTrack> getSavedTracks() {
        return this.savedTracks;
    }

    // isHost
    public boolean isHost() {
        return this.isHost;
    }

    // toString
    @Override
    public String toString() {
        return this.userId;
    }

    // prints all the information we have on a user
    public void printInfo() {
        // name
        System.out.println();
        System.out.println(">>> " + this.getDisplayName() + "'s information: ");

        // playlist and library info
        System.out.println("> found " + this.playlists.size() + " playlists");
        System.out.println("> found " + this.savedTracks.size() + " saved tracks");

        // top tracks
        System.out.println();
        System.out.println("> top songs:");
        int i = 1;
        for (Track t : this.topTracks) {
            System.out.println("> " + i + ": " + t.getName());
            i++;
        }

        // top artists
        System.out.println();
        System.out.println("> top artists:");
        i = 1;
        for (Artist a : this.topArtists) {
            System.out.println("> " + i + ": " + a.getName());
            i++;
        }

        // top genres
        System.out.println();
        System.out.println("> top genres");
        i = 1;
        for (String genre : this.topGenres) {
            System.out.println("> " + i + ": " + genre);
            i++;
        }
    }

    // returns all the information in printInfo() as a String
    public String getInfo() {
        String ret = "";
        // name
        ret += ("\n");
        ret += (">>> " + this.getDisplayName() + "'s information: \n");

        // playlist and library info
        ret += ("> found " + this.playlists.size() + " playlists\n");
        ret += ("> found " + this.savedTracks.size() + " saved tracks\n");

        // top tracks
        ret += ("\n");
        ret += ("> top songs:\n");
        int i = 1;
        for (Track t : this.topTracks) {
            ret += ("> " + i + ": " + t.getName() + "\n");
            i++;
        }

        // top artists
        ret += ("\n");
        ret += ("> top artists:\n");
        i = 1;
        for (Artist a : this.topArtists) {
            ret += ("> " + i + ": " + a.getName() + "\n");
            i++;
        }

        // top genres
        ret += ("\n");
        ret += ("> top genres\n");
        i = 1;
        for (String genre : this.topGenres) {
            ret += ("> " + i + ": " + genre + "\n");
            i++;
        }
        return ret;
    }

}

package wenjalan.groupify;

import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.specification.*;

import java.io.IOException;
import java.util.*;

// represents a specific user's taste in music
public class GroupifyUser {

    // the number of top tracks to retrieve
    public static final int TOP_TRACKS_TO_RETRIEVE = 50;

    // the number of top artists to retrieve
    public static final int TOP_ARTISTS_TO_RETRIEVE = 50;

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
    private List<Track> savedTracks;

    // constructor
    // api: the authorized Spotify API to use
    public GroupifyUser(SpotifyApi api) {
        // get the user's taste information
        try {
            this.displayName = api.getCurrentUsersProfile().build().execute().getDisplayName();
            // announce
            System.out.print("> loading " + this.displayName + "'s tracks...");
            this.userId = api.getCurrentUsersProfile().build().execute().getId();
            this.topTracks = Arrays.asList(api.getUsersTopTracks().limit(TOP_TRACKS_TO_RETRIEVE).build().execute().getItems());
            this.topArtists = Arrays.asList(api.getUsersTopArtists().limit(TOP_ARTISTS_TO_RETRIEVE).build().execute().getItems());
            this.playlists = loadPlaylists(api);
            this.savedTracks = loadSavedTracks(api, 50);
            this.topGenres = generateTopGenres(this.topArtists);
            System.out.println(" done!");
        } catch (SpotifyWebApiException | IOException e) {
            System.out.println();
            System.err.println("!!! error intializing GroupifyUser: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // returns the top genres of a user given their top artists
    protected List<String> generateTopGenres(List<Artist> topArtists) {
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

    // returns a list of a user's liked tracks
    public static List<Track> loadSavedTracks(SpotifyApi api, int limit) {
        try {
            List<Track> tracks = new ArrayList<>();
            SavedTrack[] tracksSimplified = api.getUsersSavedTracks().limit(limit).build().execute().getItems();
            for (SavedTrack st : tracksSimplified) {
                tracks.add(st.getTrack());
            }
            return tracks;
        } catch (SpotifyWebApiException | IOException e) {
            System.err.println("error loading saved tracks: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
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
        if (this.topGenres == null) {
            generateTopGenres(this.topArtists);
        }
        return topGenres;
    }

    // playlists
    public List<Playlist> getPlaylists() {
        return this.playlists;
    }

    // saved tracks
    public List<Track> getSavedTracks() {
        return this.savedTracks;
    }

    // returns whether or not a track is a top track of this user
    public boolean isTopTrack(Track track) {
        return this.topTracks.contains(track);
    }

    // returns whether or not an artist is a top artist of this user
    public boolean isTopArtist(Artist artist) {
        return this.topArtists.contains(artist);
    }

    // returns whether or not a genre is a top genre of this user
    public boolean isTopGenre(String genre) {
        return this.topGenres.contains(genre);
    }

    // toString
    @Override
    public String toString() {
        return this.userId;
    }

    // prints all the information we have on a user
    public void printInfo() {
        // name
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

}

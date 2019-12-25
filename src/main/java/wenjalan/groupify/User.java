package wenjalan.groupify;

import com.wrapper.spotify.model_objects.specification.Artist;
import com.wrapper.spotify.model_objects.specification.Track;

import java.util.*;

// represents a specific user's taste in music
public class User {

    // whether or not this User is the host
    private boolean isHost;

    // the authorization code of this user
    private String authCode;

    // the user id this Taste belongs to
    private String userId;

    // this user's top tracks
    private Track[] topTracks;

    // this user's top artists
    private Artist[] topArtists;

    // this user's top genres, based on both their top artists
    // lower index means greater affinity
    private List<String> topGenres = null;

    // constructor: user
    public User(String authCode) {
        this.authCode = authCode;
    }

    // returns the top genres of a user given their top artists
    protected List<String> generateTopGenres(Artist[] topArtists) {
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

    // userId
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    // topTracks
    public Track[] getTopTracks() {
        return topTracks;
    }

    public void setTopTracks(Track[] topTracks) {
        this.topTracks = topTracks;
    }

    // topArtists
    public Artist[] getTopArtists() {
        return topArtists;
    }

    public void setTopArtists(Artist[] topArtists) {
        this.topArtists = topArtists;
    }

    // topGenres
    public List<String> getTopGenres() {
        if (this.topGenres == null) {
            generateTopGenres(this.topArtists);
        }
        return topGenres;
    }

    // returns whether or not this User is the host
    public boolean isHost() {
        return this.isHost;
    }

    // sets whether this user is the host
    public void setHost(boolean b) {
        this.isHost = b;
    }

    // returns the auth code of this user
    public String getAuthCode() {
        return this.authCode;
    }

    // toString
    @Override
    public String toString() {
        return this.userId;
    }

}

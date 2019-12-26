package wenjalan.groupify;

import com.wrapper.spotify.model_objects.specification.Artist;
import com.wrapper.spotify.model_objects.specification.Track;

import java.util.*;

// represents a specific user's taste in music
public class GroupifyUser {

    // whether or not this GroupifyUser is the host
    private boolean isHost;

    // the authorization code of this user
    private String authCode;

    // the user id this Taste belongs to
    private String userId;

    // this user's top tracks
    private List<Track> topTracks;

    // this user's top artists
    private List<Artist> topArtists;

    // this user's top genres, based on both their top artists
    // lower index means greater affinity
    private List<String> topGenres = null;

    // constructor: user
    public GroupifyUser(String authCode) {
        this.authCode = authCode;
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

    // userId
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    // topTracks
    public Track[] getTopTracks() {
        return topTracks.toArray(new Track[0]);
    }

    public void setTopTracks(Track[] topTracks) {
        this.topTracks = Arrays.asList(topTracks);
    }

    // topArtists
    public Artist[] getTopArtists() {
        return topArtists.toArray(new Artist[0]);
    }

    public void setTopArtists(Artist[] topArtists) {
        this.topArtists = Arrays.asList(topArtists);
    }

    // topGenres
    public List<String> getTopGenres() {
        if (this.topGenres == null) {
            generateTopGenres(this.topArtists);
        }
        return topGenres;
    }

    // returns whether or not this GroupifyUser is the host
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

}

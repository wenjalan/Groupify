package wenjalan.groupify.service.util;

import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.specification.Artist;
import com.wrapper.spotify.model_objects.specification.ArtistSimplified;
import com.wrapper.spotify.model_objects.specification.Track;
import com.wrapper.spotify.model_objects.specification.TrackSimplified;
import wenjalan.groupify.service.GroupifyConfiguration;
import wenjalan.groupify.service.GroupifyService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// aids in the retrieval of several tracks
public class GetArtistBuffer {

    // the list of IDs to retrieve
    private List<String> ids;

    // the Spotify API instance to use
    private SpotifyApi spotify;

    // constructor
    public GetArtistBuffer(SpotifyApi spotify) {
        this.ids = new ArrayList<>();
        this.spotify = spotify;
    }

    // flushes the buffer, returning a List of Tracks
    public List<Artist> flush() throws SpotifyWebApiException, IOException {
        // tracks to return
        List<Artist> artists = new ArrayList<>();

        // get 50 at a time (the max allowed by Spotify)
        for (int i = 0; i < ids.size(); i += 50) {
            // get a sublist of ids
            List<String> sublist = ids.subList(i, Math.min(i + 50, ids.size()));

            // create the String of ids
            String idsQuery = String.join(",", sublist);

            // retrive the tracks
            Artist[] retrieved = spotify.getSeveralArtists().ids(idsQuery).build().execute();

            // add them to the running list
            artists.addAll(Arrays.asList(retrieved));
        }

        // return the tracks
        return artists;
    }

    // clears the buffer
    public void clear() {
        this.ids.clear();
    }

    // add a track id to the buffer
    public void add(String id) {
        ids.add(id);
    }

    // add a TrackSimplified to the buffer
    public void add(ArtistSimplified artistSimplified) {
        add(artistSimplified.getId());
    }

    // remove an id from the buffer
    public void remove(String id) {
        ids.remove(id);
    }

    // addAll
    public void addAll(ArtistSimplified[] artistSimplifieds) {
        for (ArtistSimplified as : artistSimplifieds) {
            add(as);
        }
    }

    // remove a TrackSimplified from the buffer
    public void remove(TrackSimplified trackSimplified) {
        remove(trackSimplified.getId());
    }

    // returns whether the buffer contains an id
    public boolean contains(String id) {
        return ids.contains(id);
    }

    // returns whether the buffer contains a TrackSimplified
    public boolean contains(ArtistSimplified artistSimplified) {
        return contains(artistSimplified.getId());
    }

    // returns the size of the buffer
    public int size() {
        return ids.size();
    }

    // returns the contents of the buffer
    public List<String> getIds() {
        return new ArrayList<>(ids);
    }

    // toString
    @Override
    public String toString() {
        return ids.toString();
    }

}

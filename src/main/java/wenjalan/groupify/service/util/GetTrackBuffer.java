package wenjalan.groupify.service.util;

import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.specification.Track;
import com.wrapper.spotify.model_objects.specification.TrackSimplified;
import wenjalan.groupify.service.GroupifyConfiguration;
import wenjalan.groupify.service.GroupifyService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// aids in the retrieval of several tracks
public class GetTrackBuffer {

    // the list of IDs to retrieve
    private List<String> ids;

    // the Spotify API instance to use
    private SpotifyApi spotify;

    // constructor
    public GetTrackBuffer(SpotifyApi spotify) {
        this.ids = new ArrayList<>();
        this.spotify = spotify;
    }

    // flushes the buffer, returning a List of Tracks
    public List<Track> flush() throws SpotifyWebApiException, IOException {
        // if ids is empty, complain
        if (ids.isEmpty()) {
            throw new IllegalStateException("no ids have been added to the buffer");
        }

        // tracks to return
        List<Track> tracks = new ArrayList<>();

        // get 50 at a time (the max allowed by Spotify)
        for (int i = 0; i < ids.size(); i += 50) {
            // get a sublist of ids
            List<String> sublist = ids.subList(i, Math.min(i + 50, ids.size()));

            // create the String of ids
            String idsQuery = String.join(",", sublist);

            // retrive the tracks
            Track[] retrieved = spotify.getSeveralTracks(idsQuery).build().execute();

            // add them to the running list
            tracks.addAll(Arrays.asList(retrieved));
        }

        // return the tracks
        return tracks;
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
    public void add(TrackSimplified trackSimplified) {
        add(trackSimplified.getId());
    }

    // remove an id from the buffer
    public void remove(String id) {
        ids.remove(id);
    }

    // addAll
    public void addAll(TrackSimplified[] trackSimplifieds) {
        for (TrackSimplified ts : trackSimplifieds) {
            add(ts);
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
    public boolean contains(TrackSimplified trackSimplified) {
        return contains(trackSimplified.getId());
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

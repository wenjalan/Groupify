package wenjalan.groupify;

import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.specification.Playlist;
import com.wrapper.spotify.model_objects.specification.Track;

import java.io.IOException;
import java.util.*;

public class PlaylistGenerator {

    // the Spotify API
    private SpotifyApi spotify;

    // the Set of Users to generate a playlist for
    private Set<GroupifyUser> users;

    // constructor
    // spotify: the (authenticated) API to create the playlist with
    // users: the users the playlist is to be based on
    public PlaylistGenerator(SpotifyApi spotify, Set<GroupifyUser> users) {
        this.spotify = spotify;
        this.users = users;
    }

    // creates a playlist based off the users' tastes
    // how songs are selected:
    // 1. a top song is a shared by all users
    // 2. a top song's artist is a top artist of all users
    // 3. a top song's artist is of a top genre for all users
    public Playlist createPlaylist() {
        try {
            // get the host's id
            String hostId = spotify.getCurrentUsersProfile().build().execute().getId();

            // create the playlist
            Playlist playlist = this.spotify.createPlaylist(hostId, "Groupify Playlist")
                    .collaborative(false)
                    .description(generatePlaylistDescription())
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

    // generates the playlist's description
    private String generatePlaylistDescription() {
        Iterator<GroupifyUser> iter = users.iterator();
        String desc = "A Groupify playlist for " + iter.next().getDisplayName();
        while (iter.hasNext()) {
            String nextName = iter.next().getDisplayName();
            // if this wasn't the last name
            if (iter.hasNext()) {
                desc += ", " + nextName;
            }
            // if this is the last name
            else {
                desc += " and " + nextName + ".";
            }
        }
        return desc;
    }

    // returns the shared top songs of a Set of GroupifyUsers
    // threshold: the number of users that must have a song in their top tracks for it to be considered
    private static List<Track> getSharedTopSongs(Set<GroupifyUser> users, int threshold) {
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
    private static List<Track> getSharedTopArtistsSongs(Set<GroupifyUser> users) {
        // TODO: this
        return new ArrayList<>();
    }

    // returns the top songs of a set of GroupifyUsers whose artist contains genres which are top genres for all users
    private static List<Track> getSharedTopGenresSongs(Set<GroupifyUser> users) {
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

}

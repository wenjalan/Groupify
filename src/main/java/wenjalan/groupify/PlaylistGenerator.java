package wenjalan.groupify;

import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.specification.*;
import com.wrapper.spotify.requests.data.browse.GetRecommendationsRequest;

import java.io.IOException;
import java.util.*;

public class PlaylistGenerator {

    // the Spotify API
    private SpotifyApi spotify;

    // the threshold for track property sharing
    public static final int THRESHOLD = 2;

    // the size of the playlist
    public static final int PLAYLIST_SIZE = 80;

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
        // announce
        System.out.print("> generating playlist");
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
            // String playlistUrl = idontknowhowtogetthis;

            // the set of songs (there should be no duplicate songs
            Set<Track> songs = new HashSet<>();

            // 1. find top songs shared by all users
            List<Track> sharedTopSongs = getSharedTopSongs(users, THRESHOLD);
            songs.addAll(sharedTopSongs);

            // 2. find top songs whose artist is a top artist of all users
            List<Track> sharedArtistSongs = getSharedTopArtistsSongs(users, THRESHOLD);
            songs.addAll(sharedArtistSongs);
            System.out.print(".");

            // 3. find top songs whose artist has genres shared by all users
            List<Track> sharedGenreSongs = getSharedTopGenresSongs(users, THRESHOLD);
            songs.addAll(sharedGenreSongs);
            System.out.print(".");

            // 4. fill in the rest of the playlist
            int num = PLAYLIST_SIZE - songs.size();
            List<Track> recommendations = getRecommendations(songs, num);
            songs.addAll(recommendations);
            System.out.print(".");

            // adds the songs to the playlist
            String[] uris = getUris(songs);
            if (uris.length > 0) {
                this.spotify.addTracksToPlaylist(playlistId, uris).build().execute();
            }
            else {
                System.out.println("> no songs found in common, playlist will be empty");
            }

            // print to console
            System.out.println(" done!");
            System.out.println("> created Groupify playlist with id " + playlistId);
            // System.out.println("> link: " + playlistUrl);

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
    private List<Track> getSharedTopSongs(Set<GroupifyUser> users, int threshold) {
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
        Set<String> added = new HashSet<>();
        for (String id : trackOccurrences.keySet()) {
            // if the song was alrady added skip it
            if (added.contains(id)) {
                continue;
            }
            Track t = trackLibrary.get(id);
            // if the song occurs <threshold> or more times, add it
            if (trackOccurrences.get(id) >= threshold) {
                sharedSongs.add(t);
                added.add(id);
            }
        }

        // return the list of tracks
        return sharedSongs;
    }

    // returns the top songs of a Set of GroupifyUsers whose artist is a top artist for <threshold> users
    // for songs with multiple artists, checks if any artist on the song is a top artist for <threshold> users
    // threshold: the number of users that must share an artist for a track to be considered
    private List<Track> getSharedTopArtistsSongs(Set<GroupifyUser> users, int threshold) {
        // the list to return
        List<Track> songs = new ArrayList<>();

        // everyone's top songs, mapped by IDs and their Track objects
        Map<String, Track> allSongs = getTopSongs(users);

        // keep track of the artists seen and for how many users have them
        Map<String, Integer> artistOccurrences = new HashMap<>();

        // map everyone's top artists
        for (GroupifyUser user : users) {
            for (Artist a : user.getTopArtists()) {
                String id = a.getId();
                if (!artistOccurrences.containsKey(id)) {
                    artistOccurrences.put(id, 1);
                }
                else {
                    artistOccurrences.put(id, artistOccurrences.get(id) + 1);
                }
            }
        }

        // add songs where the artist is seen at least <threshold> times
        Set<String> added = new HashSet<>();
        for (String trackId : allSongs.keySet()) {
            Track t = allSongs.get(trackId);
            // if the song was already added skip it
            if (added.contains(trackId)) {
                continue;
            }
            // for each artist on the track
            for (ArtistSimplified a : t.getArtists()) {
                // if an artist is seen <threshold> or more times, add that track and break the loop
                if (artistOccurrences.containsKey(a.getId()) && artistOccurrences.get(a.getId()) >= threshold) {
                    songs.add(t);
                    added.add(trackId);
                    break;
                }
            }
        }

        // return the list of songs
        return songs;
    }

    // returns the top songs of a set of GroupifyUsers whose artist contains genres which are top genres for all users
    // threshold: the number of users that must share a specific genre for it to be considered
    private List<Track> getSharedTopGenresSongs(Set<GroupifyUser> users, int threshold) {
        // the list of tracks to return
        List<Track> songs = new ArrayList<>();

        // map everyone's top genres
        Map<String, Integer> genreOccurrences = new HashMap<>();
        for (GroupifyUser user : users) {
            for (String genre : user.getTopGenres()) {
                if (!genreOccurrences.containsKey(genre)) {
                    genreOccurrences.put(genre, 1);
                }
                else {
                    genreOccurrences.put(genre, genreOccurrences.get(genre) + 1);
                }
            }
        }

        // get everyone's top songs
        Map<String, Track> topSongs = getTopSongs(users);

        // for each song, if an artist has a genre that has <threshold> or more occurrences, add it to the list
        try {
            Set<String> added = new HashSet<>();
            for (String trackId : topSongs.keySet()) {
                // if we've already added this track skip it
                if (added.contains(trackId)) {
                    continue;
                }
                Track t = topSongs.get(trackId);
                for (ArtistSimplified artist : t.getArtists()) {
                    String[] genres = this.spotify.getArtist(artist.getId()).build().execute().getGenres();
                    // for each genre
                    for (String genre : genres) {
                        // if the genre occurs more than threshold times, add it and break
                        if (genreOccurrences.containsKey(genre) && genreOccurrences.get(genre) >= threshold) {
                            songs.add(t);
                            added.add(trackId);
                            break;
                        }
                    }
                }
            }
        } catch (SpotifyWebApiException | IOException e) {
            System.err.println("!!! playlist generation error: getSharedTopGenreSongs encountered an issue: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
        // return the tracks
        return songs;
    }

    // returns a list of recommended songs songs based on a given list of songs
    // songs: the list of songs
    // limit: the number of songs to recommend
    private List<Track> getRecommendations(Set<Track> tracks, int limit) {
        // if we're supposed to get nothing, return nothing
        if (limit == 0) {
            return Collections.emptyList();
        }

        // the list of songs to return
        List<Track> songs = new ArrayList<>();

        // generate a list of track ids based off of tracks
        StringBuilder trackIds = new StringBuilder();
        Iterator<Track> iter = tracks.iterator();
        for (int i = 0; i < 5 && iter.hasNext(); i++) {
            Track t = iter.next();
            trackIds.append(t.getId() + ",");
        }

        // create a request
        GetRecommendationsRequest request = spotify.getRecommendations()
                .limit(limit)
                .seed_tracks(trackIds.toString())
                .build();

        // run the request
        try {
            TrackSimplified[] recommendations = request.execute().getTracks();
            // add all the songs
            for (TrackSimplified ts : recommendations) {
                Track realTrack = spotify.getTrack(ts.getId()).build().execute();
                songs.add(realTrack);
            }
        } catch (SpotifyWebApiException | IOException e) {
            System.err.println("!!! error getting recommendations: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }

        // return the songs
        return songs;
    }


    // returns a Map of the top songs of a set of users
    private Map<String, Track> getTopSongs(Set<GroupifyUser> users) {
        Map<String, Track> allSongs = new HashMap<>();
        // get everyone's top songs together
        for (GroupifyUser user : users) {
            for (Track t : user.getTopTracks()) {
                // save the song to allSongs
                String trackId = t.getId();
                if (!allSongs.keySet().contains(trackId)) {
                    allSongs.put(trackId, t);
                }
            }
        }
        return allSongs;
    }

    // returns an array of URIs given a list of tracks
    private String[] getUris(Set<Track> tracks) {
        String[] uris = new String[tracks.size()];
        Iterator<Track> iter = tracks.iterator();
        int i = 0;
        while (iter.hasNext()) {
            uris[i] = iter.next().getUri();
            i++;
        }
        return uris;
    }

}

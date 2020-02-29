package wenjalan.groupify.service;

import com.google.gson.JsonArray;
import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.specification.*;
import com.wrapper.spotify.requests.data.browse.GetRecommendationsRequest;
import wenjalan.groupify.service.model.Party;
import wenjalan.groupify.service.model.GroupifyUser;
import wenjalan.groupify.service.util.GetTrackBuffer;
import wenjalan.groupify.service.util.PlaylistConfiguration;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class PlaylistGenerator {

    // whether or not we're in debug mode
    public final boolean DEBUG_MODE;

    // the prefix we use for debug logs
    public final String DEBUG_PREFIX = "D >> ";

    // the Spotify API
    private SpotifyApi spotify;

//    // the threshold for track property sharing
//    public static final int THRESHOLD = 2;
//
//    // the minimum size of the playlist
//    public static final int MIN_PLAYLIST_SIZE = 100;
//
//    // the maximum size of the playlist
//    public static final int MAX_PLAYLIST_SIZE = 300;

    // the Set of Users to generate a playlist for
    private List<GroupifyUser> users;

    // debug constructor
    // spotify: the (authenticated) API to create the playlist with
    // users: the users the playlist is to be based on
    public PlaylistGenerator(Party party, boolean debugMode) {
        this.spotify = party.getHost().getApiInstance();
        this.users = party.getUsers();
        this.DEBUG_MODE = debugMode;
    }

    // creates a playlist based off the users' tastes
    // how songs are selected:
    // 1. a top song is a shared by THRESHOLD or more users
    // 2. a top song's artist is a top artist of THRESHOLD or more users
    // 3. a top song's artist is of a top genre of THRESHOLD or more users
    // TODO: these
    // 4. a track is a saved track of THRESHOLD or more users
    // 5. a track is in a playlist of THRESHOLD or more users
    // TODO: future improvements
    // - let host user decide what genres the playlist should have
    public Playlist createPlaylist(PlaylistConfiguration config) {
        // announce
        // System.out.println("> generating playlist...");
        try {
            // get the host's id
            String hostId = spotify.getCurrentUsersProfile().build().execute().getId();

            // the set of songs (there should be no duplicate songs)
            Set<Track> songs = new HashSet<>();

            // 1. find top songs shared by config.strictness users
            List<Track> sharedTopSongs = getSharedTopSongs(users, config.strictness);
            songs.addAll(sharedTopSongs);

            // 2. find top songs whose artist is a top artist of config.strictness users
            List<Track> sharedArtistSongs = getSharedTopArtistsSongs(users, config.strictness);
            songs.addAll(sharedArtistSongs);

            // 3. find top songs whose artist has genres shared by config.strictness users
            List<Track> sharedGenreSongs = getSharedTopGenresSongs(users, config.strictness);
            songs.addAll(sharedGenreSongs);

            // 4. fill in the rest of the playlist up to the min if the config wants to
            if (config.doRecommendations) {
                int num = config.playlistSize - songs.size();
                List<Track> recommendations = null;
                if (num > 0) {
                    recommendations = getRecommendations(songs, num);
                    songs.addAll(recommendations);
                    System.out.println(DEBUG_PREFIX + recommendations.size() + " song recommendations added:");
                    recommendations.stream().map(Track::getName).forEach(x -> System.out.println(DEBUG_PREFIX + "\t" + x));
                }
            }

            // debug logging
            if (DEBUG_MODE) {
                // threshold
                System.out.println(DEBUG_PREFIX + "current user shared trait threshold: " + config.strictness);
                System.out.println();

                // top songs
                System.out.println(DEBUG_PREFIX + sharedTopSongs.size() + " shared top songs:");
                sharedTopSongs.stream().map(Track::getName).forEach(x -> System.out.println(DEBUG_PREFIX + "\t" + x));
                System.out.println();

                // top artists
                System.out.println(DEBUG_PREFIX + sharedArtistSongs.size() + " shared top artists songs:");
                sharedArtistSongs.stream().map(Track::getName).forEach(x -> System.out.println(DEBUG_PREFIX + "\t" + x));
                System.out.println();

                // top genres
                System.out.println(DEBUG_PREFIX + sharedGenreSongs.size() + " shared top genre songs:");
                sharedGenreSongs.stream().map(Track::getName).forEach(x -> System.out.println(DEBUG_PREFIX + "\t" + x));
                System.out.println();
            }

            // get the uris
            List<String> uris = getUris(songs);

            // if we have too many, cut it down
            if (uris.size() > config.playlistSize) {
                uris = uris.subList(0, config.playlistSize);
            }

            // debug logging
            if (DEBUG_MODE) {
                System.out.println(DEBUG_PREFIX + uris.size() + " final track uris:");
                for (String uri : uris) {
                    System.out.println(DEBUG_PREFIX + "\t" + uri);
                }
            }

            // create the playlist
            Playlist playlist = this.spotify.createPlaylist(hostId, "Groupify Playlist")
                    .collaborative(false)
                    .description(generatePlaylistDescription())
                    .build()
                    .execute();
            String playlistId = playlist.getId();

            // add the songs in batches of 100 or less
            for (int i = 0; i < uris.size(); i += 100) {
                List<String> smallList = uris.subList(i, Math.min(i + 100, uris.size()));
                JsonArray jsonArray = new JsonArray();
                for (String uri : smallList) {
                    jsonArray.add(uri);
                }
                this.spotify.addTracksToPlaylist(playlistId, jsonArray).build().execute();
            }

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

    // 1.
    // returns the shared top songs of a Set of GroupifyUsers
    // threshold: the number of users that must have a song in their top tracks for it to be considered
    private List<Track> getSharedTopSongs(List<GroupifyUser> users, int threshold) {
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

    // 2.
    // returns the top songs of a Set of GroupifyUsers whose artist is a top artist for <threshold> users
    // for songs with multiple artists, checks if any artist on the song is a top artist for <threshold> users
    // threshold: the number of users that must share an artist for a track to be considered
    private List<Track> getSharedTopArtistsSongs(List<GroupifyUser> users, int threshold) {
        // the list to return
        List<Track> songs = new ArrayList<>();

        // everyone's top songs, mapped by IDs and their Track objects
        Map<String, Track> allSongs = getTopSongs(users);

        // keep track of the artists seen and for how many users have them
        Map<String, Integer> artistOccurrences = new HashMap<>();

        // keep track of artist ids to their actual artist objects (for debugging)
        Map<String, Artist> idToArtist = new HashMap<>();

        // map everyone's top artists
        for (GroupifyUser user : users) {
            for (Artist a : user.getTopArtists()) {
                String id = a.getId();
                if (!artistOccurrences.containsKey(id)) {
                    artistOccurrences.put(id, 1);
                    idToArtist.put(id, a);
                }
                else {
                    artistOccurrences.put(id, artistOccurrences.get(id) + 1);
                }
            }
        }

        // debug logging
        if (DEBUG_MODE) {
            System.out.println(DEBUG_PREFIX + "group top artists:");
            for (String id : artistOccurrences.keySet()) {
                System.out.println(DEBUG_PREFIX + "\t" + artistOccurrences.get(id) + " : " + idToArtist.get(id).getName());
            }
            System.out.println();
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

    // 3.
    // returns the top songs of a set of GroupifyUsers whose artist contains genres which are top genres for all users
    // threshold: the number of users that must share a specific genre for it to be considered
    private List<Track> getSharedTopGenresSongs(List<GroupifyUser> users, int threshold) {
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

        // debug logging
        if (DEBUG_MODE) {
            System.out.println(DEBUG_PREFIX + "group top genres:");
            for (String genre : genreOccurrences.keySet().stream().sorted().collect(Collectors.toList())) {
                System.out.println(DEBUG_PREFIX + "\t" + genreOccurrences.get(genre) + " : " + genre);
            }
            System.out.println();
        }

        // get everyone's top songs
        Map<String, Track> topSongs = getTopSongs(users);

        // get everyone's top artists
        Map<String, Artist> topArtists = getTopArtists(users);

        // find tracks that have <threshold> or more users sharing that genre
        Set<String> added = new HashSet<>();
        for (String trackId : topSongs.keySet()) {
            // if we've already added this track skip it
            if (added.contains(trackId)) {
                continue;
            }

            // get the artists of this track
            Track t = topSongs.get(trackId);
            ArtistSimplified[] artistsSimplified = t.getArtists();
            // get the real artist objects
            List<Artist> artists = new ArrayList<>();
            for (ArtistSimplified as : artistsSimplified) {
                artists.add(topArtists.get(as.getId()));
            }

            // for each artist
            for (Artist artist : artists) {
                // if the artist isn't null (why can an artist be null?)
                if (artist == null) {
                    continue;
                }
                String[] genres = artist.getGenres();
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

        // return the tracks
        return songs;
    }

    // last.
    // returns a list of recommended songs songs based on a given list of songs
    // songs: the list of songs
    // limit: the number of songs to recommend
    private List<Track> getRecommendations(Set<Track> tracks, int limit) {
        // if we're supposed to get nothing, return nothing
        if (limit <= 0) {
            return Collections.emptyList();
        }
        // if we're given no tracks, throw an exception
        if (tracks.isEmpty()) {
            throw new IllegalArgumentException("tracks cannot be empty");
        }
        // if the limit is above the max spotify wants
        if (limit > 100) {
            throw new IllegalArgumentException("cannot retrieve more than 100 recommendations");
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
            // add all recommended songs
            TrackSimplified[] recommendations = request.execute().getTracks();
            GetTrackBuffer buffer = new GetTrackBuffer(spotify);
            buffer.addAll(recommendations);
            List<Track> recTracks = buffer.flush();
            songs.addAll(recTracks);
        } catch (SpotifyWebApiException | IOException e) {
            System.err.println("!!! error getting recommendations: " + e.getMessage());
            e.printStackTrace();
            System.err.println("seed track ids: " + trackIds.toString().replace(",", ",\n"));
            return Collections.emptyList();
        }

        // return the songs
        return songs;
    }


    // returns a Map of the top songs of a set of users
    private Map<String, Track> getTopSongs(List<GroupifyUser> users) {
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

    // returns a Map of top artist ids to their Artist objects
    private Map<String, Artist> getTopArtists(List<GroupifyUser> users) {
        Map<String, Artist> allArtists = new HashMap<>();
        // for each user, get their top artists and map them
        for (GroupifyUser user : users) {
            for (Artist a : user.getTopArtists()) {
                String artistId = a.getId();
                if (!allArtists.keySet().contains(artistId)) {
                    allArtists.put(artistId, a);
                }
            }
        }
        // return the map
        return allArtists;
    }

    // returns an array of URIs given a list of tracks
    private List<String> getUris(Iterable<Track> tracks) {
        List<String> uris = new ArrayList<>();
        Iterator<Track> iter = tracks.iterator();
        while (iter.hasNext()) {
            Track t = iter.next();
            if (t == null) {
                throw new IllegalStateException("track is null");
            }
            uris.add(t.getUri());
        }
        return uris;
    }

    // returns a comma-separated list of artist ids given an array of SimplifiedArtists
    private String getIdsAsString(ArtistSimplified[] artists) {
        if (artists.length == 0) {
            return "";
        }
        else if (artists.length == 1) {
            return artists[0].getId();
        }
        else {
            String ids = "";
            for (ArtistSimplified a : artists) {
                ids += a.getId() + ",";
            }
            return ids.substring(0, ids.length() - 2); // cut off the end comma
            // return ids;
        }
    }

    // returns a comma-separated list of track ids given an array of SimplifiedTracks
    private String getIdsAsString(TrackSimplified[] tracks) {
        if (tracks.length == 0) {
            return "";
        }
        else if (tracks.length == 1) {
            return tracks[0].getId();
        }
        else {
            String ids = "";
            for (TrackSimplified t : tracks) {
                ids += t.getId() + ",";
            }
            return ids.substring(0, ids.length() - 2); // cut off the end comma
            // return ids;
        }
    }

}

package wenjalan.groupify.service.util;

// specifies certain properties on how a playlist is created
public class PlaylistConfiguration {

    // builder
    public static class Builder {

        // fields
        private int playlistMaxSize = 80;
        private boolean recommendations = true;
        private int strictness = 2;

        // constructor
        public Builder() {
            // empty
        }

        // playlistMaxSize
        public void playlistMaxSize(int size) {
            this.playlistMaxSize = size;
        }

        // recommendations
        public void doRecommendations(boolean b) {
            this.recommendations = b;
        }

        // strictness
        public void strictness(int strictness) {
            this.strictness = strictness;
        }

        // build
        public PlaylistConfiguration build() {
            return new PlaylistConfiguration(playlistMaxSize, recommendations, strictness);
        }

    }

    // properties
    public final int playlistSize; // the maximum size of the playlist
    public final boolean doRecommendations; // whether or not to add recommendations to the playlist
    public final int strictness; // the threshold of sharing used by the generator

    // constructor
    private PlaylistConfiguration(int playlistSize, boolean doRecommendations, int strictness) {
        this.playlistSize = playlistSize;
        this.doRecommendations = doRecommendations;
        this.strictness = strictness;
    }

}

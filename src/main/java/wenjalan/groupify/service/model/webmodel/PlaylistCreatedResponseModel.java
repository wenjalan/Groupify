package wenjalan.groupify.service.model.webmodel;

// represents the response sent when a playlist is created successfully
public class PlaylistCreatedResponseModel extends WebModel {

    // fields
    public final String playlistUrl;

    // constructor: with playlist object
    public PlaylistCreatedResponseModel(String playlistUrl) {
        this.playlistUrl = playlistUrl;
    }

}

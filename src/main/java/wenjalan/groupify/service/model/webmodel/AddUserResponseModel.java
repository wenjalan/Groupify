package wenjalan.groupify.service.model.webmodel;

// represents the object sent as a response to a add user request
public class AddUserResponseModel extends WebModel {

    // fields
    public final String authUrl;

    // constructor
    public AddUserResponseModel(String authUrl) {
        this.authUrl = authUrl;
    }

}

package wenjalan.groupify.service.model.webmodel;

// represents the response sent to a Create Party request
public class CreatePartyResponseModel extends WebModel {

    // fields
    public final String authUrl;

    // constructor: auth url
    public CreatePartyResponseModel(String authUrl) {
        this.authUrl = authUrl;
    }

}

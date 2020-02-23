package wenjalan.groupify.service.model.webmodel;

import com.google.gson.Gson;

// represents an object meant to be represented by JSON, for communication in HTML
public class WebModel {

    // returns the JSON representation of this object
    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

}

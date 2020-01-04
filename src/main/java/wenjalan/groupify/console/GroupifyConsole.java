package wenjalan.groupify.console;

import okhttp3.*;
import wenjalan.groupify.service.GroupifyController;

import java.io.IOException;
import java.util.Map;

// the console-based client of Groupify, used for development purposes
public class GroupifyConsole {

    // Groupify Service url
    public static final String GROUPIFY_SERVICE_URL = "http://24.16.66.0:1000/api/action";

    // JSON MediaType
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // client
    OkHttpClient client = new OkHttpClient();

    // client entry point
    public static void main(String[] args) throws IOException {
        GroupifyConsole groupifyConsole = new GroupifyConsole();
        String response = groupifyConsole.get(GROUPIFY_SERVICE_URL, "action", "create");
        if (response.isEmpty()) {
            System.out.println("no response");
        }
        else {
            System.out.println(response);
        }
        String partyId = response.split("&state=")[1].substring(0, 5);
        System.out.println("party id: " + partyId);
    }

    // get
    private String get(String url, String name, String value) throws IOException {
        HttpUrl req_url = HttpUrl.parse(url).newBuilder()
                .addQueryParameter(name, value)
                .build();
        Request request = new Request.Builder().url(req_url).build();
        System.out.println("request: " + request.url());
        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
    }

}

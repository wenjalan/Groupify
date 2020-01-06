package wenjalan.groupify.console;

import okhttp3.*;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

// the console-based client of Groupify, used for development purposes
public class GroupifyConsole {

    // Groupify Service url
    public static final String GROUPIFY_SERVICE_URL = "http://localhost:1000/api/action";

    // JSON MediaType
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // client
    OkHttpClient client = new OkHttpClient();

    // client entry point
    public static void main(String[] args) {
        new GroupifyConsole();
    }

    // constructor
    public GroupifyConsole() {
        // new Scanner for console input
        Scanner console = new Scanner(System.in);
        boolean running = true;
        String partyId = "";

        // print commands
        System.out.println("COMMANDS:\n" +
                "\tCREATE (create a new party)\n" +
                "\tADD (add a new user to the party)\n" +
                "\tMAKE (make the playlist)\n" +
                "\tCLEAR (remove all users except the host from the party)\n" +
                "\tPURGE (unfollow all Groupify Playlists on the host's account)\n" +
                "\tINFO (print information about all users in the party)\n" +
                "\tQUIT (stop the application)");

        // while the program should run
        while (running) {
            // get the response to parse
            String response = console.nextLine();

            // create
            if (response.equalsIgnoreCase("create")) {
                // send request and get response
                String serverResponse = getAction("create", null);
                // if it was successful, save the party id
                if (serverResponse != null) {
                    // save the party id
                    partyId = serverResponse.split("&state=")[1].substring(0, 5);
                    // print the auth URI to the console
                    System.out.println("host authorization uri: " + serverResponse);
                }

            }
            // add
            else if (response.equalsIgnoreCase("add")) {
                // send request and get response
                String serverResponse = getAction("add", partyId);
                // if it was successful, print the auth URI to the console
                if (serverResponse != null) {
                    System.out.println("guest authorization uri: " + serverResponse);
                }
            }
            // make
            else if (response.equalsIgnoreCase("make")) {
                String serverResponse = getAction("make", partyId);
                if (Boolean.parseBoolean(serverResponse)) {
                    System.out.println("playlist created successfully");
                }
                else {
                    System.out.println("playlist wasn't created, an error occured");
                }
            }
            // clear
            else if (response.equalsIgnoreCase("clear")) {
                String serverResponse = getAction("clear", partyId);
                System.out.println(serverResponse);
            }
            // info
            else if (response.equalsIgnoreCase("info")) {
                String serverResponse = getAction("info", partyId);
                System.out.println(serverResponse);
            }
            // purge
            else if (response.equalsIgnoreCase("purge")) {
                String serverResponse = getAction("purge", partyId);
                System.out.println(serverResponse);
            }
            // quit
            else if (response.equalsIgnoreCase("quit")) {
                // close the party
                if (!partyId.isEmpty()) {
                    String serverResponse = getAction("stop", partyId);
                    System.out.println(serverResponse);
                }
                running = false;
            }
            // not recognized
            else {
                System.out.println("command not recognized: " + response);
            }
        }
    }

    // get
    private String getAction(String action, String partyId) {
        HttpUrl req_url = HttpUrl.parse(GROUPIFY_SERVICE_URL).newBuilder()
                .addQueryParameter("action", action)
                .addQueryParameter("party", partyId)
                .build();
        Request request = new Request.Builder().url(req_url).build();
        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        } catch (IOException e) {
            System.err.println("error sending request: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

}

package wenjalan.groupify.service;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// GroupifyController handles the HTTP interfacing of the GroupifyService, including authentication callback
@RestController
public class GroupifyController {

    // the possible actions a client can ask of the service
    public enum Action {

        // create a new party
        CREATE,

        // add a guest to the party
        ADD,

        // remove a guest from the party
        REMOVE,

        // clear all guests from the party
        CLEAR,

        // get information about a user in the party
        INFO,

        // purge Groupify playlists from the host's account
        PURGE,

        // stops the session with the API
        STOP,

    }

    // the PartyManager instance
    private final PartyManager partyManager;

    // constructor
    public GroupifyController(PartyManager partyManager) {
        this.partyManager = partyManager;
    }

    // receives a Spotify Authentication Callback
    @RequestMapping(value = "api/callback")
    public String callback(
            @RequestParam(value = "code", defaultValue = "") String code,
            @RequestParam(value = "state", defaultValue = "") String state) {
        // if the code or state returned nothing, return an error message
        if (code.isEmpty()) {
            return "error retrieving code";
        }
        else if (state.isEmpty()) {
            return "error retrieving state";
        }

        // find the party id and execute the corresponding listener
        String partyId = state.split(":")[0];
        for (AuthenticationListener listener : GroupifyService.AUTHENTICATION_LISTENERS) {
            // if the ids match
            if (listener.partyId().equals(partyId)) {
                // execute that listener
                listener.onAuthenticationSuccess(code, state);
                // return happy message
                return "thanks. you can close this now.";
            }
        }

        // if no party with the id was found return error
        return "error retrieving party with id " + partyId;
    }

}

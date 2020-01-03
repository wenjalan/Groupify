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
    @RequestMapping(value = "/callback")
    public String callback(
            @RequestParam(value = "code", defaultValue = "") String code,
            @RequestParam(value = "state", defaultValue = "") String state) {
        // if the code or state returned nothing, return an error message
        if (code.isEmpty() || state.isEmpty()) {
            return "error retrieving code";
        }

        // find which groupifyParty this code belongs to
        // groupifyParty id == last 4 digits of state
        String partyId = state.substring(state.length() - 5);
        GroupifyParty groupifyParty = partyManager.getParty(partyId);

        // if none was found, throw an error
        if (groupifyParty == null) {
            return "error retrieving groupifyParty";
        }

        // otherwise, do nothing because we haven't written this part yet
        // todo
        return "thanks, you can close this now";
    }

}

package wenjalan.groupify;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GroupifyCallback {

    // the PartyManager instance
    private final PartyManager partyManager;

    // constructor
    public GroupifyCallback(PartyManager partyManager) {
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

        // find which party this code belongs to
        // party id == last 4 digits of state
        String partyId = state.substring(state.length() - 5);
        Party party = partyManager.getParty(partyId);

        // if none was found, throw an error
        if (party == null) {
            return "error retrieving party";
        }

        // otherwise, do nothing because we haven't written this part yet
        // todo
        return "thanks, you can close this now";
    }

}

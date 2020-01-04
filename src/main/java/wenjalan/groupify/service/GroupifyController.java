package wenjalan.groupify.service;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

// GroupifyController handles the HTTP interfacing of the GroupifyService, including authentication callback
@RestController
public class GroupifyController {

    // the possible actions a client can ask of the service
    public enum Action {

        // create a new party
        CREATE {
            // returns: an authorization URI for the user to click on
            @Override
            String run() {
                GroupifyService service = GroupifyService.getInstance();
                URI uri = service.createParty();
                return uri.toString();
            }
        },

        // add a guest to the party
        ADD {
            @Override
            String run() {
                return null;
            }
        },

        // remove a guest from the party
        REMOVE {
            @Override
            String run() {
                return null;
            }
        },

        // clear all guests from the party
        CLEAR {
            @Override
            String run() {
                return null;
            }
        },

        // get information about a user in the party
        INFO {
            @Override
            String run() {
                return null;
            }
        },

        // purge Groupify playlists from the host's account
        PURGE {
            @Override
            String run() {
                return null;
            }
        },

        // stops the session with the API
        STOP {
            @Override
            String run() {
                return null;
            }
        };

        // methods
        abstract String run();

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

    // receives an Action request
    @RequestMapping(value = "api/action")
    public String action(@RequestParam(value = "action", defaultValue = "") String action) {
        // if no action was specified, return an error
        if (action.isEmpty()) {
            return "no action specified, please specify an action";
        }

        // find which action was requested
        for (Action a : Action.values()) {
            // if the action requested matches a command
            if (action.equalsIgnoreCase(a.name())) {
                // run it and return its response
                return a.run();
            }
        }

        // if none was found, return an error
        return "unrecognized action: " + action;
    }

}

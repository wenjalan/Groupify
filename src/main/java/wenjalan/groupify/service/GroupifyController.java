package wenjalan.groupify.service;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import wenjalan.groupify.service.model.Party;
import wenjalan.groupify.service.model.GroupifyUser;
import wenjalan.groupify.service.model.webmodel.AddUserResponseModel;
import wenjalan.groupify.service.model.webmodel.CreatePartyResponseModel;
import wenjalan.groupify.service.model.webmodel.PartyWebModel;
import wenjalan.groupify.service.model.webmodel.PlaylistCreatedResponseModel;

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
            String run(Party party) {
                GroupifyService service = GroupifyService.getInstance();
                URI uri = service.createParty();
                return uri.toString();
            }
        },

        // add a guest to the party
        ADD {
            @Override
            String run(Party party) {
                GroupifyService service = GroupifyService.getInstance();
                URI uri = service.addUserToParty(party);
                return uri.toString();
            }
        },

        // clear all guests from the party
        CLEAR {
            @Override
            String run(Party party) {
                GroupifyService service = GroupifyService.getInstance();
                service.clearParty(party);
                return "success";
            }
        },

        // get information about a user in the party
        INFO {
            @Override
            String run(Party party) {
                // string together each user's information
                StringBuilder sb = new StringBuilder();
                for (GroupifyUser user : party.getUsers()) {
                    sb.append(user.getInfo() + "\n");
                }
                return sb.toString();
            }
        },

        // makes the playlist on the host's account
        MAKE {
            @Override
            String run(Party party) {
                return "" + GroupifyService.getInstance().makePlaylist(party);
            }
        },

        // purge Groupify playlists from the host's account
        PURGE {
            @Override
            String run(Party party) {
                GroupifyService service = GroupifyService.getInstance();
                // get the host of this party
                GroupifyUser host = party.getHost();
                // purge their playlists
                boolean success = service.purgePlaylists(host);
                // return success
                if (success) {
                    return "success";
                }
                else {
                    return "an error occurred";
                }
            }
        },

        // stops the session with the API
        STOP {
            @Override
            String run(Party party) {
                // get the party manager
                PartyManager manager = PartyManager.getInstance();
                // deregister this party
                manager.unregister(party);
                // return success
                return "success";
            }
        };

        // methods
        abstract String run(Party party);

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
    @CrossOrigin
    @RequestMapping(value = "api/action")
    public String action(
            @RequestParam(value = "action", defaultValue = "") String action,
            @RequestParam(value = "party", defaultValue = "") String party) {
        // if no action was specified, return an error
        if (action.isEmpty()) {
            return "no action specified, please specify an action";
        }

        // find which action was requested
        for (Action a : Action.values()) {
            // if the action requested matches a command
            if (action.equalsIgnoreCase(a.name())) {
                // run it and return its response
                Party p = getParty(party);

                // if no party was found and the action wasn't a create
                if (p == null && !action.equalsIgnoreCase("create")) {
                    // return an error
                    return "no party specified, please specify a party";
                }

                return a.run(p);
            }
        }

        // if none was found, return an error
        return "unrecognized action: " + action;
    }

    // receives a Join party request
    @CrossOrigin
    @RequestMapping(value = "api/join")
    public String join(
            @RequestParam(value = "party", defaultValue = "") String party) {
        // if no party specified, complain
        if (party.isEmpty()) {
            return "please specify a party id";
        }

        // find the party
        Party p = getParty(party);

        // if none found, complain
        if (p == null) {
            return "no party with id " + party + " found";
        }

        // otherwise, return an invite link
        String url = Action.ADD.run(p);
        return url;
    }

    // returns a PartyWebModel JSON given an id
    @CrossOrigin
    @RequestMapping(value = "api/party")
    public PartyWebModel party(
            @RequestParam(value = "id", defaultValue = "") String id) {
        // get the party with that id
        Party p = getParty(id);
        if (p == null) {
            throw new IllegalArgumentException("no party with id " + id + " found");
        }

        // return the JSON representation of that party
        return new PartyWebModel(p);
    }

    // creates a new party
    @CrossOrigin
    @RequestMapping(value = "api/create")
    public CreatePartyResponseModel create() {
        return new CreatePartyResponseModel(Action.CREATE.run(null));
    }

    // adds a user to a party
    @CrossOrigin
    @RequestMapping(value = "api/add")
    public AddUserResponseModel add(@RequestParam(value = "party", defaultValue = "") String partyId) {
        Party p = getParty(partyId);
        if (p == null) {
            throw new IllegalArgumentException("no party with id " + partyId + " found");
        }
        return new AddUserResponseModel(Action.ADD.run(p));
    }

    // makes the playlist given a party
    @CrossOrigin
    @RequestMapping(value = "api/make")
    public PlaylistCreatedResponseModel make(
            @RequestParam(value = "party", defaultValue = "") String partyId){
        // get party
        Party p = getParty(partyId);
        if (p == null) {
            throw new IllegalArgumentException("no party with id " + partyId + " found");
        }

        // return JSON response
        return new PlaylistCreatedResponseModel(Action.MAKE.run(p));
    }

    // returns a party given a String id
    private static Party getParty(String id) {
        return PartyManager.getInstance().getParty(id);
    }

}

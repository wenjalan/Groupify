package wenjalan.groupify.service;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import wenjalan.groupify.service.model.Party;
import wenjalan.groupify.service.model.GroupifyUser;
import wenjalan.groupify.service.model.webmodel.*;

import java.net.URI;

// GroupifyController handles the HTTP interfacing of the GroupifyService, including authentication callback
@RestController
public class GroupifyController {

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
        // get an auth uri for a new party
        GroupifyService g = GroupifyService.getInstance();
        URI uri = g.createParty();
        return new CreatePartyResponseModel(uri.toString());
    }

    // adds a user to a party
    @CrossOrigin
    @RequestMapping(value = "api/add")
    public AddUserResponseModel add(@RequestParam(value = "party", defaultValue = "") String partyId) {
        // get party
        Party p = getParty(partyId);
        if (p == null) {
            throw new IllegalArgumentException("no party with id " + partyId + " found");
        }

        // generate an auth URI for the user for this party
        GroupifyService g = GroupifyService.getInstance();
        URI uri = g.addUserToParty(p);

        // generate an auth URI for the user from this party
        return new AddUserResponseModel(uri.toString());
    }

    // makes the playlist given a party
    @CrossOrigin
    @RequestMapping(value = "api/make")
    public PlaylistCreatedResponseModel make(
            @RequestParam(value = "party", defaultValue = "") String partyId,
            @RequestParam(value = "addRecommendations", defaultValue = "") String addRecommendations){
        // get party
        Party p = getParty(partyId);
        if (p == null) {
            throw new IllegalArgumentException("no party with id " + partyId + " found");
        }

        // make the playlist
        GroupifyService g = GroupifyService.getInstance();
        String url = g.makePlaylist(p);

        // return JSON response
        return new PlaylistCreatedResponseModel(url);
    }

    // delists the party from the service
    @CrossOrigin
    @RequestMapping(value = "api/remove")
    public PartyRemoveResponseModel remove(
            @RequestParam(value = "party", defaultValue = "") String partyId) {
        // get party
        Party p = getParty(partyId);
        if (p == null) {
            throw new IllegalArgumentException("no party with id " + partyId + " found");
        }

        // remove the party from the manager
        PartyManager pm = PartyManager.getInstance();
        pm.unregister(p);
        return new PartyRemoveResponseModel("success");
    }

    // returns a party given a String id
    private static Party getParty(String id) {
        return PartyManager.getInstance().getParty(id);
    }

}

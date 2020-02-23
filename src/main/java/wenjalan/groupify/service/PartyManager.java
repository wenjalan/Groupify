package wenjalan.groupify.service;

import wenjalan.groupify.service.model.Party;

import java.util.HashMap;
import java.util.Map;

// the PartyManager
// singleton
public class PartyManager {

    // the instance of PartyManager
    private static PartyManager instance = null;

    // the Map of party ids to their party instances
    private Map<String, Party> parties = new HashMap<>();

    // private constructor
    private PartyManager() {
        // catch instantiations
        if (instance != null) {
            throw new IllegalStateException("an instance of PartyManager already exists");
        }
    }

    // registers a new Party to this PartyManager
    public void register(Party groupifyParty) {
        this.parties.put(groupifyParty.getId(), groupifyParty);
        System.out.println("> registered new party with id " + groupifyParty.getId());
    }

    // unregisters a Party from this PartyManager
    // also calls the close() method on the party
    public void unregister(Party p) {
        String id = p.getId();
        this.parties.remove(p.getId());
        p.close();
        System.out.println("> unregistered party with id " + id);
    }

    // returns a Party given a party id
    public Party getParty(String id) {
        return this.parties.get(id);
    }

    // the getter for the instance
    public static PartyManager getInstance() {
        if (instance == null) {
            instance = new PartyManager();
        }
        return instance;
    }

}

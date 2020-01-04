package wenjalan.groupify.service;

import java.util.HashMap;
import java.util.Map;

// the PartyManager
// singleton
public class PartyManager {

    // the instance of PartyManager
    private static PartyManager instance = null;

    // the Map of party ids to their party instances
    private Map<String, GroupifyParty> parties = new HashMap<>();

    // private constructor
    private PartyManager() {
        // catch instantiations
        if (instance != null) {
            throw new IllegalStateException("an instance of PartyManager already exists");
        }
    }

    // registers a new GroupifyParty to this PartyManager
    public void register(GroupifyParty groupifyParty) {
        this.parties.put(groupifyParty.getId(), groupifyParty);
        System.out.println("> registered new party with id " + groupifyParty.getId());
    }

    // unregisters a GroupifyParty from this PartyManager
    // also calls the close() method on the party
    public void unregister(GroupifyParty p) {
        this.parties.remove(p.getId());
        p.close();
    }

    // returns a GroupifyParty given a party id
    public GroupifyParty getParty(String id) {
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

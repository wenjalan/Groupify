package wenjalan.groupify;

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
    public void register(Party party) {
        this.parties.put(party.getId(), party);
    }

    // unregisters a Party from this PartyManager
    public void unregister(Party p) {
        this.parties.remove(p.getId());
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

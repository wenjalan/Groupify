package wenjalan.groupify.service;

import wenjalan.groupify.service.model.Party;

import java.util.HashMap;
import java.util.Map;

// the PartyManager
// singleton
public class PartyManager {

    // the amount of time a Party is valid for: 8 hours
    public static final int PARTY_MAX_AGE = 8 * 60 *  60 * 1000;

    // the amount of time the Reaper will go looking for parties: 5 minute intervals
    public static final int REAPER_POLL = 5 * 60 * 1000;

    // the instance of PartyManager
    private static PartyManager instance = null;

    // the Map of party ids to their party instances
    private Map<String, Party> parties = new HashMap<>();

    // party reaper unregisters parties older than PARTY_MAX_AGE
    private Thread reaper;

    // private constructor
    private PartyManager() {
        // catch instantiations
        if (instance != null) {
            throw new IllegalStateException("an instance of PartyManager already exists");
        }

        // start reaper thread
        reaper = new Thread() {
            @Override
            public void run() {
                try {
                    // log
                    System.out.println("> Starting Reaper thread...");
                    // forever
                    for (;;) {
                        // sleep for an interval
                        Thread.sleep(REAPER_POLL);
                        // check that all the parties are still young enough to live
                        long now = System.currentTimeMillis();
                        for (Party p : parties.values()) {
                            // if the party is greater than the max age, deregister it
                            if (now - p.getCreationTimestamp() > PARTY_MAX_AGE) {
                                unregister(p);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    // log error
                    System.err.println("> WARNING: Reaper thread interrupted, attempting to restart...");
                    e.printStackTrace();
                    // restart
                    this.run();
                }
            }
        };
        reaper.start();
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

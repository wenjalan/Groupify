package wenjalan.groupify.service.model;

import wenjalan.groupify.service.PartyManager;

import java.util.*;

// represents a party of users
public class Party {

    // Builder class
    public static class Builder {

        // the party id
        private String id = null;

        // build method
        public Party build(GroupifyUser host) {
            return new Party(host, id);
        }

        // returns the id of the party being created
        public String getId() {
            if (id == null) {
                id = nextId();
            }
            return id;
        }

        // generates the next unique id for a party
        public static String nextId() {
            // generate a random id based off a uuid
            UUID uuid = UUID.randomUUID();
            String id = uuid.toString().substring(0, 5).toUpperCase();

            // keep regenerating the id if it's already been registered
            while (registeredIds.contains(id)) {
                uuid = UUID.randomUUID();
                id = uuid.toString().substring(0, 5).toUpperCase();
            }

            // register the ID as used
            registeredIds.add(id);

            // return the new id
            return id;
        }

    }

    // the set of all currently registered Party ids
    private static Set<String> registeredIds = new HashSet<>();

    // the host user of this party
    private final GroupifyUser host;

    // the List of GroupifyUsers in this party, including the host
    private List<GroupifyUser> users;

    // the unique ID of this party
    private final String id;

    // constructor
    // host: the host user
    // id: the id to give this party
    private Party(GroupifyUser host, String id) {
        this.host = host;
        this.users = new LinkedList<>();
        this.id = id;
        PartyManager.getInstance().register(this);

        // add the host to their own party
        addUser(host);
    }

    // adds a user to this party
    public void addUser(GroupifyUser user) {
        this.users.add(user);
    }

    // removes a user from this party
    public void removeUser(GroupifyUser user) {
        this.users.remove(user);
    }

    // returns the host of this Party
    public GroupifyUser getHost() {
        return this.host;
    }

    // returns a copy of the List of Users in this party
    public List<GroupifyUser> getUsers() {
        return new LinkedList<>(this.users);
    }

    // returns the id of this Party
    public String getId() {
        return this.id;
    }

    // removes an id from the registry
    public static void unregisterId(String id) {
        registeredIds.remove(id);
    }

    // unregisters this party, removing its id from the registry and removing all users
    // should be called whenever a Party is done
    public void close() {
        // unregister the id of this party to free up for future parties
        unregisterId(this.id);

        // empty users
        this.users.clear();
    }

}

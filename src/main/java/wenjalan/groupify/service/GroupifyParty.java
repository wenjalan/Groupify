package wenjalan.groupify.service;

import java.util.*;

// represents a party of users
public class GroupifyParty {

    // the set of all currently registered GroupifyParty ids
    private static Set<String> registeredIds = new HashSet<>();

    // the host user of this party
    private final GroupifyUser host;

    // the List of GroupifyUsers in this party, including the host
    private List<GroupifyUser> users;

    // the unique ID of this party
    private final String id;

    // constructor
    // host: the host user
    public GroupifyParty(GroupifyUser host) {
        this.host = host;
        users = new LinkedList<>();
        id = nextId();
    }

    // constructor
    // host: the host user
    // guests: the guest users
    public GroupifyParty(GroupifyUser host, Iterable<GroupifyUser> guests) {
        this(host);
        for (GroupifyUser g : guests) {
            addUser(g);
        }
    }

    // adds a user to this party
    public void addUser(GroupifyUser user) {
        this.users.add(user);
    }

    // removes a user from this party
    public void removeUser(GroupifyUser user) {
        this.users.remove(user);
    }

    // returns the host of this GroupifyParty
    public GroupifyUser getHost() {
        return this.host;
    }

    // returns a copy of the List of Users in this party
    public List<GroupifyUser> getUsers() {
        return new LinkedList<>(this.users);
    }

    // returns the id of this GroupifyParty
    public String getId() {
        return this.id;
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

    // removes an id from the registry
    public static void unregisterId(String id) {
        registeredIds.remove(id);
    }

    // unregisters this party, removing its id from the registry and removing all users
    // should be called whenever a GroupifyParty is done
    public void close() {
        // unregister the id of this party to free up for future parties
        unregisterId(this.id);

        // empty users
        this.users.clear();
    }

}

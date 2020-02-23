package wenjalan.groupify.service.model.webmodel;

import wenjalan.groupify.service.model.Party;

import java.util.List;
import java.util.stream.Collectors;

// represents a Party JSON Object, meant to be converted to JSON
public class PartyWebModel extends WebModel {

    // fields
    public final String id;
    public final String host;
    public final List<String> users;
    public final long creationTimestamp;

    // constructor: given a Party
    public PartyWebModel(Party p) {
        this.id = p.getId();
        this.host = p.getHost().getDisplayName();
        this.users = p.getUsers().stream().map((user) -> user.getDisplayName()).collect(Collectors.toList());
        this.creationTimestamp = p.getCreationTimestamp();
    }

}

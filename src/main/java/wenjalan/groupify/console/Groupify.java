package wenjalan.groupify.console;

// the Groupify service wrapper
public class Groupify {

    // whether or not we should do verbose logging
    public final boolean VERBOSE;

    // constructor
    // debug: whether or not to do verbose logging
    public Groupify(boolean verbose) {
        this.VERBOSE = verbose;
    }

    // constructor
    public Groupify() {
        this(false);
    }

}

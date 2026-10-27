package com.opslog.location;

/* also for other users' locations */
public class LocationNotFoundException extends RuntimeException {

    public LocationNotFoundException(long id) {
        super("Location " + id + " was not found");
    }
}

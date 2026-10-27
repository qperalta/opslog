package com.opslog.location;

public class LocationReadOnlyException extends RuntimeException {

    public LocationReadOnlyException() {
        super("Kaimana gyms are read-only; create your own location instead");
    }
}

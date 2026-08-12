package com.trafficreplay.testservice.beer;

public class BeerNotFoundException extends RuntimeException {

    public BeerNotFoundException(String id) {
        super("No beer found for id: " + id);
    }
}

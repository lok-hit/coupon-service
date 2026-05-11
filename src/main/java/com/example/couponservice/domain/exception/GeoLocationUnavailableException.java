package com.example.couponservice.domain.exception;

public class GeoLocationUnavailableException extends RuntimeException {
    public GeoLocationUnavailableException(String ip) {
        super("Geo-location service unavailable for IP: " + ip);
    }
}

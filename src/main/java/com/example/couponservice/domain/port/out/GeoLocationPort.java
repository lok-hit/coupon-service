package com.example.couponservice.domain.port.out;

import java.util.Optional;

public interface GeoLocationPort {

    // Returns empty when the geo-location service is unavailable
    Optional<String> getCountryCode(String ip);
}

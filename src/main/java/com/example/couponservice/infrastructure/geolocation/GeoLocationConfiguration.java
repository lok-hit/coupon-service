package com.example.couponservice.infrastructure.geolocation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Spring configuration for the geo-location HTTP client.
 * Separates RestClient construction (which requires GeoLocationProperties)
 * from the GeoLocationClient component so the client can be unit-tested
 * by injecting a RestClient backed by MockRestServiceServer.
 */
@Configuration
class GeoLocationConfiguration {

    /**
     * Creates a {@link RestClient} pre-configured with the provider base URL
     * and connection/read timeouts from {@link GeoLocationProperties}.
     *
     * @param properties validated geolocation configuration
     * @return a ready-to-use, thread-safe RestClient instance
     */
    @Bean
    RestClient geoLocationRestClient(GeoLocationProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) properties.getConnectTimeout().toMillis());
        factory.setReadTimeout((int) properties.getReadTimeout().toMillis());
        return RestClient.builder()
                .baseUrl(properties.getProviderUrl())
                .requestFactory(factory)
                .build();
    }
}
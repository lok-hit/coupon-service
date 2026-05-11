package com.example.couponservice.infrastructure.geolocation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Unit tests for {@link GeoLocationClient}. No Spring context — uses
 * {@link MockRestServiceServer} to intercept RestClient HTTP calls.
 */
class GeoLocationClientTest {

    private static final String BASE_URL = "http://ip-api.com/json";
    private static final String TEST_IP = "1.2.3.4";

    private MockRestServiceServer mockServer;
    private GeoLocationClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl(BASE_URL).build();
        client = new GeoLocationClient(restClient);
    }

    @Test
    void fetchCountry_successResponse_returnsOptionalWithCountryCode() {
        mockServer.expect(requestTo(BASE_URL + "/" + TEST_IP))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        """
                        {"status":"success","countryCode":"PL"}
                        """,
                        MediaType.APPLICATION_JSON));

        Optional<GeoLocationResponse> result = client.fetchCountry(TEST_IP);

        assertThat(result).isPresent();
        assertThat(result.get().countryCode()).isEqualTo("PL");
        assertThat(result.get().success()).isTrue();
        mockServer.verify();
    }

    @Test
    void fetchCountry_http500_returnsEmpty() {
        mockServer.expect(requestTo(BASE_URL + "/" + TEST_IP))
                .andRespond(withServerError());

        Optional<GeoLocationResponse> result = client.fetchCountry(TEST_IP);

        assertThat(result).isEmpty();
        mockServer.verify();
    }

    @Test
    void fetchCountry_timeout_returnsEmpty() {
        mockServer.expect(requestTo(BASE_URL + "/" + TEST_IP))
                .andRespond(request -> {
                    throw new SocketTimeoutException("Read timed out");
                });

        Optional<GeoLocationResponse> result = client.fetchCountry(TEST_IP);

        assertThat(result).isEmpty();
        mockServer.verify();
    }

    @Test
    void fetchCountry_malformedJson_returnsEmpty() {
        mockServer.expect(requestTo(BASE_URL + "/" + TEST_IP))
                .andRespond(withSuccess("{ invalid json {{{{", MediaType.APPLICATION_JSON));

        Optional<GeoLocationResponse> result = client.fetchCountry(TEST_IP);

        assertThat(result).isEmpty();
        mockServer.verify();
    }
}
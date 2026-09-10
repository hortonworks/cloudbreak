package com.sequenceiq.maintenance.dispatcher;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class SubmitterServiceEndpointResolverTest {

    @Test
    void resolveBaseUrlReturnsConfiguredServiceUrl() {
        SubmitterServiceEndpointResolver underTest = new SubmitterServiceEndpointResolver(Map.of(
                "datalake", "http://datalake:8086/dl"));

        assertThat(underTest.resolveBaseUrl("datalake"))
                .contains("http://datalake:8086/dl");
    }

    @Test
    void resolveBaseUrlMapsCloudbreakService() {
        SubmitterServiceEndpointResolver underTest = new SubmitterServiceEndpointResolver(
                "http://cloudbreak:9091/cb",
                "http://datalake:8086/dl",
                "http://freeipa:8090/freeipa");

        assertThat(underTest.resolveBaseUrl("cloudbreak"))
                .contains("http://cloudbreak:9091/cb");
    }

    @Test
    void resolveBaseUrlReturnsEmptyForUnknownService() {
        SubmitterServiceEndpointResolver underTest = new SubmitterServiceEndpointResolver(Map.of());

        assertThat(underTest.resolveBaseUrl("unknown")).isEmpty();
        assertThat(underTest.resolveBaseUrl("")).isEmpty();
        assertThat(underTest.resolveBaseUrl(null)).isEmpty();
    }
}

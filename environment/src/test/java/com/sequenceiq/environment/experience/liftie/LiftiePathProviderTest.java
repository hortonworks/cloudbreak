package com.sequenceiq.environment.experience.liftie;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LiftiePathProviderTest {

    private static final String CLUSTER_ENDPOINT_FORMAT_TEMPLATE = "%s/liftie/api/v1/cluster";

    private static final String LIFTIE_API = "https://localhost:1234";

    private LiftiePathProvider underTest;

    @BeforeEach
    void setUp() {
        underTest = new LiftiePathProvider(LIFTIE_API);
    }

    @Test
    void testGetPathToClustersEndpointShouldReturnTheExpectedlyFormattedPath() {
        String expected = getExpectedClusterEndpointPathString();

        String result = underTest.getPathToClustersEndpoint();

        Assertions.assertEquals(expected, result);
    }

    @Test
    void testGetPathToClusterEndpointShouldReturnTheExpectedlyFormattedPath() {
        String clusterId = "someClusterId";
        String expected = String.format("%s/%s", getExpectedClusterEndpointPathString(), clusterId);

        String result = underTest.getPathToClusterEndpoint(clusterId);

        Assertions.assertEquals(expected, result);
    }

    private String getExpectedClusterEndpointPathString() {
        return String.format(CLUSTER_ENDPOINT_FORMAT_TEMPLATE, LIFTIE_API);
    }

}

package com.sequenceiq.environment.experience.liftie;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LiftiePathProviderTest {

    private static final String CLUSTER_ENDPOINT_FORMAT_TEMPLATE = "%s://%s:%s/liftie/api/v1/cluster";

    private static final String LIFTIE_PORT = "1234";

    private static final String LIFTIE_ADDRESS = "localhost";

    private static final String LIFTIE_PROTOCOL = "https";

    private LiftiePathProvider underTest;

    @BeforeEach
    void setUp() {
        underTest = new LiftiePathProvider(LIFTIE_PORT, LIFTIE_ADDRESS, LIFTIE_PROTOCOL);
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
        return String.format(CLUSTER_ENDPOINT_FORMAT_TEMPLATE, LIFTIE_PROTOCOL, LIFTIE_ADDRESS, LIFTIE_PORT);
    }

}
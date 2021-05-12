package com.sequenceiq.environment.experience.liftie;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.environment.experience.config.ExperiencePathConfig;

class LiftiePathProviderTest {

    private static final String CLUSTER_ENDPOINT_FORMAT_TEMPLATE = "%s/liftie/api/v1/cluster";

    private static final String LIFTIE_API = "https://localhost:1234";

    private static final String LIFTIE_PATH_INFIX = "/liftie/api/v1";

    private static final String LIFTIE_POLICY_ENDPOINT = "/prerequisites?cloudPlatform={cloudProvider}";

    private LiftiePathProvider underTest;

    private ExperiencePathConfig pathConfig;

    @BeforeEach
    void setUp() {
        pathConfig = new ExperiencePathConfig(Map.of("envCrn", "{environmentCrn}", "cloudProvider", "{cloudProvider}"));
        underTest = new LiftiePathProvider(pathConfig, LIFTIE_API, LIFTIE_PATH_INFIX, LIFTIE_POLICY_ENDPOINT);
    }

    @Test
    void testGetPathToClustersEndpointShouldReturnTheExpectedlyFormattedPath() {
        String expected = getExpectedClusterEndpointPathString();

        String result = underTest.getPathToClustersEndpoint();

        assertEquals(expected, result);
    }

    @Test
    void testGetPathToPolicyEndpointWhenCallHappensTheExpectedOutputComes() {
        String platform = "AWS";
        String expected = LIFTIE_API + LIFTIE_PATH_INFIX + LIFTIE_POLICY_ENDPOINT.replace("{cloudProvider}", platform);

        String result = underTest.getPathToPolicyEndpoint(platform);

        assertEquals(expected, result);
    }

    @Test
    void testGetPathToClusterEndpointShouldReturnTheExpectedlyFormattedPath() {
        String clusterId = "someClusterId";
        String expected = String.format("%s/%s", getExpectedClusterEndpointPathString(), clusterId);

        String result = underTest.getPathToClusterEndpoint(clusterId);

        assertEquals(expected, result);
    }

    private String getExpectedClusterEndpointPathString() {
        return String.format(CLUSTER_ENDPOINT_FORMAT_TEMPLATE, LIFTIE_API);
    }

}

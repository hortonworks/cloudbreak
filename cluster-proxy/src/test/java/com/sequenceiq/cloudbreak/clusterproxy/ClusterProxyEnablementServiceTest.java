package com.sequenceiq.cloudbreak.clusterproxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ClusterProxyEnablementServiceTest {

    @Mock
    private ClusterProxyConfiguration clusterProxyConfiguration;

    @InjectMocks
    private ClusterProxyEnablementService clusterProxyEnablementService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(clusterProxyEnablementService, "clusterProxyDisabledPlatforms", Set.of("MOCK"));
    }

    @ParameterizedTest(name = "{index}: clusterProxyEnablementService.clusterProxyApplicable(get cloudPlatform {0} "
            + "with clusterProxyIntegrationEnabled {1}) = output is clusterProxyApplicable {2}")
    @MethodSource("data")
    void isClusterProxyApplicable(String cloudPlatform, boolean clusterProxyIntegrationEnabled, boolean clusterProxyApplicable) {
        when(clusterProxyConfiguration.isClusterProxyIntegrationEnabled()).thenReturn(clusterProxyIntegrationEnabled);

        assertEquals(clusterProxyApplicable, clusterProxyEnablementService.isClusterProxyApplicable(cloudPlatform));
    }

    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of("MOCK", true, false),
                Arguments.of("MOCK", false, false),
                Arguments.of("AWS", true, true),
                Arguments.of("AWS", false, false),
                Arguments.of("AZURE", true, true),
                Arguments.of("AZURE", false, false)
        );
    }

}
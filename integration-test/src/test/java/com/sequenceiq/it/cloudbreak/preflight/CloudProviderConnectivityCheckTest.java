package com.sequenceiq.it.cloudbreak.preflight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

@ExtendWith(MockitoExtension.class)
class CloudProviderConnectivityCheckTest {

    @Mock
    private CloudProviderEndpointResolver endpointResolver;

    @Spy
    private ReachabilityRestTemplateFactory reachabilityRestTemplateFactory = new ReachabilityRestTemplateFactory();

    @InjectMocks
    private CloudProviderConnectivityCheck underTest;

    @Test
    void testCheckSkippedWhenNoHostnamesToCheck() {
        when(endpointResolver.resolveActiveProviderHostnames()).thenReturn(List.of());
        when(endpointResolver.activeCloudPlatform()).thenReturn(CloudPlatform.MOCK);

        PreflightCheckResult result = underTest.check();

        assertThat(result.isSkipped()).isTrue();
    }

    @Test
    void testCheckFailsWhenHostUnreachable() {
        when(endpointResolver.resolveActiveProviderHostnames()).thenReturn(List.of("this-host-does-not-exist.invalid"));

        PreflightCheckResult result = underTest.check();

        assertThat(result.isPassed()).isFalse();
        assertThat(result.isSkipped()).isFalse();
        assertThat(result.getMessage())
                .contains("this-host-does-not-exist.invalid")
                .contains("UnknownHostException");
    }
}

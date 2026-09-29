package com.sequenceiq.it.cloudbreak.preflight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

public class CloudProviderConnectivityCheckTest {

    private CloudProviderEndpointResolver endpointResolver;

    private CloudProviderConnectivityCheck underTest;

    @BeforeMethod
    public void setUp() {
        endpointResolver = mock(CloudProviderEndpointResolver.class);

        underTest = new CloudProviderConnectivityCheck();
        ReflectionTestUtils.setField(underTest, "endpointResolver", endpointResolver);
        ReflectionTestUtils.setField(underTest, "reachabilityRestTemplateFactory", new ReachabilityRestTemplateFactory());
    }

    @Test
    public void testCheckSkippedWhenNoHostnamesToCheck() {
        when(endpointResolver.resolveActiveProviderHostnames()).thenReturn(List.of());
        when(endpointResolver.activeCloudPlatform()).thenReturn(CloudPlatform.MOCK);

        PreflightCheckResult result = underTest.check();

        assertThat(result.isSkipped()).isTrue();
    }

    @Test
    public void testCheckFailsWhenHostUnreachable() {
        when(endpointResolver.resolveActiveProviderHostnames()).thenReturn(List.of("this-host-does-not-exist.invalid"));

        PreflightCheckResult result = underTest.check();

        assertThat(result.isPassed()).isFalse();
        assertThat(result.isSkipped()).isFalse();
        assertThat(result.getMessage())
                .contains("this-host-does-not-exist.invalid")
                .contains("UnknownHostException");
    }
}

package com.sequenceiq.it.cloudbreak.preflight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.config.CcmProperties;

public class DnsResolutionCheckTest {

    private CloudProviderEndpointResolver endpointResolver;

    private CcmProperties ccmProperties;

    private DnsResolutionCheck underTest;

    @BeforeMethod
    public void setUp() {
        endpointResolver = mock(CloudProviderEndpointResolver.class);
        ccmProperties = mock(CcmProperties.class);

        underTest = new DnsResolutionCheck();
        ReflectionTestUtils.setField(underTest, "endpointResolver", endpointResolver);
        ReflectionTestUtils.setField(underTest, "ccmProperties", ccmProperties);
    }

    @Test
    public void testCheckSkippedWhenNoHostnames() {
        when(endpointResolver.resolveActiveProviderHostnames()).thenReturn(List.of());
        when(ccmProperties.getHost()).thenReturn("");

        PreflightCheckResult result = underTest.check();

        assertThat(result.isSkipped()).isTrue();
    }

    @Test
    public void testCheckPassesWhenHostnamesResolve() {
        when(endpointResolver.resolveActiveProviderHostnames()).thenReturn(List.of("localhost"));
        when(ccmProperties.getHost()).thenReturn("");

        PreflightCheckResult result = underTest.check();

        assertThat(result.isPassed()).isTrue();
    }

    @Test
    public void testCheckFailsWhenHostnameDoesNotResolve() {
        when(endpointResolver.resolveActiveProviderHostnames()).thenReturn(List.of());
        when(ccmProperties.getHost()).thenReturn("this-host-does-not-exist.invalid");

        PreflightCheckResult result = underTest.check();

        assertThat(result.isPassed()).isFalse();
        assertThat(result.isSkipped()).isFalse();
    }
}

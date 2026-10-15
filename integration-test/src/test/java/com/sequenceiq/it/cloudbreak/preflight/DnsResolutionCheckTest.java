package com.sequenceiq.it.cloudbreak.preflight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.it.cloudbreak.config.CcmProperties;

@ExtendWith(MockitoExtension.class)
class DnsResolutionCheckTest {

    @Mock
    private CloudProviderEndpointResolver endpointResolver;

    @Mock
    private CcmProperties ccmProperties;

    @InjectMocks
    private DnsResolutionCheck underTest;

    @Test
    void testCheckSkippedWhenNoHostnames() {
        when(endpointResolver.resolveActiveProviderHostnames()).thenReturn(List.of());
        when(ccmProperties.getHost()).thenReturn("");

        PreflightCheckResult result = underTest.check();

        assertThat(result.isSkipped()).isTrue();
    }

    @Test
    void testCheckPassesWhenHostnamesResolve() {
        when(endpointResolver.resolveActiveProviderHostnames()).thenReturn(List.of("localhost"));
        when(ccmProperties.getHost()).thenReturn("");

        PreflightCheckResult result = underTest.check();

        assertThat(result.isPassed()).isTrue();
    }

    @Test
    void testCheckFailsWhenHostnameDoesNotResolve() {
        when(endpointResolver.resolveActiveProviderHostnames()).thenReturn(List.of());
        when(ccmProperties.getHost()).thenReturn("this-host-does-not-exist.invalid");

        PreflightCheckResult result = underTest.check();

        assertThat(result.isPassed()).isFalse();
        assertThat(result.isSkipped()).isFalse();
    }
}

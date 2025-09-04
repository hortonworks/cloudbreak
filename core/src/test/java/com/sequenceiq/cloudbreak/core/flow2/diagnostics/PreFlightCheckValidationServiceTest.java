package com.sequenceiq.cloudbreak.core.flow2.diagnostics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.proxy.model.response.ProxyResponse;

@ExtendWith(MockitoExtension.class)
class PreFlightCheckValidationServiceTest {

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:acct:environment:1234";

    @Mock
    private EnvironmentService environmentService;

    @InjectMocks
    private PreFlightCheckValidationService underTest;

    @Mock
    private DetailedEnvironmentResponse detailedEnvironmentResponse;

    @Test
    @DisplayName("Proxy used but proxy feature not supported -> false")
    void testProxyUsedAndNotSupportedReturnsFalse() {
        when(environmentService.getByCrn(ENV_CRN)).thenReturn(detailedEnvironmentResponse);
        when(detailedEnvironmentResponse.getProxyConfig()).thenReturn(new ProxyResponse());

        boolean result = underTest.preFlightCheckSupported(ENV_CRN, false);

        assertThat(result).isFalse();
        verify(environmentService).getByCrn(ENV_CRN);
    }

    @Test
    @DisplayName("Proxy used and proxy feature supported -> true")
    void testProxyUsedAndSupportedReturnsTrue() {
        when(environmentService.getByCrn(ENV_CRN)).thenReturn(detailedEnvironmentResponse);
        when(detailedEnvironmentResponse.getProxyConfig()).thenReturn(new ProxyResponse());

        boolean result = underTest.preFlightCheckSupported(ENV_CRN, true);

        assertThat(result).isTrue();
        verify(environmentService).getByCrn(ENV_CRN);
    }

    @Test
    @DisplayName("No proxy used and proxy feature not supported -> true (proxy not required)")
    void testNoProxyUsedAndNotSupportedReturnsTrue() {
        when(environmentService.getByCrn(ENV_CRN)).thenReturn(detailedEnvironmentResponse);
        when(detailedEnvironmentResponse.getProxyConfig()).thenReturn(null);

        boolean result = underTest.preFlightCheckSupported(ENV_CRN, false);

        assertThat(result).isTrue();
        verify(environmentService).getByCrn(ENV_CRN);
    }

    @Test
    @DisplayName("No proxy used and proxy feature supported -> true")
    void testNoProxyUsedAndSupportedReturnsTrue() {
        when(environmentService.getByCrn(ENV_CRN)).thenReturn(detailedEnvironmentResponse);
        when(detailedEnvironmentResponse.getProxyConfig()).thenReturn(null);

        boolean result = underTest.preFlightCheckSupported(ENV_CRN, true);

        assertThat(result).isTrue();
        verify(environmentService).getByCrn(ENV_CRN);
    }

    @Test
    @DisplayName("Null environment CRN still handled (no proxy) -> true")
    void testNullEnvironmentCrnReturnsTrueWhenNoProxy() {
        when(environmentService.getByCrn(null)).thenReturn(detailedEnvironmentResponse);
        when(detailedEnvironmentResponse.getProxyConfig()).thenReturn(null);

        boolean result = underTest.preFlightCheckSupported(null, false);

        assertThat(result).isTrue();
        verify(environmentService).getByCrn(null);
    }

    @Test
    @DisplayName("Null environment CRN with proxy present -> false when not supported")
    void testNullEnvironmentCrnWithProxyAndNotSupported() {
        when(environmentService.getByCrn(null)).thenReturn(detailedEnvironmentResponse);
        when(detailedEnvironmentResponse.getProxyConfig()).thenReturn(new ProxyResponse());

        boolean result = underTest.preFlightCheckSupported(null, false);

        assertThat(result).isFalse();
        verify(environmentService).getByCrn(null);
    }

    @Test
    @DisplayName("Null environment CRN with proxy present -> true when supported")
    void testNullEnvironmentCrnWithProxyAndSupported() {
        when(environmentService.getByCrn(null)).thenReturn(detailedEnvironmentResponse);
        when(detailedEnvironmentResponse.getProxyConfig()).thenReturn(new ProxyResponse());

        boolean result = underTest.preFlightCheckSupported(null, true);

        assertThat(result).isTrue();
        verify(environmentService).getByCrn(null);
    }
}

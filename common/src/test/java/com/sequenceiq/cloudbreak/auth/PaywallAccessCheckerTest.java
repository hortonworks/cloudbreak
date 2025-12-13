package com.sequenceiq.cloudbreak.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.client.RestClientFactory;

@ExtendWith(MockitoExtension.class)
class PaywallAccessCheckerTest {

    private static final String PAYWALL_URL = "paywallyUrl";

    private static final String ERROR_MSG = "The Cloudera Manager license is not valid to authenticate to paywall, "
            + "please contact a Cloudera administrator to update it.";

    @Mock
    private RestClientFactory restClientFactory;

    private Client client = mock(Client.class);

    private WebTarget target = mock(WebTarget.class);

    private JsonCMLicense license = mock(JsonCMLicense.class);

    private Invocation.Builder invocationBuilder = mock(Invocation.Builder.class);

    @InjectMocks
    private PaywallAccessChecker underTest;

    @BeforeEach
    public void init() {
        when(restClientFactory.getOrCreateDefault()).thenReturn(client);
        when(client.target(anyString())).thenReturn(target);
        when(license.getPaywallUsername()).thenReturn("userName");
        when(license.getPaywallPassword()).thenReturn("password");
        when(target.request()).thenReturn(invocationBuilder);
    }

    @AfterEach
    public void assertAfter() {
        verify(client).target(PAYWALL_URL);
        verify(target).register(any(HttpAuthenticationFeature.class));
    }

    @Test
    public void testValid() {
        when(invocationBuilder.get()).thenReturn(Response.ok().build());

        underTest.checkPaywallAccess(license, PAYWALL_URL);
    }

    @Test
    public void testResponseNotOk() {
        when(invocationBuilder.get()).thenReturn(Response.serverError().build());
        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.checkPaywallAccess(license, PAYWALL_URL));
        assertEquals(ERROR_MSG, exception.getMessage());
    }

    @Test
    public void testCantReachPaywall() {
        when(invocationBuilder.get()).thenThrow(new ProcessingException("test"));
        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.checkPaywallAccess(license, PAYWALL_URL));
        assertEquals(ERROR_MSG, exception.getMessage());
    }
}

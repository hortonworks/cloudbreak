package com.sequenceiq.cloudbreak.service.mpack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.domain.ManagementPack;
import com.sequenceiq.cloudbreak.service.credential.PaywallCredentialService;

@RunWith(MockitoJUnitRunner.class)
public class ManagementPackCreationValidatorTest {

    private static final String WRONG_CONTENT_TYPE = "application/wrong-type";

    @Mock
    private Client client;

    @Mock
    private PaywallCredentialService paywallCredentialService;

    @InjectMocks
    private ManagementPackCreationValidator underTest;

    @Before
    public void setUp() {
        when(paywallCredentialService.paywallCredentialAvailable()).thenReturn(false);
    }

    @Test
    public void testWithInvalidMediaType() {
        WebTarget webTarget = Mockito.mock(WebTarget.class);
        Builder webTargetRequest = Mockito.mock(Builder.class);
        when(webTarget.request()).thenReturn(webTargetRequest);
        when(client.target(anyString())).thenReturn(webTarget);
        try (Response response = Response.ok().header("Content-Type", WRONG_CONTENT_TYPE).build()) {
            when(webTargetRequest.head()).thenReturn(response);
            ManagementPack managementPack = new ManagementPack();
            managementPack.setMpackUrl("url");
            ValidationResult validationResult = underTest.validate(managementPack);
            assertEquals(1L, validationResult.getErrors().size());
            assertThat(validationResult.getErrors().get(0), CoreMatchers.containsString(WRONG_CONTENT_TYPE));
            assertThat(validationResult.getErrors().get(0), CoreMatchers.containsString("application/x-tar"));
        }
    }

    @Test
    public void testWithValidMediaType() {
        runValidMediaTypeTest("application/x-gzip");
        runValidMediaTypeTest("application/x-tar");
        runValidMediaTypeTest("application/octet-stream");
    }

    private void runValidMediaTypeTest(String subType) {
        WebTarget webTarget = Mockito.mock(WebTarget.class);
        Builder webTargetRequest = Mockito.mock(Builder.class);
        when(webTarget.request()).thenReturn(webTargetRequest);
        when(client.target(anyString())).thenReturn(webTarget);
        try (Response response = Response.ok().header("Content-Type", subType).build()) {
            when(webTargetRequest.head()).thenReturn(response);
            ManagementPack managementPack = new ManagementPack();
            managementPack.setMpackUrl("url");
            ValidationResult validationResult = underTest.validate(managementPack);
            assertFalse(subType, validationResult.hasError());
            verify(webTargetRequest, never()).header(eq("Authorization"), anyString());
        }
    }

    @Test
    public void testPaywallAuthorization() {
        WebTarget webTarget = Mockito.mock(WebTarget.class);
        Builder webTargetRequest = Mockito.mock(Builder.class);
        when(webTarget.request()).thenReturn(webTargetRequest);
        when(client.target(anyString())).thenReturn(webTarget);
        when(paywallCredentialService.paywallCredentialAvailable()).thenReturn(true);
        try (Response response = Response.ok().header("Content-Type", "application/octet-stream").build()) {
            when(webTargetRequest.head()).thenReturn(response);
            ManagementPack managementPack = new ManagementPack();
            managementPack.setMpackUrl("url");
            ValidationResult validationResult = underTest.validate(managementPack);
            verify(webTargetRequest, times(1)).header(eq("Authorization"), startsWith("Basic"));
        }
    }

    @Test
    public void testWithInvalidUrl() {
        WebTarget webTarget = Mockito.mock(WebTarget.class);
        Builder webTargetRequest = Mockito.mock(Builder.class);
        when(webTarget.request()).thenReturn(webTargetRequest);
        when(client.target(anyString())).thenReturn(webTarget);
        try (Response response = Response.status(300).build()) {
            when(webTargetRequest.head()).thenReturn(response);
            ManagementPack managementPack = new ManagementPack();
            managementPack.setMpackUrl("url");
            ValidationResult validationResult = underTest.validate(managementPack);
            assertEquals(1L, validationResult.getErrors().size());
            assertThat(validationResult.getErrors().get(0), CoreMatchers.containsString("The URL is invalid!"));
        }
    }
}

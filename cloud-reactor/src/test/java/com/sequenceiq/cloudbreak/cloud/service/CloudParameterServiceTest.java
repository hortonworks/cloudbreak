package com.sequenceiq.cloudbreak.cloud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jakarta.ws.rs.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import com.sequenceiq.cloudbreak.client.ProviderAuthenticationFailedException;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationException;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformDatabaseCapabilityRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformDatabaseCapabilityResult;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformNoSqlTablesRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformNoSqlTablesResult;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformRegionsRequestV2;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformRegionsResultV2;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDatabaseCapabilities;
import com.sequenceiq.cloudbreak.cloud.model.nosql.CloudNoSqlTable;
import com.sequenceiq.cloudbreak.cloud.model.nosql.CloudNoSqlTables;
import com.sequenceiq.cloudbreak.eventbus.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;

@ExtendWith(MockitoExtension.class)
class CloudParameterServiceTest {

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @InjectMocks
    private CloudParameterService underTest;

    @BeforeEach
    void setUp() {
        when(eventFactory.createEvent(any())).then((Answer<Event>) invocationOnMock -> new Event(invocationOnMock.getArgument(0, CloudPlatformRequest.class)));
    }

    @Test
    void getNoSqlTables() {
        CloudNoSqlTables expected = new CloudNoSqlTables(List.of(new CloudNoSqlTable("a"), new CloudNoSqlTable("b")));
        GetPlatformNoSqlTablesResult response = new GetPlatformNoSqlTablesResult(1L, expected);
        doAnswer(invocation -> {
            Event<GetPlatformNoSqlTablesRequest> ev = invocation.getArgument(1);
            ev.getData().getResult().onNext(response);
            return null;
        }).when(eventBus).notify(anyString(), any(Event.class));
        CloudNoSqlTables noSqlTables = underTest.getNoSqlTables(
                new ExtendedCloudCredential(new CloudCredential("id", "name", "acc"),
                        "aws", "desc", "account", new ArrayList<>()), "region", "aws", null);
        assertEquals(expected, noSqlTables);
    }

    @Test
    void getDatabaseCapabilities() {
        PlatformDatabaseCapabilities expected = new PlatformDatabaseCapabilities(new HashMap<>(), null, new HashMap<>(), null);
        GetPlatformDatabaseCapabilityResult response = new GetPlatformDatabaseCapabilityResult(1L, expected);
        doAnswer(invocation -> {
            Event<GetPlatformDatabaseCapabilityRequest> ev = invocation.getArgument(1);
            ev.getData().getResult().onNext(response);
            return null;
        }).when(eventBus).notify(anyString(), any(Event.class));
        PlatformDatabaseCapabilities platformDatabaseCapabilities = underTest.getDatabaseCapabilities(
                new ExtendedCloudCredential(new CloudCredential("id", "name", "acc"),
                        "aws", "desc", "account", new ArrayList<>()), "region", "aws", null);
        assertEquals(expected, platformDatabaseCapabilities);
    }

    @Test
    void getDatabaseCapabilitiesFailsWithAuthorizationError() {
        doAnswer(invocation -> {
            Event<GetPlatformDatabaseCapabilityRequest> ev = invocation.getArgument(1);
            ev.getData().getResult().onNext(new GetPlatformDatabaseCapabilityResult("Unauthorized", new ProviderAuthenticationFailedException("error"), 1L));
            return null;
        }).when(eventBus).notify(anyString(), any(Event.class));
        assertThrows(BadRequestException.class, () -> underTest.getDatabaseCapabilities(
                new ExtendedCloudCredential(new CloudCredential("id", "name", "acc"),
                        "aws", "desc", "account", new ArrayList<>()), "region", "aws", null));
    }

    @Test
    void getDatabaseCapabilitiesFailsWithTooManyRequestError() {
        doAnswer(invocation -> {
            Event<GetPlatformDatabaseCapabilityRequest> ev = invocation.getArgument(1);
            ev.getData().getResult().onNext(new GetPlatformDatabaseCapabilityResult("Too many requests", new RuntimeException("error"), 1L));
            return null;
        }).when(eventBus).notify(anyString(), any(Event.class));
        assertThrows(GetCloudParameterException.class, () -> underTest.getDatabaseCapabilities(
                new ExtendedCloudCredential(new CloudCredential("id", "name", "acc"),
                        "aws", "desc", "account", new ArrayList<>()), "region", "aws", null));
    }

    @Test
    void getDatabaseCapabilitiesFailsWithProviderAuthenticationFailedException() {
        doAnswer(invocation -> {
            Event<GetPlatformDatabaseCapabilityRequest> ev = invocation.getArgument(1);
            ev.getData().getResult().onNext(new GetPlatformDatabaseCapabilityResult("reason", new ProviderAuthenticationFailedException("error"), 1L));
            return null;
        }).when(eventBus).notify(anyString(), any(Event.class));
        assertThrows(BadRequestException.class, () -> underTest.getDatabaseCapabilities(
                new ExtendedCloudCredential(new CloudCredential("id", "name", "acc"),
                        "aws", "desc", "account", new ArrayList<>()), "region", "aws", null));
    }

    @Test
    void getDatabaseCapabilitiesFailsWithAwsAuthException() {
        doAnswer(invocation -> {
            Event<GetPlatformDatabaseCapabilityRequest> ev = invocation.getArgument(1);
            ev.getData().getResult().onNext(new GetPlatformDatabaseCapabilityResult("reason", new RuntimeException("user is not authorized to perform X"), 1L));
            return null;
        }).when(eventBus).notify(anyString(), any(Event.class));
        assertThrows(BadRequestException.class, () -> underTest.getDatabaseCapabilities(
                new ExtendedCloudCredential(new CloudCredential("id", "name", "acc"),
                        "aws", "desc", "account", new ArrayList<>()), "region", "aws", null));
    }

    @Test
    void getRegionsV2FailsWithSanitizedMessageWhenHandlerReturnsInternalError() {
        String statusReason = "Failed to get regions from the cloud provider.";
        NullPointerException internalError = new NullPointerException(
                "Cannot invoke \"java.util.Map.get(Object)\" because the return value of "
                        + "\"com.sequenceiq.cloudbreak.cloud.model.CloudCredential.getParameter(String, java.lang.Class)\" is null");
        doAnswer(invocation -> {
            Event<GetPlatformRegionsRequestV2> ev = invocation.getArgument(1);
            ev.getData().getResult().onNext(new GetPlatformRegionsResultV2(statusReason, internalError, 1L));
            return null;
        }).when(eventBus).notify(anyString(), any(Event.class));

        GetCloudParameterException exception = assertThrows(GetCloudParameterException.class, () -> underTest.getRegionsV2(
                new ExtendedCloudCredential(new CloudCredential("id", "name", "acc"),
                        "aws", "desc", "account", new ArrayList<>()), "region", "native", null, false));

        assertThat(exception.getMessage()).isEqualTo("Failed to get regions for the cloud provider: Failed to get regions from the cloud provider.");
        assertThat(exception.getMessage()).doesNotContain("Cannot invoke");
        assertThat(exception.getMessage()).doesNotContain("Causes:");
    }

    @Test
    void getRegionsV2IncludesProviderCauseForCloudConnectorFailures() {
        String providerMessage = "The security token included in the request is invalid.";
        doAnswer(invocation -> {
            Event<GetPlatformRegionsRequestV2> ev = invocation.getArgument(1);
            ev.getData().getResult().onNext(new GetPlatformRegionsResultV2(
                    "Failed to get regions from the cloud provider.", new CloudConnectorException(providerMessage), 1L));
            return null;
        }).when(eventBus).notify(anyString(), any(Event.class));

        GetCloudParameterException exception = assertThrows(GetCloudParameterException.class, () -> underTest.getRegionsV2(
                new ExtendedCloudCredential(new CloudCredential("id", "name", "acc"),
                        "aws", "desc", "account", new ArrayList<>()), "region", "native", null, false));

        assertThat(exception.getMessage())
                .isEqualTo("Failed to get regions for the cloud provider: Failed to get regions from the cloud provider. "
                        + "Causes: [" + providerMessage + "].");
    }

    @Test
    void getRegionsV2IncludesCredentialVerificationCause() {
        String credentialMessage = "Missing access or secret key from the credential.";
        doAnswer(invocation -> {
            Event<GetPlatformRegionsRequestV2> ev = invocation.getArgument(1);
            ev.getData().getResult().onNext(new GetPlatformRegionsResultV2(
                    "Failed to get regions from the cloud provider.", new CredentialVerificationException(credentialMessage), 1L));
            return null;
        }).when(eventBus).notify(anyString(), any(Event.class));

        GetCloudParameterException exception = assertThrows(GetCloudParameterException.class, () -> underTest.getRegionsV2(
                new ExtendedCloudCredential(new CloudCredential("id", "name", "acc"),
                        "aws", "desc", "account", new ArrayList<>()), "region", "native", null, false));

        assertThat(exception.getMessage())
                .isEqualTo("Failed to get regions for the cloud provider: Failed to get regions from the cloud provider. "
                        + "Causes: [" + credentialMessage + "].");
    }

    @Test
    void getRegionsV2IncludesGcpProviderCauseForGoogleJsonResponseException() {
        String gcpMessage = "Request had insufficient authentication scopes.";
        GoogleJsonResponseException gcpError = createGoogleJsonResponseException(gcpMessage);
        doAnswer(invocation -> {
            Event<GetPlatformRegionsRequestV2> ev = invocation.getArgument(1);
            ev.getData().getResult().onNext(new GetPlatformRegionsResultV2(
                    "Failed to get regions from the cloud provider.", gcpError, 1L));
            return null;
        }).when(eventBus).notify(anyString(), any(Event.class));

        GetCloudParameterException exception = assertThrows(GetCloudParameterException.class, () -> underTest.getRegionsV2(
                new ExtendedCloudCredential(new CloudCredential("id", "name", "acc"),
                        "gcp", "desc", "account", new ArrayList<>()), "region", "variant", null, false));

        assertThat(exception.getMessage())
                .isEqualTo("Failed to get regions for the cloud provider: Failed to get regions from the cloud provider. "
                        + "Causes: [" + gcpMessage + "].");
    }

    private static GoogleJsonResponseException createGoogleJsonResponseException(String message) {
        GoogleJsonError error = new GoogleJsonError();
        error.setMessage(message);
        error.setCode(403);
        return new GoogleJsonResponseException(new HttpResponseException.Builder(403, "Forbidden", new HttpHeaders()), error);
    }

    @Test
    void getDatabaseCapabilitiesFailsWithAzureAuthException() {
        doAnswer(invocation -> {
            Event<GetPlatformDatabaseCapabilityRequest> ev = invocation.getArgument(1);
            ev.getData().getResult().onNext(new GetPlatformDatabaseCapabilityResult("reason",
                    new RuntimeException("does not have authorization to perform action X"), 1L));
            return null;
        }).when(eventBus).notify(anyString(), any(Event.class));
        assertThrows(BadRequestException.class, () -> underTest.getDatabaseCapabilities(
                new ExtendedCloudCredential(new CloudCredential("id", "name", "acc"),
                        "azure", "desc", "account", new ArrayList<>()), "region", "azure", null));
    }
}

package com.sequenceiq.environment.environment.experience.service;

import com.sequenceiq.environment.environment.experience.response.ExperienceCallResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;
import java.net.URI;
import java.util.Set;

import static javax.ws.rs.client.Invocation.Builder;
import static javax.ws.rs.core.Response.Status.Family.CLIENT_ERROR;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExperienceConnectorServiceTest {

    private static final String COMPONENT_TO_REPLACE_IN_PATH = "{crn}";

    private static final String EXPERIENCE_BASE_PATH = "https://127.0.0.1:9003/dwx/api/v3/cp-internal/environment/someenvcrn";

    private static final String ENVIRONMENT_CRN = "someEnvironmentCrn";

    @Mock
    private Client mockClient;

    @Mock
    private WebTarget mockWebTarget;

    @Mock
    private Builder mockInvocationBuilder;

    @Mock
    private Response mockResponse;

    @Mock
    private StatusType mockStatusType;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mockInvocationBuilder.accept(anyString())).thenReturn(mockInvocationBuilder);
        when(mockInvocationBuilder.header(anyString(), any())).thenReturn(mockInvocationBuilder);
        when(mockResponse.getStatusInfo()).thenReturn(mockStatusType);
        when(mockStatusType.getFamily()).thenReturn(SUCCESSFUL);
    }

    @Test
    void testExperienceConnectorServiceCouldNotBeInstantiatedWithNullComponentToReplaceInPathValue() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ExperienceConnectorService(null, mockClient));
    }

    @Test
    void testExperienceConnectorServiceCouldNotBeInstantiatedWithEmptyComponentToReplaceInPathValue() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ExperienceConnectorService("", mockClient));
    }

    @Test
    void testWhenGetWorkspaceNamesConnectedToEnvCalledWithNullExperienceBasePathValueThenIllegalArgumentExceptionComes() {
        ExperienceConnectorService underTest = new ExperienceConnectorService(COMPONENT_TO_REPLACE_IN_PATH, mockClient);
        Assertions.assertThrows(IllegalArgumentException.class, () -> underTest.getWorkspaceNamesConnectedToEnv(null, ENVIRONMENT_CRN));
    }

    @Test
    void testWhenGetWorkspaceNamesConnectedToEnvCalledThenWebTargetCreatedWithGivenExperienceBasePath() {
        when(mockClient.target(EXPERIENCE_BASE_PATH)).thenReturn(mockWebTarget);
        when(mockWebTarget.request()).thenReturn(mockInvocationBuilder);
        when(mockInvocationBuilder.get()).thenReturn(mockResponse);

        ExperienceConnectorService underTest = new ExperienceConnectorService(COMPONENT_TO_REPLACE_IN_PATH, mockClient);
        underTest.getWorkspaceNamesConnectedToEnv(EXPERIENCE_BASE_PATH, ENVIRONMENT_CRN);

        verify(mockClient, times(1)).target(anyString());
        verify(mockClient, times(1)).target(EXPERIENCE_BASE_PATH);
    }

    @Test
    void testWhenUnableToReadResponseDueToIllegalStateExceptionThenEmptySetReturns() {
        when(mockClient.target(EXPERIENCE_BASE_PATH)).thenReturn(mockWebTarget);
        when(mockWebTarget.request()).thenReturn(mockInvocationBuilder);
        when(mockInvocationBuilder.get()).thenReturn(mockResponse);
        when(mockWebTarget.getUri()).thenReturn(URI.create(EXPERIENCE_BASE_PATH));
        doThrow(new IllegalStateException()).when(mockResponse).readEntity(ExperienceCallResponse.class);

        ExperienceConnectorService underTest = new ExperienceConnectorService(COMPONENT_TO_REPLACE_IN_PATH, mockClient);
        Set<String> result = underTest.getWorkspaceNamesConnectedToEnv(EXPERIENCE_BASE_PATH, ENVIRONMENT_CRN);

        assertNotNull(result);
        assertEquals(0L, result.size());
    }

    @Test
    void testWhenUnableToReadResponseDueToProcessingExceptionThenEmptySetReturns() {
        when(mockWebTarget.getUri()).thenReturn(URI.create(EXPERIENCE_BASE_PATH));
        when(mockClient.target(EXPERIENCE_BASE_PATH)).thenReturn(mockWebTarget);
        when(mockWebTarget.request()).thenReturn(mockInvocationBuilder);
        when(mockInvocationBuilder.get()).thenReturn(mockResponse);
        doThrow(new ProcessingException("")).when(mockResponse).readEntity(ExperienceCallResponse.class);

        ExperienceConnectorService underTest = new ExperienceConnectorService(COMPONENT_TO_REPLACE_IN_PATH, mockClient);
        Set<String> result = underTest.getWorkspaceNamesConnectedToEnv(EXPERIENCE_BASE_PATH, ENVIRONMENT_CRN);

        assertNotNull(result);
        assertEquals(0L, result.size());
    }

    @Test
    void testWhenRequestWasNotSuccessfulThenEmptySetReturns() {
        when(mockWebTarget.getUri()).thenReturn(URI.create(EXPERIENCE_BASE_PATH));
        when(mockClient.target(EXPERIENCE_BASE_PATH)).thenReturn(mockWebTarget);
        when(mockWebTarget.request()).thenReturn(mockInvocationBuilder);
        when(mockInvocationBuilder.get()).thenReturn(mockResponse);
        when(mockStatusType.getFamily()).thenReturn(CLIENT_ERROR);

        ExperienceConnectorService underTest = new ExperienceConnectorService(COMPONENT_TO_REPLACE_IN_PATH, mockClient);
        Set<String> result = underTest.getWorkspaceNamesConnectedToEnv(EXPERIENCE_BASE_PATH, ENVIRONMENT_CRN);

        assertNotNull(result);
        assertEquals(0L, result.size());
    }

    @Test
    void testGetWorkspaceNamesConnectedToEnvWhenGettingResponseThrowsSomeRuntimeExceptionThenEmptySetReturns() {
        when(mockClient.target(EXPERIENCE_BASE_PATH)).thenReturn(mockWebTarget);
        when(mockWebTarget.request()).thenReturn(mockInvocationBuilder);
        doThrow(new RuntimeException()).when(mockInvocationBuilder).get();

        ExperienceConnectorService underTest = new ExperienceConnectorService(COMPONENT_TO_REPLACE_IN_PATH, mockClient);
        Set<String> result = underTest.getWorkspaceNamesConnectedToEnv(EXPERIENCE_BASE_PATH, ENVIRONMENT_CRN);

        assertNotNull(result);
        assertEquals(0L, result.size());
    }

}
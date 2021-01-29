package com.sequenceiq.environment.experience.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.environment.experience.InvocationBuilderProvider;
import com.sequenceiq.environment.experience.RetryableWebTarget;
import com.sequenceiq.environment.experience.common.responses.CpInternalEnvironmentResponse;
import com.sequenceiq.environment.experience.common.responses.DeleteCommonExperienceWorkspaceResponse;

class CommonExperienceConnectorServiceTest {

    private static final String COMMON_XP_RESPONSE_RESOLVE_ERROR_MSG = "Unable to resolve the experience's response!";

    private static final String TEST_XP_BASE_PATH = "someExperienceBasePath";

    private static final String TEST_ENV_CRN = "someEnvironmentCrn";

    private static final URI TEST_URI = URI.create("somePath");

    private static final int ONCE = 1;

    @Mock
    private CommonExperienceWebTargetProvider mockCommonExperienceWebTargetProvider;

    @Mock
    private CommonExperienceResponseReader mockCommonExperienceResponseReader;

    @Mock
    private InvocationBuilderProvider mockInvocationBuilderProvider;

    @Mock
    private RetryableWebTarget mockRetryableWebTarget;

    @Mock
    private Invocation.Builder mockInvocationBuilder;

    @Mock
    private WebTarget mockWebTarget;

    @Mock
    private Response mockResponse;

    private CommonExperienceConnectorService underTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        underTest = new CommonExperienceConnectorService(mockRetryableWebTarget, mockCommonExperienceResponseReader, mockCommonExperienceWebTargetProvider,
                mockInvocationBuilderProvider);

        when(mockWebTarget.getUri()).thenReturn(TEST_URI);
        when(mockInvocationBuilderProvider.createInvocationBuilder(mockWebTarget)).thenReturn(mockInvocationBuilder);
    }

    @Test
    void testGetWorkspaceNamesConnectedToEnvShouldObtainWebTargetFromCreator() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetBasedOnInputs(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);

        underTest.getWorkspaceNamesConnectedToEnv(TEST_XP_BASE_PATH, TEST_ENV_CRN);

        verify(mockCommonExperienceWebTargetProvider, times(ONCE)).createWebTargetBasedOnInputs(any(), any());
        verify(mockCommonExperienceWebTargetProvider, times(ONCE)).createWebTargetBasedOnInputs(TEST_XP_BASE_PATH, TEST_ENV_CRN);
    }

    @Test
    void testGetWorkspaceNamesConnectedToEnvShouldObtainInvocationBuilderFromCreator() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetBasedOnInputs(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilder(mockWebTarget)).thenReturn(mockInvocationBuilder);

        underTest.getWorkspaceNamesConnectedToEnv(TEST_XP_BASE_PATH, TEST_ENV_CRN);

        verify(mockInvocationBuilderProvider, times(ONCE)).createInvocationBuilder(any());
        verify(mockInvocationBuilderProvider, times(ONCE)).createInvocationBuilder(mockWebTarget);
    }

    @Test
    void testGetWorkspaceNamesConnectedToEnvShouldExecuteItsCallThroughRetryableWebTarget() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetBasedOnInputs(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilder(mockWebTarget)).thenReturn(mockInvocationBuilder);

        underTest.getWorkspaceNamesConnectedToEnv(TEST_XP_BASE_PATH, TEST_ENV_CRN);

        verify(mockRetryableWebTarget, times(ONCE)).get(any());
        verify(mockRetryableWebTarget, times(ONCE)).get(mockInvocationBuilder);
    }

    @Test
    void testGetWorkspaceNamesConnectedToEnvWhenCallResultIsEmptyThenEmptyOptionalShouldReturn() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetBasedOnInputs(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilder(mockWebTarget)).thenReturn(mockInvocationBuilder);
        when(mockRetryableWebTarget.get(mockInvocationBuilder)).thenReturn(null);

        Set<String> result = underTest.getWorkspaceNamesConnectedToEnv(TEST_XP_BASE_PATH, TEST_ENV_CRN);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetWorkspaceNamesConnectedToEnvWhenCallResultIsNotEmptyThenResponseReaderShouldBeInvokeToResolveContent() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetBasedOnInputs(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilder(mockWebTarget)).thenReturn(mockInvocationBuilder);
        when(mockRetryableWebTarget.get(mockInvocationBuilder)).thenReturn(mockResponse);

        underTest.getWorkspaceNamesConnectedToEnv(TEST_XP_BASE_PATH, TEST_ENV_CRN);

        verify(mockCommonExperienceResponseReader, times(ONCE)).read(any(), any(), any());
        verify(mockCommonExperienceResponseReader, times(ONCE)).read(TEST_URI.toString(), mockResponse, CpInternalEnvironmentResponse.class);
    }

    @Test
    void testGetWorkspaceNamesConnectedToEnvWhenResponseReaderUnableToResolveResponseThenEmptyOptionalReturnsAndResponseResultIterationDoesntHappen() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetBasedOnInputs(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilder(mockWebTarget)).thenReturn(mockInvocationBuilder);
        when(mockRetryableWebTarget.get(mockInvocationBuilder)).thenReturn(mockResponse);
        when(mockCommonExperienceResponseReader.read(TEST_URI.toString(), mockResponse, CpInternalEnvironmentResponse.class)).thenReturn(Optional.empty());

        Set<String> result = underTest.getWorkspaceNamesConnectedToEnv(TEST_XP_BASE_PATH, TEST_ENV_CRN);

        assertTrue(CollectionUtils.isEmpty(result));
    }

    @Test
    void testGetWorkspaceNamesConnectedToEnvWhenResponseReaderIsAbleToResolveResponseThenResponseResultIterationHappens() {
        CpInternalEnvironmentResponse mockCpInternalEnvironmentResponse = mock(CpInternalEnvironmentResponse.class);
        when(mockCommonExperienceWebTargetProvider.createWebTargetBasedOnInputs(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilder(mockWebTarget)).thenReturn(mockInvocationBuilder);
        when(mockRetryableWebTarget.get(mockInvocationBuilder)).thenReturn(mockResponse);
        when(mockCommonExperienceResponseReader.read(TEST_URI.toString(), mockResponse, CpInternalEnvironmentResponse.class))
                .thenReturn(Optional.of(mockCpInternalEnvironmentResponse));

        underTest.getWorkspaceNamesConnectedToEnv(TEST_XP_BASE_PATH, TEST_ENV_CRN);

        verify(mockCpInternalEnvironmentResponse, times(ONCE)).getResults();
    }

    @Test
    void testGetWorkspaceNamesConnectedToEnvWhenCallExecutionThrowsExceptionThenEmptyResultShouldReturn() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetBasedOnInputs(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilder(mockWebTarget)).thenReturn(mockInvocationBuilder);
        when(mockRetryableWebTarget.get(mockInvocationBuilder)).thenThrow(new RuntimeException());

        Set<String> result = underTest.getWorkspaceNamesConnectedToEnv(TEST_XP_BASE_PATH, TEST_ENV_CRN);

        assertTrue(CollectionUtils.isEmpty(result));

        verify(mockCommonExperienceResponseReader, never()).read(any(), any(), any());
    }

    @Test
    void testDeleteWorkspaceForEnvironmentShouldObtainWebTargetFromCreator() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetBasedOnInputs(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);
        when(mockRetryableWebTarget.delete(any())).thenReturn(mockResponse);
        when(mockCommonExperienceResponseReader.read(any(), any(), any())).thenReturn(Optional.of(mock(DeleteCommonExperienceWorkspaceResponse.class)));

        underTest.deleteWorkspaceForEnvironment(TEST_XP_BASE_PATH, TEST_ENV_CRN);

        verify(mockCommonExperienceWebTargetProvider, times(ONCE)).createWebTargetBasedOnInputs(any(), any());
        verify(mockCommonExperienceWebTargetProvider, times(ONCE)).createWebTargetBasedOnInputs(TEST_XP_BASE_PATH, TEST_ENV_CRN);
    }

    @Test
    void testDeleteWorkspaceForEnvironmentShouldObtainInvocationBuilderFromCreator() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetBasedOnInputs(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilder(mockWebTarget)).thenReturn(mockInvocationBuilder);
        when(mockRetryableWebTarget.delete(any())).thenReturn(mockResponse);
        when(mockCommonExperienceResponseReader.read(any(), any(), any())).thenReturn(Optional.of(mock(DeleteCommonExperienceWorkspaceResponse.class)));

        underTest.deleteWorkspaceForEnvironment(TEST_XP_BASE_PATH, TEST_ENV_CRN);

        verify(mockInvocationBuilderProvider, times(ONCE)).createInvocationBuilder(any());
        verify(mockInvocationBuilderProvider, times(ONCE)).createInvocationBuilder(mockWebTarget);
    }

    @Test
    void testDeleteWorkspaceForEnvironmentShouldExecuteItsCallThroughRetryableWebTarget() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetBasedOnInputs(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilder(mockWebTarget)).thenReturn(mockInvocationBuilder);
        when(mockRetryableWebTarget.delete(any())).thenReturn(mockResponse);
        when(mockCommonExperienceResponseReader.read(any(), any(), any())).thenReturn(Optional.of(mock(DeleteCommonExperienceWorkspaceResponse.class)));

        underTest.deleteWorkspaceForEnvironment(TEST_XP_BASE_PATH, TEST_ENV_CRN);

        verify(mockRetryableWebTarget, times(ONCE)).delete(any());
        verify(mockRetryableWebTarget, times(ONCE)).delete(mockInvocationBuilder);
    }

    @Test
    void testDeleteWorkspaceForEnvironmentWhenCallResultIsEmptyThenIllegalStateExceptionShouldCome() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetBasedOnInputs(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilder(mockWebTarget)).thenReturn(mockInvocationBuilder);
        when(mockRetryableWebTarget.delete(mockInvocationBuilder)).thenReturn(null);

        IllegalStateException expectedException = assertThrows(IllegalStateException.class,
                () -> underTest.deleteWorkspaceForEnvironment(TEST_XP_BASE_PATH, TEST_ENV_CRN));

        assertNotNull(expectedException);
        assertEquals(COMMON_XP_RESPONSE_RESOLVE_ERROR_MSG, expectedException.getMessage());
    }

    @Test
    void testDeleteWorkspaceForEnvironmentWhenCallResultIsNotEmptyThenResponseReaderShouldBeInvokeToResolveContent() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetBasedOnInputs(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilder(mockWebTarget)).thenReturn(mockInvocationBuilder);
        when(mockRetryableWebTarget.delete(any())).thenReturn(mockResponse);
        when(mockCommonExperienceResponseReader.read(any(), any(), any())).thenReturn(Optional.of(mock(DeleteCommonExperienceWorkspaceResponse.class)));

        underTest.deleteWorkspaceForEnvironment(TEST_XP_BASE_PATH, TEST_ENV_CRN);

        verify(mockCommonExperienceResponseReader, times(ONCE)).read(any(), any(), any());
        verify(mockCommonExperienceResponseReader, times(ONCE)).read(TEST_URI.toString(), mockResponse, DeleteCommonExperienceWorkspaceResponse.class);
    }

    @Test
    void testDeleteWorkspaceForEnvironmentWhenResponseReaderUnableToResolveResponseThenIllegalStateExceptionShouldCome() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetBasedOnInputs(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilder(mockWebTarget)).thenReturn(mockInvocationBuilder);
        when(mockRetryableWebTarget.delete(mockInvocationBuilder)).thenReturn(mockResponse);
        when(mockCommonExperienceResponseReader.read(TEST_URI.toString(), mockResponse, CpInternalEnvironmentResponse.class)).thenReturn(Optional.empty());

        IllegalStateException expectedException = assertThrows(IllegalStateException.class,
                () -> underTest.deleteWorkspaceForEnvironment(TEST_XP_BASE_PATH, TEST_ENV_CRN));

        assertNotNull(expectedException);
        assertEquals(COMMON_XP_RESPONSE_RESOLVE_ERROR_MSG, expectedException.getMessage());
    }

    @Test
    void testDeleteWorkspaceForEnvironmentWhenResponseReaderIsAbleToResolveResponseThenResponseResultIterationHappens() {
        DeleteCommonExperienceWorkspaceResponse mockCpInternalEnvironmentResponse = mock(DeleteCommonExperienceWorkspaceResponse.class);
        when(mockCommonExperienceWebTargetProvider.createWebTargetBasedOnInputs(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilder(mockWebTarget)).thenReturn(mockInvocationBuilder);
        when(mockRetryableWebTarget.delete(mockInvocationBuilder)).thenReturn(mockResponse);
        when(mockCommonExperienceResponseReader.read(TEST_URI.toString(), mockResponse, DeleteCommonExperienceWorkspaceResponse.class))
                .thenReturn(Optional.of(mockCpInternalEnvironmentResponse));

        underTest.deleteWorkspaceForEnvironment(TEST_XP_BASE_PATH, TEST_ENV_CRN);
    }

    @Test
    void testDeleteWorkspaceForEnvironmentWhenCallExecutionThrowsExceptionThenIllegalStateExceptionShouldCome() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetBasedOnInputs(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilder(mockWebTarget)).thenReturn(mockInvocationBuilder);
        when(mockRetryableWebTarget.delete(mockInvocationBuilder)).thenThrow(new RuntimeException());

        IllegalStateException expectedException = assertThrows(IllegalStateException.class,
                () -> underTest.deleteWorkspaceForEnvironment(TEST_XP_BASE_PATH, TEST_ENV_CRN));

        assertNotNull(expectedException);
        assertEquals(COMMON_XP_RESPONSE_RESOLVE_ERROR_MSG, expectedException.getMessage());

        verify(mockCommonExperienceResponseReader, never()).read(any(), any(), any());
    }

}
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
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.sequenceiq.environment.exception.ExperienceOperationFailedException;
import com.sequenceiq.environment.experience.InvocationBuilderProvider;
import com.sequenceiq.environment.experience.RetryableWebTarget;
import com.sequenceiq.environment.experience.common.responses.CpInternalCluster;
import com.sequenceiq.environment.experience.common.responses.CpInternalEnvironmentResponse;
import com.sequenceiq.environment.experience.common.responses.DeleteCommonExperienceWorkspaceResponse;

@ExtendWith(MockitoExtension.class)
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
        underTest = new CommonExperienceConnectorService(mockRetryableWebTarget, mockCommonExperienceResponseReader, mockCommonExperienceWebTargetProvider,
                mockInvocationBuilderProvider);

        when(mockWebTarget.getUri()).thenReturn(TEST_URI);
        when(mockInvocationBuilderProvider.createInvocationBuilder(mockWebTarget)).thenReturn(mockInvocationBuilder);
    }

    @Test
    void testGetWorkspaceNamesConnectedToEnvShouldObtainWebTargetFromCreator() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetBasedOnInputs(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);

        when(mockRetryableWebTarget.get(mockInvocationBuilder)).thenReturn(mockResponse);
        when(mockCommonExperienceResponseReader.read(any(), any(), any())).thenReturn(Optional.of(createCpInternalEnvironmentResponse()));

        underTest.getExperienceClustersConnectedToEnv(TEST_XP_BASE_PATH, TEST_ENV_CRN);

        verify(mockCommonExperienceWebTargetProvider, times(ONCE)).createWebTargetBasedOnInputs(any(), any());
        verify(mockCommonExperienceWebTargetProvider, times(ONCE)).createWebTargetBasedOnInputs(TEST_XP_BASE_PATH, TEST_ENV_CRN);
    }

    @Test
    void testGetWorkspaceNamesConnectedToEnvShouldReturnTheNameOfClustersFromTheCallResult() {
        CpInternalEnvironmentResponse response = createCpInternalEnvironmentResponse();

        when(mockCommonExperienceWebTargetProvider.createWebTargetBasedOnInputs(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);
        when(mockRetryableWebTarget.get(mockInvocationBuilder)).thenReturn(mockResponse);
        when(mockCommonExperienceResponseReader.read(any(), any(), any())).thenReturn(Optional.of(response));

        Set<CpInternalCluster> result = underTest.getExperienceClustersConnectedToEnv(TEST_XP_BASE_PATH, TEST_ENV_CRN);

        assertEquals(response.getResults().size(), result.size());
        response.getResults().forEach(cluster -> assertTrue(result.contains(cluster)));
    }

    @Test
    void testGetWorkspaceNamesConnectedToEnvShouldObtainInvocationBuilderFromCreator() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetBasedOnInputs(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilder(mockWebTarget)).thenReturn(mockInvocationBuilder);

        when(mockRetryableWebTarget.get(mockInvocationBuilder)).thenReturn(mockResponse);
        when(mockCommonExperienceResponseReader.read(any(), any(), any())).thenReturn(Optional.of(createCpInternalEnvironmentResponse()));

        underTest.getExperienceClustersConnectedToEnv(TEST_XP_BASE_PATH, TEST_ENV_CRN);

        verify(mockInvocationBuilderProvider, times(ONCE)).createInvocationBuilder(any());
        verify(mockInvocationBuilderProvider, times(ONCE)).createInvocationBuilder(mockWebTarget);
    }

    @Test
    void testGetWorkspaceNamesConnectedToEnvShouldExecuteItsCallThroughRetryableWebTarget() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetBasedOnInputs(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilder(mockWebTarget)).thenReturn(mockInvocationBuilder);

        when(mockRetryableWebTarget.get(mockInvocationBuilder)).thenReturn(mockResponse);
        when(mockCommonExperienceResponseReader.read(any(), any(), any())).thenReturn(Optional.of(createCpInternalEnvironmentResponse()));

        underTest.getExperienceClustersConnectedToEnv(TEST_XP_BASE_PATH, TEST_ENV_CRN);

        verify(mockRetryableWebTarget, times(ONCE)).get(any());
        verify(mockRetryableWebTarget, times(ONCE)).get(mockInvocationBuilder);
    }

    @Test
    void testGetWorkspaceNamesConnectedToEnvWhenCallResultIsEmptyThenIllegalStateExceptionShouldBeThrown() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetBasedOnInputs(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilder(mockWebTarget)).thenReturn(mockInvocationBuilder);
        when(mockRetryableWebTarget.get(mockInvocationBuilder)).thenReturn(null);

        ExperienceOperationFailedException expectedException = assertThrows(ExperienceOperationFailedException.class,
                () -> underTest.getExperienceClustersConnectedToEnv(TEST_XP_BASE_PATH, TEST_ENV_CRN));

        assertEquals(COMMON_XP_RESPONSE_RESOLVE_ERROR_MSG, expectedException.getMessage());
    }

    @Test
    void testGetWorkspaceNamesConnectedToEnvWhenCallResultIsNotEmptyThenResponseReaderShouldBeInvokeToResolveContent() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetBasedOnInputs(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilder(mockWebTarget)).thenReturn(mockInvocationBuilder);
        when(mockRetryableWebTarget.get(mockInvocationBuilder)).thenReturn(mockResponse);
        when(mockCommonExperienceResponseReader.read(any(), any(), any())).thenReturn(Optional.of(createCpInternalEnvironmentResponse()));

        underTest.getExperienceClustersConnectedToEnv(TEST_XP_BASE_PATH, TEST_ENV_CRN);

        verify(mockCommonExperienceResponseReader, times(ONCE)).read(any(), any(), any());
        verify(mockCommonExperienceResponseReader, times(ONCE)).read(TEST_URI.toString(), mockResponse, CpInternalEnvironmentResponse.class);
    }

    @Test
    void testGetWorkspaceNamesConnectedToEnvWhenResponseReaderUnableToResolveResponseThenEmptyOptionalReturnsAndResponseResultIterationDoesntHappen() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetBasedOnInputs(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilder(mockWebTarget)).thenReturn(mockInvocationBuilder);
        when(mockRetryableWebTarget.get(mockInvocationBuilder)).thenReturn(mockResponse);
        when(mockCommonExperienceResponseReader.read(TEST_URI.toString(), mockResponse, CpInternalEnvironmentResponse.class)).thenReturn(Optional.empty());

        ExperienceOperationFailedException expectedException = assertThrows(ExperienceOperationFailedException.class,
                () -> underTest.getExperienceClustersConnectedToEnv(TEST_XP_BASE_PATH, TEST_ENV_CRN));

        assertEquals(COMMON_XP_RESPONSE_RESOLVE_ERROR_MSG, expectedException.getMessage());
    }

    @Test
    void testGetWorkspaceNamesConnectedToEnvWhenResponseReaderIsAbleToResolveResponseThenResponseResultIterationHappens() {
        CpInternalEnvironmentResponse mockCpInternalEnvironmentResponse = mock(CpInternalEnvironmentResponse.class);
        when(mockCommonExperienceWebTargetProvider.createWebTargetBasedOnInputs(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilder(mockWebTarget)).thenReturn(mockInvocationBuilder);
        when(mockRetryableWebTarget.get(mockInvocationBuilder)).thenReturn(mockResponse);
        when(mockCommonExperienceResponseReader.read(TEST_URI.toString(), mockResponse, CpInternalEnvironmentResponse.class))
                .thenReturn(Optional.of(mockCpInternalEnvironmentResponse));

        underTest.getExperienceClustersConnectedToEnv(TEST_XP_BASE_PATH, TEST_ENV_CRN);

        verify(mockCpInternalEnvironmentResponse, times(ONCE)).getResults();
    }

    @Test
    void testGetWorkspaceNamesConnectedToEnvWhenCallExecutionThrowsExceptionThenIllegalStateExceptionShouldBeThrown() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetBasedOnInputs(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilder(mockWebTarget)).thenReturn(mockInvocationBuilder);
        when(mockRetryableWebTarget.get(mockInvocationBuilder)).thenThrow(new RuntimeException());

        ExperienceOperationFailedException expectedException = assertThrows(ExperienceOperationFailedException.class,
                () -> underTest.getExperienceClustersConnectedToEnv(TEST_XP_BASE_PATH, TEST_ENV_CRN));

        assertEquals(COMMON_XP_RESPONSE_RESOLVE_ERROR_MSG, expectedException.getMessage());
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

        ExperienceOperationFailedException expectedException = assertThrows(ExperienceOperationFailedException.class,
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
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testDeleteWorkspaceForEnvironmentWhenResponseReaderUnableToResolveResponseThenIllegalStateExceptionShouldCome() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetBasedOnInputs(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilder(mockWebTarget)).thenReturn(mockInvocationBuilder);
        when(mockRetryableWebTarget.delete(mockInvocationBuilder)).thenReturn(mockResponse);
        when(mockCommonExperienceResponseReader.read(TEST_URI.toString(), mockResponse, CpInternalEnvironmentResponse.class)).thenReturn(Optional.empty());

        ExperienceOperationFailedException expectedException = assertThrows(ExperienceOperationFailedException.class,
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

        ExperienceOperationFailedException expectedException = assertThrows(ExperienceOperationFailedException.class,
                () -> underTest.deleteWorkspaceForEnvironment(TEST_XP_BASE_PATH, TEST_ENV_CRN));

        assertNotNull(expectedException);
        assertEquals(COMMON_XP_RESPONSE_RESOLVE_ERROR_MSG, expectedException.getMessage());

        verify(mockCommonExperienceResponseReader, never()).read(any(), any(), any());
    }

    private CpInternalEnvironmentResponse createCpInternalEnvironmentResponse() {
        CpInternalEnvironmentResponse response = new CpInternalEnvironmentResponse();
        response.setResults(createCpInternalClusters(5));
        return response;
    }

    private Set<CpInternalCluster> createCpInternalClusters(int quantity) {
        Set<CpInternalCluster> clusters = new LinkedHashSet<>(quantity);
        for (int i = 0; i < quantity; i++) {
            CpInternalCluster c = new CpInternalCluster();
            c.setName("cluster_" + i);
            c.setStatus("AVAILABLE");
            c.setCrn("cluster_" + i + "_crn");
            clusters.add(c);
        }
        return clusters;
    }

}

package com.sequenceiq.environment.experience.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
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
import javax.ws.rs.core.Response.Status.Family;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
import com.sequenceiq.environment.experience.policy.response.ExperiencePolicyResponse;

@ExtendWith(MockitoExtension.class)
class CommonExperienceConnectorServiceTest {

    private static final String COMMON_XP_RESPONSE_RESOLVE_ERROR_MSG = "Unable to resolve the experience's response!";

    private static final String NOT_SUCCESSFUL_EXPERIENCE_DELETE_MESSAGE = "We are unable to delete connected experience(s), please retry or check " +
            "your experiences manually.";

    private static final String TEST_XP_BASE_PATH = "someExperienceBasePath";

    private static final String TEST_ENV_CRN = "someEnvironmentCrn";

    private static final URI TEST_URI = URI.create("somePath");

    private static final String TEST_CLOUD_PLATFORM = "AWS";

    private static final boolean NO_FORCE_DELETE = false;

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
    private Response.StatusType mockStatusType;

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
        lenient().when(mockInvocationBuilderProvider.createInvocationBuilder(mockWebTarget)).thenReturn(mockInvocationBuilder);
        lenient().when(mockStatusType.getFamily()).thenReturn(Family.SUCCESSFUL);
        lenient().when(mockResponse.getStatusInfo()).thenReturn(mockStatusType);
    }

    @Test
    void testGetWorkspaceNamesConnectedToEnvShouldObtainWebTargetFromCreator() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetForClusterFetch(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);

        when(mockRetryableWebTarget.get(mockInvocationBuilder)).thenReturn(mockResponse);
        when(mockCommonExperienceResponseReader.read(any(), any(), any())).thenReturn(Optional.of(createCpInternalEnvironmentResponse()));

        underTest.getExperienceClustersConnectedToEnv(TEST_XP_BASE_PATH, TEST_ENV_CRN);

        verify(mockCommonExperienceWebTargetProvider, times(ONCE)).createWebTargetForClusterFetch(any(), any());
        verify(mockCommonExperienceWebTargetProvider, times(ONCE)).createWebTargetForClusterFetch(TEST_XP_BASE_PATH, TEST_ENV_CRN);
    }

    @Test
    void testGetWorkspaceNamesConnectedToEnvShouldReturnTheNameOfClustersFromTheCallResult() {
        CpInternalEnvironmentResponse response = createCpInternalEnvironmentResponse();

        when(mockCommonExperienceWebTargetProvider.createWebTargetForClusterFetch(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);
        when(mockRetryableWebTarget.get(mockInvocationBuilder)).thenReturn(mockResponse);
        when(mockCommonExperienceResponseReader.read(any(), any(), any())).thenReturn(Optional.of(response));

        Set<CpInternalCluster> result = underTest.getExperienceClustersConnectedToEnv(TEST_XP_BASE_PATH, TEST_ENV_CRN);

        assertEquals(response.getResults().size(), result.size());
        response.getResults().forEach(cluster -> assertTrue(result.contains(cluster)));
    }

    @Test
    void testGetWorkspaceNamesConnectedToEnvShouldObtainInvocationBuilderFromCreator() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetForClusterFetch(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilder(mockWebTarget)).thenReturn(mockInvocationBuilder);

        when(mockRetryableWebTarget.get(mockInvocationBuilder)).thenReturn(mockResponse);
        when(mockCommonExperienceResponseReader.read(any(), any(), any())).thenReturn(Optional.of(createCpInternalEnvironmentResponse()));

        underTest.getExperienceClustersConnectedToEnv(TEST_XP_BASE_PATH, TEST_ENV_CRN);

        verify(mockInvocationBuilderProvider, times(ONCE)).createInvocationBuilder(any());
        verify(mockInvocationBuilderProvider, times(ONCE)).createInvocationBuilder(mockWebTarget);
    }

    @Test
    void testGetWorkspaceNamesConnectedToEnvShouldExecuteItsCallThroughRetryableWebTarget() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetForClusterFetch(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilder(mockWebTarget)).thenReturn(mockInvocationBuilder);

        when(mockRetryableWebTarget.get(mockInvocationBuilder)).thenReturn(mockResponse);
        when(mockCommonExperienceResponseReader.read(any(), any(), any())).thenReturn(Optional.of(createCpInternalEnvironmentResponse()));

        underTest.getExperienceClustersConnectedToEnv(TEST_XP_BASE_PATH, TEST_ENV_CRN);

        verify(mockRetryableWebTarget, times(ONCE)).get(any());
        verify(mockRetryableWebTarget, times(ONCE)).get(mockInvocationBuilder);
    }

    @Test
    void testGetWorkspaceNamesConnectedToEnvWhenCallResultIsEmptyThenIllegalStateExceptionShouldBeThrown() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetForClusterFetch(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilder(mockWebTarget)).thenReturn(mockInvocationBuilder);
        when(mockRetryableWebTarget.get(mockInvocationBuilder)).thenReturn(null);

        ExperienceOperationFailedException expectedException = assertThrows(ExperienceOperationFailedException.class,
                () -> underTest.getExperienceClustersConnectedToEnv(TEST_XP_BASE_PATH, TEST_ENV_CRN));

        assertEquals(COMMON_XP_RESPONSE_RESOLVE_ERROR_MSG, expectedException.getMessage());
    }

    @Test
    void testGetWorkspaceNamesConnectedToEnvWhenCallResultIsNotEmptyThenResponseReaderShouldBeInvokeToResolveContent() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetForClusterFetch(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilder(mockWebTarget)).thenReturn(mockInvocationBuilder);
        when(mockRetryableWebTarget.get(mockInvocationBuilder)).thenReturn(mockResponse);
        when(mockCommonExperienceResponseReader.read(any(), any(), any())).thenReturn(Optional.of(createCpInternalEnvironmentResponse()));

        underTest.getExperienceClustersConnectedToEnv(TEST_XP_BASE_PATH, TEST_ENV_CRN);

        verify(mockCommonExperienceResponseReader, times(ONCE)).read(any(), any(), any());
        verify(mockCommonExperienceResponseReader, times(ONCE)).read(TEST_URI.toString(), mockResponse, CpInternalEnvironmentResponse.class);
    }

    @Test
    void testGetWorkspaceNamesConnectedToEnvWhenResponseReaderUnableToResolveResponseThenEmptyOptionalReturnsAndResponseResultIterationDoesntHappen() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetForClusterFetch(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);
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
        when(mockCommonExperienceWebTargetProvider.createWebTargetForClusterFetch(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilder(mockWebTarget)).thenReturn(mockInvocationBuilder);
        when(mockRetryableWebTarget.get(mockInvocationBuilder)).thenReturn(mockResponse);
        when(mockCommonExperienceResponseReader.read(TEST_URI.toString(), mockResponse, CpInternalEnvironmentResponse.class))
                .thenReturn(Optional.of(mockCpInternalEnvironmentResponse));

        underTest.getExperienceClustersConnectedToEnv(TEST_XP_BASE_PATH, TEST_ENV_CRN);

        verify(mockCpInternalEnvironmentResponse, times(ONCE)).getResults();
    }

    @Test
    void testGetWorkspaceNamesConnectedToEnvWhenCallExecutionThrowsExceptionThenIllegalStateExceptionShouldBeThrown() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetForClusterFetch(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilder(mockWebTarget)).thenReturn(mockInvocationBuilder);
        when(mockRetryableWebTarget.get(mockInvocationBuilder)).thenThrow(new RuntimeException());

        ExperienceOperationFailedException expectedException = assertThrows(ExperienceOperationFailedException.class,
                () -> underTest.getExperienceClustersConnectedToEnv(TEST_XP_BASE_PATH, TEST_ENV_CRN));

        assertEquals(COMMON_XP_RESPONSE_RESOLVE_ERROR_MSG, expectedException.getMessage());
        verify(mockCommonExperienceResponseReader, never()).read(any(), any(), any());
    }

    @Test
    void testDeleteWorkspaceForEnvironmentShouldObtainWebTargetFromCreator() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetForClusterFetch(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);
        when(mockRetryableWebTarget.delete(any())).thenReturn(mockResponse);
        lenient().when(mockCommonExperienceResponseReader.read(any(), any(), any()))
                .thenReturn(Optional.of(mock(DeleteCommonExperienceWorkspaceResponse.class)));

        underTest.deleteWorkspaceForEnvironment(TEST_XP_BASE_PATH, TEST_ENV_CRN, NO_FORCE_DELETE);

        verify(mockCommonExperienceWebTargetProvider, times(ONCE)).createWebTargetForClusterFetch(any(), any());
        verify(mockCommonExperienceWebTargetProvider, times(ONCE)).createWebTargetForClusterFetch(TEST_XP_BASE_PATH, TEST_ENV_CRN);
    }

    @Test
    void testDeleteWorkspaceForEnvironmentShouldObtainInvocationBuilderFromCreator() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetForClusterFetch(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilder(mockWebTarget)).thenReturn(mockInvocationBuilder);
        when(mockRetryableWebTarget.delete(any())).thenReturn(mockResponse);
        lenient().when(mockCommonExperienceResponseReader.read(any(), any(), any()))
                .thenReturn(Optional.of(mock(DeleteCommonExperienceWorkspaceResponse.class)));

        underTest.deleteWorkspaceForEnvironment(TEST_XP_BASE_PATH, TEST_ENV_CRN, NO_FORCE_DELETE);

        verify(mockInvocationBuilderProvider, times(ONCE)).createInvocationBuilder(any());
        verify(mockInvocationBuilderProvider, times(ONCE)).createInvocationBuilder(mockWebTarget);
    }

    @Test
    void testDeleteWorkspaceForEnvironmentShouldExecuteItsCallThroughRetryableWebTarget() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetForClusterFetch(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilder(mockWebTarget)).thenReturn(mockInvocationBuilder);
        when(mockRetryableWebTarget.delete(any())).thenReturn(mockResponse);
        lenient().when(mockCommonExperienceResponseReader.read(any(), any(), any()))
                .thenReturn(Optional.of(mock(DeleteCommonExperienceWorkspaceResponse.class)));

        underTest.deleteWorkspaceForEnvironment(TEST_XP_BASE_PATH, TEST_ENV_CRN, NO_FORCE_DELETE);

        verify(mockRetryableWebTarget, times(ONCE)).delete(any());
        verify(mockRetryableWebTarget, times(ONCE)).delete(mockInvocationBuilder);
    }

    @Test
    void testDeleteWorkspaceForEnvironmentWhenCallResultIsEmptyThenIllegalStateExceptionShouldCome() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetForClusterFetch(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilder(mockWebTarget)).thenReturn(mockInvocationBuilder);
        when(mockRetryableWebTarget.delete(mockInvocationBuilder)).thenReturn(null);

        ExperienceOperationFailedException expectedException = assertThrows(ExperienceOperationFailedException.class,
                () -> underTest.deleteWorkspaceForEnvironment(TEST_XP_BASE_PATH, TEST_ENV_CRN, NO_FORCE_DELETE));

        assertNotNull(expectedException);
        assertEquals(COMMON_XP_RESPONSE_RESOLVE_ERROR_MSG, expectedException.getMessage());
    }

    @Test
    void testDeleteWorkspaceForEnvironmentWhenResponseReaderIsAbleToResolveResponseThenResponseResultIterationHappens() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetForClusterFetch(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilder(mockWebTarget)).thenReturn(mockInvocationBuilder);
        when(mockRetryableWebTarget.delete(mockInvocationBuilder)).thenReturn(mockResponse);

        underTest.deleteWorkspaceForEnvironment(TEST_XP_BASE_PATH, TEST_ENV_CRN, NO_FORCE_DELETE);
    }

    @Test
    void testDeleteWorkspaceForEnvironmentWhenCallExecutionThrowsExceptionThenIllegalStateExceptionShouldCome() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetForClusterFetch(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilder(mockWebTarget)).thenReturn(mockInvocationBuilder);
        when(mockRetryableWebTarget.delete(mockInvocationBuilder)).thenThrow(new RuntimeException());

        ExperienceOperationFailedException expectedException = assertThrows(ExperienceOperationFailedException.class,
                () -> underTest.deleteWorkspaceForEnvironment(TEST_XP_BASE_PATH, TEST_ENV_CRN, NO_FORCE_DELETE));

        assertNotNull(expectedException);
        assertEquals(COMMON_XP_RESPONSE_RESOLVE_ERROR_MSG, expectedException.getMessage());

        verify(mockCommonExperienceResponseReader, never()).read(any(), any(), any());
    }

    @Test
    void testCollectPolicyShouldObtainWebTargetFromCreator() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetForPolicyFetch(TEST_XP_BASE_PATH, TEST_CLOUD_PLATFORM)).thenReturn(mockWebTarget);
        when(mockRetryableWebTarget.get(any())).thenReturn(mockResponse);
        when(mockCommonExperienceResponseReader.read(any(), any(), any())).thenReturn(Optional.of(mock(ExperiencePolicyResponse.class)));

        underTest.collectPolicy(TEST_XP_BASE_PATH, TEST_CLOUD_PLATFORM);

        verify(mockCommonExperienceWebTargetProvider, times(ONCE)).createWebTargetForPolicyFetch(any(), any());
        verify(mockCommonExperienceWebTargetProvider, times(ONCE)).createWebTargetForPolicyFetch(TEST_XP_BASE_PATH, TEST_CLOUD_PLATFORM);
    }

    @Test
    void testCollectPolicyShouldObtainInvocationBuilderFromCreator() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetForPolicyFetch(TEST_XP_BASE_PATH, TEST_CLOUD_PLATFORM)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilderForInternalActor(mockWebTarget)).thenReturn(mockInvocationBuilder);
        when(mockRetryableWebTarget.get(any())).thenReturn(mockResponse);
        when(mockCommonExperienceResponseReader.read(any(), any(), any())).thenReturn(Optional.of(mock(ExperiencePolicyResponse.class)));

        underTest.collectPolicy(TEST_XP_BASE_PATH, TEST_CLOUD_PLATFORM);

        verify(mockInvocationBuilderProvider, times(ONCE)).createInvocationBuilderForInternalActor(any());
        verify(mockInvocationBuilderProvider, times(ONCE)).createInvocationBuilderForInternalActor(mockWebTarget);
    }

    @Test
    void testCollectPolicyShouldExecuteItsCallThroughRetryableWebTarget() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetForPolicyFetch(TEST_XP_BASE_PATH, TEST_CLOUD_PLATFORM)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilderForInternalActor(mockWebTarget)).thenReturn(mockInvocationBuilder);
        when(mockRetryableWebTarget.get(any())).thenReturn(mockResponse);
        when(mockCommonExperienceResponseReader.read(any(), any(), any())).thenReturn(Optional.of(mock(ExperiencePolicyResponse.class)));

        underTest.collectPolicy(TEST_XP_BASE_PATH, TEST_CLOUD_PLATFORM);

        verify(mockRetryableWebTarget, times(ONCE)).get(any());
        verify(mockRetryableWebTarget, times(ONCE)).get(mockInvocationBuilder);
    }

    @Test
    void testCollectPolicyWhenCallResultIsEmptyThenIllegalStateExceptionShouldCome() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetForPolicyFetch(TEST_XP_BASE_PATH, TEST_CLOUD_PLATFORM)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilderForInternalActor(mockWebTarget)).thenReturn(mockInvocationBuilder);
        when(mockRetryableWebTarget.get(mockInvocationBuilder)).thenReturn(null);

        ExperienceOperationFailedException expectedException = assertThrows(ExperienceOperationFailedException.class,
                () -> underTest.collectPolicy(TEST_XP_BASE_PATH, TEST_CLOUD_PLATFORM));

        assertNotNull(expectedException);
        assertEquals(COMMON_XP_RESPONSE_RESOLVE_ERROR_MSG, expectedException.getMessage());
    }

    @Test
    void testCollectPolicyWhenCallResultIsNotEmptyThenResponseReaderShouldBeInvokeToResolveContent() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetForPolicyFetch(TEST_XP_BASE_PATH, TEST_CLOUD_PLATFORM)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilderForInternalActor(mockWebTarget)).thenReturn(mockInvocationBuilder);
        when(mockRetryableWebTarget.get(any())).thenReturn(mockResponse);
        when(mockCommonExperienceResponseReader.read(any(), any(), any())).thenReturn(Optional.of(mock(ExperiencePolicyResponse.class)));

        underTest.collectPolicy(TEST_XP_BASE_PATH, TEST_CLOUD_PLATFORM);

        verify(mockCommonExperienceResponseReader, times(ONCE)).read(any(), any(), any());
        verify(mockCommonExperienceResponseReader, times(ONCE)).read(TEST_URI.toString(), mockResponse, ExperiencePolicyResponse.class);
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testCollectPolicyWhenResponseReaderUnableToResolveResponseThenIllegalStateExceptionShouldCome() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetForPolicyFetch(TEST_XP_BASE_PATH, TEST_CLOUD_PLATFORM)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilderForInternalActor(mockWebTarget)).thenReturn(mockInvocationBuilder);
        when(mockRetryableWebTarget.get(mockInvocationBuilder)).thenReturn(mockResponse);
        when(mockCommonExperienceResponseReader.read(TEST_URI.toString(), mockResponse, ExperiencePolicyResponse.class)).thenReturn(Optional.empty());

        ExperienceOperationFailedException expectedException = assertThrows(ExperienceOperationFailedException.class,
                () -> underTest.collectPolicy(TEST_XP_BASE_PATH, TEST_CLOUD_PLATFORM));

        assertNotNull(expectedException);
        assertEquals(COMMON_XP_RESPONSE_RESOLVE_ERROR_MSG, expectedException.getMessage());
    }

    @Test
    void testCollectPolicyWhenResponseReaderIsAbleToResolveResponseThenResponseResultIterationHappens() {
        ExperiencePolicyResponse mockCpInternalEnvironmentResponse = mock(ExperiencePolicyResponse.class);
        when(mockCommonExperienceWebTargetProvider.createWebTargetForPolicyFetch(TEST_XP_BASE_PATH, TEST_CLOUD_PLATFORM)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilderForInternalActor(mockWebTarget)).thenReturn(mockInvocationBuilder);
        when(mockRetryableWebTarget.get(mockInvocationBuilder)).thenReturn(mockResponse);
        when(mockCommonExperienceResponseReader.read(TEST_URI.toString(), mockResponse, ExperiencePolicyResponse.class))
                .thenReturn(Optional.of(mockCpInternalEnvironmentResponse));

        underTest.collectPolicy(TEST_XP_BASE_PATH, TEST_CLOUD_PLATFORM);
    }

    @Test
    void testCollectPolicyWhenCallExecutionThrowsExceptionThenIllegalStateExceptionShouldCome() {
        when(mockCommonExperienceWebTargetProvider.createWebTargetForPolicyFetch(TEST_XP_BASE_PATH, TEST_CLOUD_PLATFORM)).thenReturn(mockWebTarget);
        when(mockInvocationBuilderProvider.createInvocationBuilderForInternalActor(mockWebTarget)).thenReturn(mockInvocationBuilder);
        when(mockRetryableWebTarget.get(mockInvocationBuilder)).thenThrow(new RuntimeException());

        ExperienceOperationFailedException expectedException = assertThrows(ExperienceOperationFailedException.class,
                () -> underTest.collectPolicy(TEST_XP_BASE_PATH, TEST_CLOUD_PLATFORM));

        assertNotNull(expectedException);
        assertEquals(COMMON_XP_RESPONSE_RESOLVE_ERROR_MSG, expectedException.getMessage());

        verify(mockCommonExperienceResponseReader, never()).read(any(), any(), any());
    }

    @ParameterizedTest
    @EnumSource(value = Family.class, names = "SUCCESSFUL", mode = EXCLUDE)
    void testWhenDeletionReturnsWithNotSuccessfulStatusThenExceptionShouldCome(Family family) {
        when(mockCommonExperienceWebTargetProvider.createWebTargetForClusterFetch(TEST_XP_BASE_PATH, TEST_ENV_CRN)).thenReturn(mockWebTarget);
        when(mockStatusType.getFamily()).thenReturn(family);
        when(mockResponse.getStatusInfo()).thenReturn(mockStatusType);
        when(mockRetryableWebTarget.delete(any())).thenReturn(mockResponse);

        ExperienceOperationFailedException expectedException = assertThrows(ExperienceOperationFailedException.class,
                () -> underTest.deleteWorkspaceForEnvironment(TEST_XP_BASE_PATH, TEST_ENV_CRN, false));

        assertEquals(NOT_SUCCESSFUL_EXPERIENCE_DELETE_MESSAGE, expectedException.getMessage());
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

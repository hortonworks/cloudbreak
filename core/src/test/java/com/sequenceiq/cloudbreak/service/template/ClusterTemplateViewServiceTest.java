package com.sequenceiq.cloudbreak.service.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.verification.VerificationMode;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Type;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.view.ClusterTemplateView;
import com.sequenceiq.cloudbreak.repository.cluster.ClusterTemplateViewRepository;
import com.sequenceiq.distrox.v1.distrox.service.InternalClusterTemplateValidator;

@ExtendWith(MockitoExtension.class)
public class ClusterTemplateViewServiceTest {

    private static final VerificationMode ONCE = times(1);

    private static final String TEST_RUNTIME = "someRuntimeVersion";

    private static final String TEST_ENV_CRN = "someEnvCrn";

    private static final Long WORKSPACE_ID = 0L;

    @Mock
    private ClusterTemplateViewRepository repository;

    @Mock
    private InternalClusterTemplateValidator internalClusterTemplateValidator;

    @InjectMocks
    private ClusterTemplateViewService underTest;

    @Test
    public void testPrepareCreation() {
        BadRequestException expectedException = assertThrows(BadRequestException.class, () -> underTest.prepareCreation(new ClusterTemplateView()));

        assertEquals("Cluster template creation is not supported from ClusterTemplateViewService", expectedException.getMessage());
    }

    @Test
    public void testPrepareDeletion() {
        BadRequestException expectedException = assertThrows(BadRequestException.class, () -> underTest.prepareDeletion(new ClusterTemplateView()));

        assertEquals("Cluster template deletion is not supported from ClusterTemplateViewService", expectedException.getMessage());
    }

    @Test
    public void testWhenFindAllAvailableViewInWorkspaceIsCalledThenItsResultSetShouldBeReturnedWithoutFiltering() {
        ClusterTemplateView repositoryResult = new ClusterTemplateView();
        Set<ClusterTemplateView> resultSetFromRepository = Set.of(repositoryResult);
        when(repository.findAllActive(WORKSPACE_ID)).thenReturn(resultSetFromRepository);
        when(internalClusterTemplateValidator.shouldPopulate(repositoryResult, false)).thenReturn(true);

        Set<ClusterTemplateView> result = underTest.findAllActive(WORKSPACE_ID, false);

        assertNotNull(result);
        assertEquals(resultSetFromRepository.size(), result.size());
        assertTrue(resultSetFromRepository.containsAll(result));

        verify(repository, times(1)).findAllActive(anyLong());
        verify(repository, times(1)).findAllActive(WORKSPACE_ID);
    }

    @Test
    public void testWhenFindAllAvailableViewInWorkspaceIsCalledThenItsResultSetShouldBeNotReturned() {
        ClusterTemplateView repositoryResult = new ClusterTemplateView();
        Set<ClusterTemplateView> resultSetFromRepository = Set.of(repositoryResult);
        when(repository.findAllActive(WORKSPACE_ID)).thenReturn(resultSetFromRepository);
        when(internalClusterTemplateValidator.shouldPopulate(repositoryResult, false)).thenReturn(false);

        Set<ClusterTemplateView> result = underTest.findAllActive(WORKSPACE_ID, false);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @ParameterizedTest
    @EnumSource(CloudPlatform.class)
    public void testFindAllUserManagedAndDefaultByEnvironmentCrnWhenRepositoryCantFindAnyThenEmptySetShouldReturn(CloudPlatform cloudPlatform) {
        Set<ClusterTemplateView> expectedEmptyResultSet = new LinkedHashSet<>();
        when(repository.findAllUserManagedAndDefaultByEnvironmentCrn(any(), any(), any(), any())).thenReturn(expectedEmptyResultSet);

        Set<ClusterTemplateView> result = underTest.findAllUserManagedAndDefaultByEnvironmentCrn(WORKSPACE_ID, TEST_ENV_CRN, cloudPlatform.name(),
                TEST_RUNTIME, false, null);

        assertEquals(expectedEmptyResultSet, result);

        verify(repository, ONCE).findAllUserManagedAndDefaultByEnvironmentCrn(any(), any(), any(), any());
        verify(repository, ONCE).findAllUserManagedAndDefaultByEnvironmentCrn(WORKSPACE_ID, TEST_ENV_CRN, cloudPlatform.name(), TEST_RUNTIME);
    }

    @ParameterizedTest
    @EnumSource(CloudPlatform.class)
    public void testFindAllUserManagedAndDefaultByEnvironmentCrnWhenRepositoryCanFindClusterTemplatesThenNonEmptySetShouldReturn(CloudPlatform cloudPlatform) {
        ClusterTemplateView match = new ClusterTemplateView();
        match.setType(ClusterTemplateV4Type.DATAENGINEERING);
        Set<ClusterTemplateView> expectedNonEmptyResultSet = Set.of(match);
        when(repository.findAllUserManagedAndDefaultByEnvironmentCrn(any(), any(), any(), any())).thenReturn(expectedNonEmptyResultSet);
        when(internalClusterTemplateValidator.shouldPopulate(match, false)).thenReturn(true);

        Set<ClusterTemplateView> result = underTest.findAllUserManagedAndDefaultByEnvironmentCrn(WORKSPACE_ID, TEST_ENV_CRN, cloudPlatform.name(),
                TEST_RUNTIME, false, null);

        assertEquals(expectedNonEmptyResultSet, result);
        assertEquals(expectedNonEmptyResultSet.size(), result.size());
        assertEquals(match, result.stream().findFirst().get());

        verify(repository, ONCE).findAllUserManagedAndDefaultByEnvironmentCrn(any(), any(), any(), any());
        verify(repository, ONCE).findAllUserManagedAndDefaultByEnvironmentCrn(WORKSPACE_ID, TEST_ENV_CRN, cloudPlatform.name(), TEST_RUNTIME);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testHybridFiltering(boolean hybridEnv) {
        ClusterTemplateView hybridTemplate = mock();
        when(hybridTemplate.getType()).thenReturn(ClusterTemplateV4Type.HYBRID_DATAENGINEERING_HA);
        ClusterTemplateView nonHybridTemplate = mock();
        when(nonHybridTemplate.getType()).thenReturn(ClusterTemplateV4Type.DATAENGINEERING_HA);
        Set<ClusterTemplateView> clusterTemplateViews = Set.of(hybridTemplate, nonHybridTemplate);
        when(repository.findAllUserManagedAndDefaultByEnvironmentCrn(WORKSPACE_ID, TEST_ENV_CRN, "AWS", TEST_RUNTIME)).thenReturn(clusterTemplateViews);
        when(internalClusterTemplateValidator.shouldPopulate(any(ClusterTemplateView.class), anyBoolean())).thenReturn(true);

        Set<ClusterTemplateView> result = underTest.findAllUserManagedAndDefaultByEnvironmentCrn(WORKSPACE_ID, TEST_ENV_CRN, "AWS",
                TEST_RUNTIME, false, hybridEnv);

        Assertions.assertThat(result)
                .hasSize(1)
                .allMatch(t -> t.getType().isHybrid() == hybridEnv);
    }

}

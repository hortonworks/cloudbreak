package com.sequenceiq.cloudbreak.service.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.verification.VerificationMode;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.view.ClusterTemplateView;
import com.sequenceiq.cloudbreak.repository.cluster.ClusterTemplateViewRepository;

public class ClusterTemplateViewServiceTest {

    private static final VerificationMode ONCE = times(1);

    private static final String TEST_RUNTIME = "someRuntimeVersion";

    private static final String TEST_ENV_CRN = "someEnvCrn";

    private static final Long WORKSPACE_ID = 0L;

    @Mock
    private ClusterTemplateViewRepository repository;

    @InjectMocks
    private ClusterTemplateViewService underTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

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

        Set<ClusterTemplateView> result = underTest.findAllActive(WORKSPACE_ID);

        assertNotNull(result);
        assertEquals(resultSetFromRepository.size(), result.size());
        assertTrue(resultSetFromRepository.containsAll(result));

        verify(repository, times(1)).findAllActive(anyLong());
        verify(repository, times(1)).findAllActive(WORKSPACE_ID);
    }

    @ParameterizedTest
    @EnumSource(CloudPlatform.class)
    public void testFindAllUserManagedAndDefaultByEnvironmentCrnWhenRepositoryCantFindAnyThenEmptySetShouldReturn(CloudPlatform cloudPlatform) {
        Set<ClusterTemplateView> expectedEmptyResultSet = new LinkedHashSet<>();
        when(repository.findAllUserManagedAndDefaultByEnvironmentCrn(any(), any(), any(), any())).thenReturn(expectedEmptyResultSet);

        Set<ClusterTemplateView> result = underTest.findAllUserManagedAndDefaultByEnvironmentCrn(WORKSPACE_ID, TEST_ENV_CRN, cloudPlatform.name(),
                TEST_RUNTIME);

        assertEquals(expectedEmptyResultSet, result);

        verify(repository, ONCE).findAllUserManagedAndDefaultByEnvironmentCrn(any(), any(), any(), any());
        verify(repository, ONCE).findAllUserManagedAndDefaultByEnvironmentCrn(WORKSPACE_ID, TEST_ENV_CRN, cloudPlatform.name(), TEST_RUNTIME);
    }

    @ParameterizedTest
    @EnumSource(CloudPlatform.class)
    public void testFindAllUserManagedAndDefaultByEnvironmentCrnWhenRepositoryCanFindClusterTemplatesThenNonEmptySetShouldReturn(CloudPlatform cloudPlatform) {
        ClusterTemplateView match = new ClusterTemplateView();
        Set<ClusterTemplateView> expectedNonEmptyResultSet = Set.of(match);
        when(repository.findAllUserManagedAndDefaultByEnvironmentCrn(any(), any(), any(), any())).thenReturn(expectedNonEmptyResultSet);

        Set<ClusterTemplateView> result = underTest.findAllUserManagedAndDefaultByEnvironmentCrn(WORKSPACE_ID, TEST_ENV_CRN, cloudPlatform.name(),
                TEST_RUNTIME);

        assertEquals(expectedNonEmptyResultSet, result);
        assertEquals(expectedNonEmptyResultSet.size(), result.size());
        assertEquals(match, result.stream().findFirst().get());

        verify(repository, ONCE).findAllUserManagedAndDefaultByEnvironmentCrn(any(), any(), any(), any());
        verify(repository, ONCE).findAllUserManagedAndDefaultByEnvironmentCrn(WORKSPACE_ID, TEST_ENV_CRN, cloudPlatform.name(), TEST_RUNTIME);
    }

}

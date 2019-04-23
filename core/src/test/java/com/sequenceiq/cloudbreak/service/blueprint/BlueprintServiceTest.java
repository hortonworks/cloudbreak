package com.sequenceiq.cloudbreak.service.blueprint;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.DEFAULT;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.USER_MANAGED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_IN_PROGRESS;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.init.blueprint.BlueprintLoaderService;
import com.sequenceiq.cloudbreak.repository.BlueprintArchivedRepository;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.service.Clock;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;

@RunWith(MockitoJUnitRunner.class)
public class BlueprintServiceTest {

    private static final String NAME_ONE = "One";

    private static final String NAME_TWO = "Two";

    private static final String NAME_THREE = "Three";

    @Rule
    public final ExpectedException exceptionRule = ExpectedException.none();

    @Mock
    private ClusterService clusterService;

    @Mock
    private BlueprintRepository blueprintRepository;

    @Mock
    private BlueprintArchivedRepository blueprintArchivedRepository;

    @Mock
    private Clock clock;

    @Mock
    private BlueprintLoaderService blueprintLoaderService;

    @InjectMocks
    private BlueprintService underTest;

    private Blueprint blueprint = new Blueprint();

    @Before
    public void setup() {
        blueprint = getBlueprint("name", USER_MANAGED);
    }

    @Test
    public void testDeletionWithZeroClusters() {
        when(clusterService.findNotDeletedByBlueprint(any())).thenReturn(Collections.emptySet());

        Blueprint deleted = underTest.delete(blueprint);

        assertNotNull(deleted);
    }

    @Test
    public void testDeletionWithNonTerminatedCluster() {
        Cluster cluster = getCluster("c1", 1L, blueprint, AVAILABLE);
        exceptionRule.expect(BadRequestException.class);
        exceptionRule.expectMessage("c1");
        when(clusterService.findNotDeletedByBlueprint(any())).thenReturn(Set.of(cluster));

        underTest.delete(blueprint);
    }

    @Test
    public void testDeleteByIdArchives() {
        blueprint.setArchived(false);
        blueprint.setStatus(USER_MANAGED);
        when(blueprintRepository.findById(2L)).thenReturn(Optional.of(blueprint));

        underTest.delete(2L);

        assertTrue(blueprint.isArchived());
        verify(blueprintRepository).save(blueprint);
        verify(blueprintRepository, never()).delete(blueprint);
    }

    @Test
    public void testDeletionWithTerminatedClusters() {
        when(clusterService.findNotDeletedByBlueprint(any())).thenReturn(Set.of());

        Blueprint deleted = underTest.delete(blueprint);

        assertNotNull(deleted);
    }

    @Test
    public void testDeletionWithTerminatedAndNonTerminatedClusters() {
        Set<Cluster> clusters = getClusterWithStatus(AVAILABLE, DELETE_IN_PROGRESS);

        when(clusterService.findNotDeletedByBlueprint(any())).thenReturn(clusters);

        try {
            underTest.delete(blueprint);
        } catch (BadRequestException e) {
            assertTrue(e.getMessage().contains("c1"));
        }
    }

    @Test
    public void testGetByNameForWorkspaceAndLoadDefaultsIfNecessaryWhenFound() {
        Blueprint blueprint = getBlueprint(NAME_ONE, DEFAULT);
        when(blueprintRepository.findByWorkspaceIdAndName(1L, "One")).thenReturn(Optional.of(blueprint));

        Blueprint foundBlueprint = underTest.getByNameForWorkspaceAndLoadDefaultsIfNecessary(NAME_ONE, getWorkspace());

        assertEquals("One", foundBlueprint.getName());
        verify(blueprintRepository).findByWorkspaceIdAndName(1L, NAME_ONE);
        verify(blueprintRepository, never()).findAllByWorkspaceIdAndStatus(anyLong(), any());
        verify(blueprintLoaderService, never()).isAddingDefaultBlueprintsNecessaryForTheUser(any(), any());
    }

    @Test
    public void testGetByNameForWorkspaceAndLoadDefaultsIfNecessaryWhenLoaded() {
        Blueprint blueprint1 = getBlueprint(NAME_ONE, DEFAULT);
        Blueprint blueprint2 = getBlueprint(NAME_TWO, DEFAULT);
        when(blueprintRepository.findByWorkspaceIdAndName(1L, NAME_TWO)).thenReturn(Optional.empty());
        when(blueprintRepository.findAllByWorkspaceIdAndStatus(anyLong(), any())).thenReturn(Set.of(blueprint1));
        when(blueprintLoaderService.isAddingDefaultBlueprintsNecessaryForTheUser(any(), any())).thenReturn(true);
        when(blueprintLoaderService.loadBlueprintsForTheWorkspace(any(), any(), any(), any())).thenReturn(Set.of(blueprint1, blueprint2));

        Blueprint foundBlueprint = underTest.getByNameForWorkspaceAndLoadDefaultsIfNecessary(NAME_TWO, getWorkspace());

        assertEquals(NAME_TWO, foundBlueprint.getName());
        verify(blueprintRepository).findByWorkspaceIdAndName(1L, NAME_TWO);
        verify(blueprintRepository).findAllByWorkspaceIdAndStatus(anyLong(), any());
        verify(blueprintLoaderService).isAddingDefaultBlueprintsNecessaryForTheUser(any(), any());
        verify(blueprintLoaderService).loadBlueprintsForTheWorkspace(any(), any(), any(), any());
    }

    @Test
    public void testGetByNameForWorkspaceAndLoadDefaultsIfNecessaryWhenNotFound() {
        Blueprint blueprint1 = getBlueprint(NAME_ONE, DEFAULT);
        Blueprint blueprint2 = getBlueprint(NAME_TWO, DEFAULT);
        when(blueprintRepository.findByWorkspaceIdAndName(1L, NAME_THREE)).thenReturn(Optional.empty());
        when(blueprintRepository.findAllByWorkspaceIdAndStatus(anyLong(), any())).thenReturn(Set.of(blueprint1));
        when(blueprintLoaderService.isAddingDefaultBlueprintsNecessaryForTheUser(any(), any())).thenReturn(true);
        when(blueprintLoaderService.loadBlueprintsForTheWorkspace(any(), any(), any(), any())).thenReturn(Set.of(blueprint1, blueprint2));

        try {
            underTest.getByNameForWorkspaceAndLoadDefaultsIfNecessary(NAME_THREE, getWorkspace());
        } catch (NotFoundException e) {
            assertThat(e.getMessage(), containsString(NAME_THREE));
        }

        verify(blueprintRepository).findByWorkspaceIdAndName(1L, NAME_THREE);
        verify(blueprintRepository).findAllByWorkspaceIdAndStatus(anyLong(), any());
        verify(blueprintLoaderService).isAddingDefaultBlueprintsNecessaryForTheUser(any(), any());
        verify(blueprintLoaderService).loadBlueprintsForTheWorkspace(any(), any(), any(), any());
    }

    private Set<Cluster> getClusterWithStatus(Status... statuses) {
        Set<Cluster> clusters = new HashSet<>();
        long id = 0L;
        for (Status status : statuses) {
            clusters.add(getCluster(++id, status));
        }
        return clusters;
    }

    private Cluster getCluster(Long id, Status status) {
        return getCluster("c" + id, id, blueprint, status);
    }

    private Cluster getCluster(String name, Long id, Blueprint blueprint, Status status) {
        Cluster cluster1 = new Cluster();
        cluster1.setName(name);
        cluster1.setId(id);
        cluster1.setBlueprint(blueprint);
        cluster1.setStatus(status);
        return cluster1;
    }

    private Blueprint getBlueprint(String name, ResourceStatus status) {
        Blueprint blueprint = new Blueprint();
        blueprint.setName(name);
        blueprint.setWorkspace(getWorkspace());
        blueprint.setStatus(status);
        return blueprint;
    }

    private Workspace getWorkspace() {
        Workspace workspace = new Workspace();
        workspace.setId(1L);
        return workspace;
    }
}
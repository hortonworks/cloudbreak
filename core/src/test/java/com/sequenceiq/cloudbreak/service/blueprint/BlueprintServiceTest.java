package com.sequenceiq.cloudbreak.service.blueprint;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.dto.BlueprintAccessDto.BlueprintAccessDtoBuilder.aBlueprintAccessDtoBuilder;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.DEFAULT;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.USER_MANAGED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.PRE_DELETE_IN_PROGRESS;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.init.blueprint.BlueprintLoaderService;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@RunWith(MockitoJUnitRunner.class)
public class BlueprintServiceTest {

    private static final String MISSING_CRN_OR_NAME_EXCEPTION_MESSAGE = "No name or crn provieded, hence unable to obtain blueprint!";

    private static final String NULL_DTO_EXCEPTION_MESSAGE = "BlueprintAccessDto should not be null";

    @Rule
    public final ExpectedException exceptionRule = ExpectedException.none();

    @Mock
    private ClusterService clusterService;

    @Mock
    private BlueprintRepository blueprintRepository;

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
    public void testDeleteByWorkspaceWhenDtoNameFilledThenDeleteCalled() {
        when(blueprintRepository.findByNameAndWorkspaceId(blueprint.getName(), blueprint.getWorkspace().getId())).thenReturn(Optional.of(blueprint));

        Blueprint result = underTest.deleteByWorkspace(aBlueprintAccessDtoBuilder().withName(blueprint.getName()).build(), blueprint.getWorkspace().getId());

        assertEquals(blueprint, result);
        verify(blueprintRepository, times(1)).findByNameAndWorkspaceId(anyString(), anyLong());
        verify(blueprintRepository, times(1)).findByNameAndWorkspaceId(blueprint.getName(), blueprint.getWorkspace().getId());
        verify(blueprintRepository, times(1)).delete(any(Blueprint.class));
        verify(blueprintRepository, times(1)).delete(blueprint);
    }

    @Test
    public void testDeleteByWorkspaceWhenDtoCrnFilledThenDeleteCalled() {
        when(blueprintRepository.findByCrnAndWorkspaceId(blueprint.getCrn(), blueprint.getWorkspace().getId())).thenReturn(Optional.of(blueprint));

        Blueprint result = underTest.deleteByWorkspace(aBlueprintAccessDtoBuilder().withCrn(blueprint.getCrn()).build(), blueprint.getWorkspace().getId());

        assertEquals(blueprint, result);
        verify(blueprintRepository, times(1)).findByCrnAndWorkspaceId(anyString(), anyLong());
        verify(blueprintRepository, times(1)).findByCrnAndWorkspaceId(blueprint.getCrn(), blueprint.getWorkspace().getId());
        verify(blueprintRepository, times(1)).delete(any(Blueprint.class));
        verify(blueprintRepository, times(1)).delete(blueprint);
    }

    @Test
    public void testDeleteByWorkspaceWhenNeitherCrnOrNameProvidedThenBadRequestExceptionComes() {
        exceptionRule.expect(BadRequestException.class);
        exceptionRule.expectMessage(MISSING_CRN_OR_NAME_EXCEPTION_MESSAGE);

        underTest.deleteByWorkspace(aBlueprintAccessDtoBuilder().build(), blueprint.getWorkspace().getId());

        verify(blueprintRepository, times(0)).findByCrnAndWorkspaceId(anyString(), anyLong());
        verify(blueprintRepository, times(0)).delete(any());
    }

    @Test
    public void  testDeleteByWorkspaceIfDtoIsNullThenIllegalArgumentExceptionComes() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage(NULL_DTO_EXCEPTION_MESSAGE);

        underTest.deleteByWorkspace(null, blueprint.getWorkspace().getId());
    }

    @Test
    public void testGetByWorkspaceWhenDtoNameFilledThenProperGetCalled() {
        when(blueprintRepository.findByNameAndWorkspaceId(blueprint.getName(), blueprint.getWorkspace().getId())).thenReturn(Optional.of(blueprint));

        Blueprint result = underTest.getByWorkspace(aBlueprintAccessDtoBuilder().withName(blueprint.getName()).build(), blueprint.getWorkspace().getId());

        assertEquals(blueprint, result);
        verify(blueprintRepository, times(1)).findByNameAndWorkspaceId(anyString(), anyLong());
        verify(blueprintRepository, times(1)).findByNameAndWorkspaceId(blueprint.getName(), blueprint.getWorkspace().getId());
    }

    @Test
    public void testGetByWorkspaceWhenDtoCrnFilledThenProperGetCalled() {
        when(blueprintRepository.findByCrnAndWorkspaceId(blueprint.getCrn(), blueprint.getWorkspace().getId())).thenReturn(Optional.of(blueprint));

        Blueprint result = underTest.getByWorkspace(aBlueprintAccessDtoBuilder().withCrn(blueprint.getCrn()).build(), blueprint.getWorkspace().getId());

        assertEquals(blueprint, result);
        verify(blueprintRepository, times(1)).findByCrnAndWorkspaceId(anyString(), anyLong());
        verify(blueprintRepository, times(1)).findByCrnAndWorkspaceId(blueprint.getCrn(), blueprint.getWorkspace().getId());
    }

    @Test
    public void testGetByWorkspaceWhenNeitherCrnOrNameProvidedThenBadRequestExceptionComes() {
        exceptionRule.expect(BadRequestException.class);
        exceptionRule.expectMessage(MISSING_CRN_OR_NAME_EXCEPTION_MESSAGE);

        underTest.getByWorkspace(aBlueprintAccessDtoBuilder().build(), blueprint.getWorkspace().getId());

        verify(blueprintRepository, times(0)).findByCrnAndWorkspaceId(anyString(), anyLong());
        verify(blueprintRepository, times(0)).save(any());
    }

    @Test
    public void  testGetByWorkspaceIfDtoIsNullThenIllegalArgumentExceptionComes() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage(NULL_DTO_EXCEPTION_MESSAGE);

        underTest.getByWorkspace(null, blueprint.getWorkspace().getId());
    }

    @Test
    public void testDeletionWithZeroClusters() {
        when(clusterService.findByBlueprint(any())).thenReturn(Collections.emptySet());

        Blueprint deleted = underTest.delete(blueprint);

        assertNotNull(deleted);
    }

    @Test
    public void testDeletionWithNonTerminatedCluster() {
        Cluster cluster = getCluster("c1", 1L, blueprint, AVAILABLE);
        exceptionRule.expect(BadRequestException.class);
        exceptionRule.expectMessage("c1");
        when(clusterService.findByBlueprint(any())).thenReturn(Set.of(cluster));

        underTest.delete(blueprint);
    }

    @Test
    public void testDeletionWithTerminatedClusters() {
        Set<Cluster> clusters = getClusterWithStatus(PRE_DELETE_IN_PROGRESS, DELETE_IN_PROGRESS, DELETE_COMPLETED, DELETE_FAILED);

        when(clusterService.findByBlueprint(any())).thenReturn(clusters);

        Blueprint deleted = underTest.delete(blueprint);

        assertNotNull(deleted);
        verify(clusterService, times(1)).saveAll(anyCollection());
    }

    @Test
    public void testDeletionWithTerminatedAndNonTerminatedClusters() {
        Set<Cluster> clusters = getClusterWithStatus(AVAILABLE, DELETE_IN_PROGRESS);

        when(clusterService.findByBlueprint(any())).thenReturn(clusters);

        try {
            underTest.delete(blueprint);
        } catch (BadRequestException e) {
            assertTrue(e.getMessage().contains("c1"));
            assertFalse(e.getMessage().contains("c2"));
        }
        verify(clusterService, times(1)).saveAll(anyCollection());
    }

    @Test
    public void testGetByNameForWorkspaceAndLoadDefaultsIfNecessaryWhenFound() {
        Blueprint blueprint1 = getBlueprint("One", DEFAULT);
        when(blueprintRepository.findAllByNotDeletedInWorkspace(1L)).thenReturn(Set.of(blueprint1));

        Blueprint foundBlueprint = underTest.getByNameForWorkspaceAndLoadDefaultsIfNecessary("One", getWorkspace());

        assertEquals("One", foundBlueprint.getName());
        verify(blueprintRepository).findAllByNotDeletedInWorkspace(1L);
        verify(blueprintRepository, never()).findAllByWorkspaceIdAndStatusIn(anyLong(), any());
        verify(blueprintLoaderService, never()).isAddingDefaultBlueprintsNecessaryForTheUser(any());
    }

    @Test
    public void testGetByNameForWorkspaceAndLoadDefaultsIfNecessaryWhenLoaded() {
        Blueprint blueprint1 = getBlueprint("One", DEFAULT);
        Blueprint blueprint2 = getBlueprint("Two", DEFAULT);
        when(blueprintRepository.findAllByNotDeletedInWorkspace(1L)).thenReturn(Set.of(blueprint1));
        when(blueprintRepository.findAllByWorkspaceIdAndStatusIn(anyLong(), any())).thenReturn(Set.of(blueprint1));
        when(blueprintLoaderService.isAddingDefaultBlueprintsNecessaryForTheUser(any())).thenReturn(true);
        when(blueprintLoaderService.loadBlueprintsForTheWorkspace(any(), any(), any())).thenReturn(Set.of(blueprint1, blueprint2));

        Blueprint foundBlueprint = underTest.getByNameForWorkspaceAndLoadDefaultsIfNecessary("Two", getWorkspace());

        assertEquals("Two", foundBlueprint.getName());
        verify(blueprintRepository).findAllByNotDeletedInWorkspace(1L);
        verify(blueprintRepository).findAllByWorkspaceIdAndStatusIn(anyLong(), any());
        verify(blueprintLoaderService).isAddingDefaultBlueprintsNecessaryForTheUser(any());
        verify(blueprintLoaderService).loadBlueprintsForTheWorkspace(any(), any(), any());
    }

    @Test
    public void testGetByNameForWorkspaceAndLoadDefaultsIfNecessaryWhenNotFound() {
        Blueprint blueprint1 = getBlueprint("One", DEFAULT);
        Blueprint blueprint2 = getBlueprint("Two", DEFAULT);
        when(blueprintRepository.findAllByNotDeletedInWorkspace(1L)).thenReturn(Set.of(blueprint1));
        when(blueprintRepository.findAllByWorkspaceIdAndStatusIn(anyLong(), any())).thenReturn(Set.of(blueprint1));
        when(blueprintLoaderService.isAddingDefaultBlueprintsNecessaryForTheUser(any())).thenReturn(true);
        when(blueprintLoaderService.loadBlueprintsForTheWorkspace(any(), any(), any())).thenReturn(Set.of(blueprint1, blueprint2));

        try {
            underTest.getByNameForWorkspaceAndLoadDefaultsIfNecessary("Three", getWorkspace());
        } catch (NotFoundException e) {
            assertThat(e.getMessage(), containsString("Three"));
        }

        verify(blueprintRepository).findAllByNotDeletedInWorkspace(1L);
        verify(blueprintRepository).findAllByWorkspaceIdAndStatusIn(anyLong(), any());
        verify(blueprintLoaderService).isAddingDefaultBlueprintsNecessaryForTheUser(any());
        verify(blueprintLoaderService).loadBlueprintsForTheWorkspace(any(), any(), any());
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
        blueprint.setCreator("someone");
        blueprint.setCrn("someCrn");
        return blueprint;
    }

    private Workspace getWorkspace() {
        Workspace workspace = new Workspace();
        workspace.setId(1L);
        return workspace;
    }
}
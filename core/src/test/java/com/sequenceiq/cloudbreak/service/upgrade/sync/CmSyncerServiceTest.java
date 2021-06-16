package com.sequenceiq.cloudbreak.service.upgrade.sync;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterComponentUpdater;
import com.sequenceiq.cloudbreak.service.upgrade.StackComponentUpdater;

@ExtendWith(MockitoExtension.class)
public class CmSyncerServiceTest {

    @Mock
    private StackComponentUpdater stackComponentUpdater;

    @Mock
    private ClusterComponentUpdater clusterComponentUpdater;

    @Mock
    private CmInstalledComponentFinderService cmInstalledComponentFinderService;

    @Mock
    private CmServerQueryService cmServerQueryService;

    @InjectMocks
    private CmSyncerService underTest;

    @Mock
    private Stack stack;

    private final Component cdhComponent = new Component(ComponentType.CDH_PRODUCT_DETAILS, "CDH", new Json("{}"), new Stack());

    private final Component cmRepoComponent = new Component(ComponentType.CM_REPO_DETAILS, "CmRepoDetails", new Json("{}"), new Stack());

    @Test
    void testSyncFromCmToDbWhenCmServerRunningThenSyncIsExecuted() {
        when(cmServerQueryService.isCmServerRunning(stack)).thenReturn(true);
        Set<Image> candidateImages = Set.of(mock(Image.class));
        when(cmInstalledComponentFinderService.findParcelComponents(stack, candidateImages)).thenReturn(Set.of(cdhComponent));
        when(cmInstalledComponentFinderService.findCmRepoComponent(stack, candidateImages)).thenReturn(Optional.of(cmRepoComponent));

        underTest.syncFromCmToDb(stack, candidateImages);

        verify(cmInstalledComponentFinderService).findCmRepoComponent(eq(stack), eq(candidateImages));
        verify(cmInstalledComponentFinderService).findParcelComponents(eq(stack), eq(candidateImages));

        ArgumentCaptor<Set<Component>> componentArgumentCaptor = ArgumentCaptor.forClass(Set.class);
        verify(stackComponentUpdater).updateComponentsByStackId(eq(stack), componentArgumentCaptor.capture(), anyBoolean());
        Set<Component> componentsToPersist = componentArgumentCaptor.getValue();
        assertThat(componentsToPersist, hasSize(2));
        assertThat(componentsToPersist, containsInAnyOrder(
                hasProperty("componentType", is(ComponentType.CDH_PRODUCT_DETAILS)),
                hasProperty("componentType", is(ComponentType.CM_REPO_DETAILS))
        ));

        ArgumentCaptor<Set<Component>> clusterComponentArgumentCaptor = ArgumentCaptor.forClass(Set.class);
        verify(clusterComponentUpdater).updateClusterComponentsByStackId(eq(stack), clusterComponentArgumentCaptor.capture(), anyBoolean());
        Set<Component> clusterComponentsToPersist = clusterComponentArgumentCaptor.getValue();
        assertThat(clusterComponentsToPersist, hasSize(2));
        assertThat(componentsToPersist, containsInAnyOrder(
                hasProperty("componentType", is(ComponentType.CDH_PRODUCT_DETAILS)),
                hasProperty("componentType", is(ComponentType.CM_REPO_DETAILS))
        ));
    }

    @Test
    void testSyncFromCmToDbWhenCmServerDownThenSyncSkipped() {
        when(cmServerQueryService.isCmServerRunning(stack)).thenReturn(false);
        Set<Image> candidateImages = Set.of(mock(Image.class));

        underTest.syncFromCmToDb(stack, candidateImages);

        verify(cmServerQueryService).isCmServerRunning(eq(stack));
        verify(cmInstalledComponentFinderService, never()).findCmRepoComponent(any(), any());
        verify(cmInstalledComponentFinderService, never()).findParcelComponents(any(), any());
        verify(stackComponentUpdater, never()).updateComponentsByStackId(any(), any(), anyBoolean());
        verify(clusterComponentUpdater, never()).updateClusterComponentsByStackId(any(), any(), anyBoolean());
    }

    @Test
    void testSyncFromCmToDbWhenNoCandidateImagesThenSyncSkipped() {
        when(cmServerQueryService.isCmServerRunning(stack)).thenReturn(true);
        Set<Image> candidateImages = Set.of();

        underTest.syncFromCmToDb(stack, candidateImages);

        verify(cmServerQueryService).isCmServerRunning(eq(stack));
        verify(cmInstalledComponentFinderService, never()).findCmRepoComponent(any(), any());
        verify(cmInstalledComponentFinderService, never()).findParcelComponents(any(), any());
        verify(stackComponentUpdater, never()).updateComponentsByStackId(any(), any(), anyBoolean());
        verify(clusterComponentUpdater, never()).updateClusterComponentsByStackId(any(), any(), anyBoolean());
    }

}

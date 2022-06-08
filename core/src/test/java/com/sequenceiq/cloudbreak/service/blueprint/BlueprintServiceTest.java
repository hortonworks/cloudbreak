package com.sequenceiq.cloudbreak.service.blueprint;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.DEFAULT;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.SERVICE_MANAGED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.USER_MANAGED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.powermock.reflect.Whitebox;
import org.springframework.validation.Errors;

import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.BlueprintView;
import com.sequenceiq.cloudbreak.domain.view.ClusterTemplateView;
import com.sequenceiq.cloudbreak.init.blueprint.BlueprintLoaderService;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.repository.BlueprintViewRepository;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.runtimes.SupportedRuntimes;
import com.sequenceiq.cloudbreak.service.template.ClusterTemplateViewService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.LegacyRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.validation.HueWorkaroundValidatorService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@ExtendWith(MockitoExtension.class)
public class BlueprintServiceTest {
    private static final String ACCOUNT_ID = "ACCOUNT_ID";

    private static final String CREATOR = "CREATOR";

    @Mock
    private TransactionService transactionService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private BlueprintRepository blueprintRepository;

    @Mock
    private BlueprintViewRepository blueprintViewRepository;

    @Mock
    private BlueprintLoaderService blueprintLoaderService;

    @Mock
    private UserService userService;

    @Mock
    private LegacyRestRequestThreadLocalService legacyRestRequestThreadLocalService;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private CloudbreakUser cloudbreakUser;

    @Mock
    private User user;

    @Mock
    private BlueprintValidator blueprintValidator;

    @Mock
    private OwnerAssignmentService ownerAssignmentService;

    @Spy
    private BlueprintListFilters blueprintListFilters;

    @Mock
    private HueWorkaroundValidatorService hueWorkaroundValidatorService;

    @Mock
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Mock
    private ClusterTemplateViewService clusterTemplateViewService;

    @Mock
    private ImageCatalogService imageCatalogService;

    @InjectMocks
    private BlueprintService underTest;

    @BeforeEach
    public void setup() {
        CrnTestUtil.mockCrnGenerator(regionAwareCrnGenerator);
        SupportedRuntimes supportedRuntimes = new SupportedRuntimes();
        Whitebox.setInternalState(blueprintListFilters, "supportedRuntimes", supportedRuntimes);
        Whitebox.setInternalState(supportedRuntimes, "imageCatalogService", imageCatalogService);
        lenient().when(legacyRestRequestThreadLocalService.getCloudbreakUser()).thenReturn(cloudbreakUser);
        lenient().when(userService.getOrCreate(cloudbreakUser)).thenReturn(user);
        lenient().when(workspaceService.get(1L, user)).thenReturn(getWorkspace());
        lenient().doNothing().when(ownerAssignmentService).notifyResourceDeleted(anyString());
    }

    @Test
    public void testDeleteByWorkspaceWhenDtoNameFilledThenDeleteCalled() {
        Blueprint blueprint = getBlueprint("name", USER_MANAGED);
        when(blueprintRepository.findByNameAndWorkspaceId(blueprint.getName(),
                blueprint.getWorkspace().getId())).thenReturn(Optional.of(blueprint));

        Blueprint result = underTest.deleteByWorkspace(NameOrCrn.ofName(blueprint.getName()), blueprint.getWorkspace().getId());

        assertEquals(blueprint, result);
        verify(blueprintRepository, times(1))
                .findByNameAndWorkspaceId(anyString(), anyLong());
        verify(blueprintRepository, times(1))
                .findByNameAndWorkspaceId(blueprint.getName(), blueprint.getWorkspace().getId());
        verify(blueprintRepository, times(1)).delete(any(Blueprint.class));
        verify(blueprintRepository, times(1)).delete(blueprint);
    }

    @Test
    public void testDeleteServiceManaged() {
        Blueprint blueprint = getBlueprint("name", SERVICE_MANAGED);
        when(blueprintRepository.findByNameAndWorkspaceId(blueprint.getName(),
                blueprint.getWorkspace().getId())).thenReturn(Optional.of(blueprint));

        Blueprint result = underTest.deleteByWorkspace(NameOrCrn.ofName(blueprint.getName()), blueprint.getWorkspace().getId());

        assertEquals(blueprint, result);
        verify(blueprintRepository, times(1))
                .findByNameAndWorkspaceId(anyString(), anyLong());
        verify(blueprintRepository, times(1))
                .findByNameAndWorkspaceId(blueprint.getName(), blueprint.getWorkspace().getId());
        verify(blueprintRepository, times(1)).delete(any(Blueprint.class));
        verify(blueprintRepository, times(1)).delete(blueprint);
    }

    @Test
    public void testDeleteByWorkspaceWhenDtoCrnFilledThenDeleteCalled() {
        Blueprint blueprint = getBlueprint("name", USER_MANAGED);
        when(blueprintRepository.findByResourceCrnAndWorkspaceId(blueprint.getResourceCrn(),
                blueprint.getWorkspace().getId())).thenReturn(Optional.of(blueprint));

        Blueprint result = underTest.deleteByWorkspace(NameOrCrn.ofCrn(blueprint.getResourceCrn()), blueprint.getWorkspace().getId());

        assertEquals(blueprint, result);
        verify(blueprintRepository, times(1))
                .findByResourceCrnAndWorkspaceId(anyString(), anyLong());
        verify(blueprintRepository, times(1))
                .findByResourceCrnAndWorkspaceId(blueprint.getResourceCrn(), blueprint.getWorkspace().getId());
        verify(blueprintRepository, times(1)).delete(any(Blueprint.class));
        verify(blueprintRepository, times(1)).delete(blueprint);
    }

    @Test
    public void testGetByWorkspaceWhenDtoNameFilledThenProperGetCalled() {
        Blueprint blueprint = getBlueprint("name", USER_MANAGED);
        when(blueprintRepository.findByNameAndWorkspaceId(blueprint.getName(),
                blueprint.getWorkspace().getId())).thenReturn(Optional.of(blueprint));

        Blueprint result = underTest.getByWorkspace(NameOrCrn.ofName(blueprint.getName()), blueprint.getWorkspace().getId());

        assertEquals(blueprint, result);
        verify(blueprintRepository, times(1))
                .findByNameAndWorkspaceId(anyString(), anyLong());
        verify(blueprintRepository, times(1))
                .findByNameAndWorkspaceId(blueprint.getName(), blueprint.getWorkspace().getId());
    }

    @Test
    public void testGetByWorkspaceWhenDtoCrnFilledThenProperGetCalled() {
        Blueprint blueprint = getBlueprint("name", USER_MANAGED);
        when(blueprintRepository.findByResourceCrnAndWorkspaceId(blueprint.getResourceCrn(),
                blueprint.getWorkspace().getId())).thenReturn(Optional.of(blueprint));

        Blueprint result = underTest.getByWorkspace(NameOrCrn.ofCrn(blueprint.getResourceCrn()), blueprint.getWorkspace().getId());

        assertEquals(blueprint, result);
        verify(blueprintRepository, times(1))
                .findByResourceCrnAndWorkspaceId(anyString(), anyLong());
        verify(blueprintRepository, times(1))
                .findByResourceCrnAndWorkspaceId(blueprint.getResourceCrn(), blueprint.getWorkspace().getId());
    }

    @Test
    public void testDeletionWithZeroClusters() {
        Blueprint blueprint = getBlueprint("name", USER_MANAGED);
        when(clusterService.findByBlueprint(any())).thenReturn(Collections.emptySet());

        Blueprint deleted = underTest.delete(blueprint);

        assertNotNull(deleted);
    }

    @Test
    public void testDeletionWithNonTerminatedClusterAndStack() {
        Blueprint blueprint = getBlueprint("name", USER_MANAGED);
        Cluster cluster = getCluster("c1", 1L, blueprint, DetailedStackStatus.AVAILABLE);
        ClusterTemplateView clusterTemplateView = new ClusterTemplateView();
        clusterTemplateView.setName("ClusterDefinition");

        when(clusterTemplateViewService.findAllByStackIds(any())).thenReturn(Set.of(clusterTemplateView));
        when(clusterService.findByBlueprint(any())).thenReturn(Set.of(cluster));

        BadRequestException exception = Assertions.assertThrows(BadRequestException.class, () -> underTest.delete(blueprint));
        assertEquals("There is a cluster ['ClusterDefinition'] which uses cluster template 'name'. "
                        + "Please remove this cluster before deleting the cluster template.",
                exception.getMessage());
    }

    @Test
    public void testDeletionWithTerminatedClustersNonTerminatedStacks() {
        Blueprint blueprint = getBlueprint("name", USER_MANAGED);
        Set<Cluster> clusters = new HashSet<>();
        clusters.add(getCluster("c1", 1L, blueprint, DetailedStackStatus.PRE_DELETE_IN_PROGRESS));
        clusters.add(getCluster("c2", 1L, blueprint, DetailedStackStatus.DELETE_IN_PROGRESS));
        clusters.add(getCluster("c3", 1L, blueprint, DetailedStackStatus.DELETE_COMPLETED));
        ClusterTemplateView clusterTemplateView = new ClusterTemplateView();
        clusterTemplateView.setName("ClusterDefinition");

        when(clusterTemplateViewService.findAllByStackIds(any())).thenReturn(Set.of(clusterTemplateView));
        when(clusterService.findByBlueprint(any())).thenReturn(clusters);

        BadRequestException exception = Assertions.assertThrows(BadRequestException.class, () -> underTest.delete(blueprint));
        assertEquals("There is a cluster ['ClusterDefinition'] which uses cluster template 'name'. "
                        + "Please remove this cluster before deleting the cluster template.",
                exception.getMessage());
    }

    @Test
    public void testDeletionWithTerminatedAndNonTerminatedClusters() {
        Blueprint blueprint = getBlueprint("name", USER_MANAGED);
        Set<Cluster> clusters = new HashSet<>();
        clusters.add(getCluster("c1", 1L, blueprint, DetailedStackStatus.PRE_DELETE_IN_PROGRESS));
        clusters.add(getCluster("c2", 1L, blueprint, DetailedStackStatus.DELETE_COMPLETED));
        ClusterTemplateView clusterTemplateView = new ClusterTemplateView();
        clusterTemplateView.setName("ClusterDefinition1");
        ClusterTemplateView clusterTemplateView2 = new ClusterTemplateView();
        clusterTemplateView2.setName("ClusterDefinition2");
        when(clusterTemplateViewService.findAllByStackIds(any())).thenReturn(Set.of(clusterTemplateView, clusterTemplateView2));
        when(clusterService.findByBlueprint(any())).thenReturn(clusters);

        try {
            underTest.delete(blueprint);
        } catch (BadRequestException e) {
            assertTrue(e.getMessage().contains("ClusterDefinition1"));
            assertFalse(e.getMessage().contains("ClusterDefinition2"));
        }
        verify(clusterService, times(1)).saveAll(anyCollection());
    }

    @Test
    public void testGetByNameForWorkspaceAndLoadDefaultsIfNecessaryWhenFound() {
        Blueprint blueprint1 = getBlueprint("One", DEFAULT);
        when(blueprintRepository.findByNameAndWorkspaceId("One", 1L)).thenReturn(Optional.of(blueprint1));

        Blueprint foundBlueprint = underTest.getByNameForWorkspaceAndLoadDefaultsIfNecessary("One", getWorkspace());

        assertEquals("One", foundBlueprint.getName());
        verify(blueprintRepository).findByNameAndWorkspaceId("One", 1L);
        verify(blueprintRepository, never()).findAllByWorkspaceIdAndStatusIn(anyLong(), any());
        verify(blueprintLoaderService, never()).isAddingDefaultBlueprintsNecessaryForTheUser(any());
    }

    @Test
    public void testGetByNameForWorkspaceAndLoadDefaultsIfNecessaryWhenLoaded() {
        Blueprint blueprint1 = getBlueprint("One", DEFAULT);
        Blueprint blueprint2 = getBlueprint("Two", DEFAULT);
        when(blueprintRepository.findByNameAndWorkspaceId("Two", 1L)).thenReturn(Optional.empty());
        when(blueprintRepository.findAllByWorkspaceIdAndStatusIn(anyLong(), any())).thenReturn(Set.of(blueprint1));
        when(blueprintLoaderService.isAddingDefaultBlueprintsNecessaryForTheUser(any())).thenReturn(true);
        when(blueprintLoaderService.loadBlueprintsForTheWorkspace(any(), any(), any())).thenReturn(Set.of(blueprint1, blueprint2));

        Blueprint foundBlueprint = underTest.getByNameForWorkspaceAndLoadDefaultsIfNecessary("Two", getWorkspace());

        assertEquals("Two", foundBlueprint.getName());
        verify(blueprintRepository).findByNameAndWorkspaceId("Two", 1L);
        verify(blueprintRepository).findAllByWorkspaceIdAndStatusIn(anyLong(), any());
        verify(blueprintLoaderService).isAddingDefaultBlueprintsNecessaryForTheUser(any());
        verify(blueprintLoaderService).loadBlueprintsForTheWorkspace(any(), any(), any());
    }

    @Test
    public void testGetByNameForWorkspaceAndLoadDefaultsIfNecessaryWhenNotFound() {
        Blueprint blueprint1 = getBlueprint("One", DEFAULT);
        Blueprint blueprint2 = getBlueprint("Two", DEFAULT);
        when(blueprintRepository.findByNameAndWorkspaceId("Three", 1L)).thenReturn(Optional.empty());
        when(blueprintRepository.findAllByWorkspaceIdAndStatusIn(anyLong(), any())).thenReturn(Set.of(blueprint1));
        when(blueprintLoaderService.isAddingDefaultBlueprintsNecessaryForTheUser(any())).thenReturn(true);
        when(blueprintLoaderService.loadBlueprintsForTheWorkspace(any(), any(), any())).thenReturn(Set.of(blueprint1, blueprint2));

        try {
            underTest.getByNameForWorkspaceAndLoadDefaultsIfNecessary("Three", getWorkspace());
        } catch (NotFoundException e) {
            assertThat(e.getMessage(), containsString("Three"));
        }

        verify(blueprintRepository).findByNameAndWorkspaceId("Three", 1L);
        verify(blueprintRepository).findAllByWorkspaceIdAndStatusIn(anyLong(), any());
        verify(blueprintLoaderService).isAddingDefaultBlueprintsNecessaryForTheUser(any());
        verify(blueprintLoaderService).loadBlueprintsForTheWorkspace(any(), any(), any());
    }

    @Test
    public void testPopulateCrnCorrectly() {
        Blueprint blueprint = new Blueprint();
        underTest.decorateWithCrn(blueprint, ACCOUNT_ID, CREATOR);

        assertThat(blueprint.getCreator(), is(CREATOR));
        assertTrue(blueprint.getResourceCrn().matches("crn:cdp:datahub:us-west-1:" + ACCOUNT_ID + ":clustertemplate:.*"));
    }

    @Test
    public void testGetAllAvailableViewInWorkspaceGivenHasSdxReadyWhenWithSdxReady() {
        Set<BlueprintView> blueprintViews = Set.of(getBlueprintView("bp1", ResourceStatus.DEFAULT, true),
                getBlueprintView("bp2", ResourceStatus.DEFAULT, false),
                getBlueprintView("bp3", ResourceStatus.DEFAULT, false));
        when(blueprintViewRepository.findAllByNotDeletedInWorkspace(1L)).thenReturn(blueprintViews);
        Set<BlueprintView> result = underTest.getAllAvailableViewInWorkspaceAndFilterBySdxReady(1L, true);
        assertEquals(3, result.size());
        assertTrue(result.stream().map(BlueprintView::getName).collect(Collectors.toSet()).containsAll(Set.of("bp1", "bp2", "bp3")));
    }

    @Test
    public void testGetAllAvailableViewInWorkspaceGivenSdxReadyIsNullWhenWithSdxReady() {
        Set<BlueprintView> blueprintViews = Set.of(getBlueprintView("bp1", new Json("{}")),
                getBlueprintView("bp2", ResourceStatus.DEFAULT, false),
                getBlueprintView("bp3", ResourceStatus.DEFAULT, false));
        when(blueprintViewRepository.findAllByNotDeletedInWorkspace(1L)).thenReturn(blueprintViews);
        Set<BlueprintView> result = underTest.getAllAvailableViewInWorkspaceAndFilterBySdxReady(1L, true);
        assertEquals(3, result.size());
    }

    @Test
    public void testGetAllAvailableViewInWorkspaceGivenHasNullOfSdxReadyWhenWithSdxReady() {
        Set<BlueprintView> blueprintViews = Set.of(getBlueprintView("bp1", new Json("{}")),
                getBlueprintView("bp2", ResourceStatus.DEFAULT, true),
                getBlueprintView("bp3", ResourceStatus.DEFAULT, false));
        when(blueprintViewRepository.findAllByNotDeletedInWorkspace(1L)).thenReturn(blueprintViews);
        Set<BlueprintView> result = underTest.getAllAvailableViewInWorkspaceAndFilterBySdxReady(1L, true);
        assertEquals(3, result.size());
        assertTrue(result.stream().map(BlueprintView::getName).collect(Collectors.toSet()).containsAll(Set.of("bp1", "bp2", "bp3")));
    }

    @Test
    public void testGetAllAvailableViewInWorkspaceGivenAttributeIsNullWhenWithSdxReady() {
        Set<BlueprintView> blueprintViews = Set.of(getBlueprintView("bp1", null));
        when(blueprintViewRepository.findAllByNotDeletedInWorkspace(1L)).thenReturn(blueprintViews);
        Set<BlueprintView> result = underTest.getAllAvailableViewInWorkspaceAndFilterBySdxReady(1L, true);
        assertEquals(1, result.size());
        assertTrue(result.stream().map(BlueprintView::getName).collect(Collectors.toSet()).contains("bp1"));
    }

    @Test
    public void testGetAllAvailableViewInWorkspaceGivenAttributeValueIsNullWhenWithSdxReady() {
        Set<BlueprintView> blueprintViews = Set.of(getBlueprintView("bp1", new Json(null)));
        when(blueprintViewRepository.findAllByNotDeletedInWorkspace(1L)).thenReturn(blueprintViews);
        Set<BlueprintView> result = underTest.getAllAvailableViewInWorkspaceAndFilterBySdxReady(1L, true);
        assertEquals(1, result.size());
        assertTrue(result.stream().map(BlueprintView::getName).collect(Collectors.toSet()).contains("bp1"));
    }

    @Test
    // if the tags are null or empty that same as no an sdx ready bp
    public void testGetAllAvailableViewInWorkspaceGivenAttributeValueIsNullWhenWithoutSdxReady() {
        Set<BlueprintView> blueprintViews = Set.of(getBlueprintView("bp1", new Json(null)));
        when(blueprintViewRepository.findAllByNotDeletedInWorkspace(1L)).thenReturn(blueprintViews);
        Set<BlueprintView> result = underTest.getAllAvailableViewInWorkspaceAndFilterBySdxReady(1L, false);
        assertEquals(1, result.size());
        assertTrue(result.stream().map(BlueprintView::getName).collect(Collectors.toSet()).contains("bp1"));
    }

    @Test
    public void testGetAllAvailableViewInWorkspaceGivenHasSdxReadyWhenWithoutSdxReady() {
        Set<BlueprintView> blueprintViews = Set.of(getBlueprintView("bp1", ResourceStatus.DEFAULT, true),
                getBlueprintView("bp2", ResourceStatus.DEFAULT, false),
                getBlueprintView("bp3", ResourceStatus.DEFAULT, false));
        when(blueprintViewRepository.findAllByNotDeletedInWorkspace(1L)).thenReturn(blueprintViews);
        Set<BlueprintView> result = underTest.getAllAvailableViewInWorkspaceAndFilterBySdxReady(1L, false);
        assertEquals(2, result.size());
        assertTrue(result.stream().map(BlueprintView::getName).collect(Collectors.toSet()).containsAll(Set.of("bp2", "bp3")));
    }

    @Test
    // if the tags are null or empty that same as no an sdx ready bp
    public void testGetAllAvailableViewInWorkspaceGivenSdxReadyIsNullWhenWithoutSdxReady() {
        Set<BlueprintView> blueprintViews = Set.of(getBlueprintView("bp1", new Json("{}")),
                getBlueprintView("bp2", ResourceStatus.DEFAULT, false),
                getBlueprintView("bp3", ResourceStatus.DEFAULT, false));
        when(blueprintViewRepository.findAllByNotDeletedInWorkspace(1L)).thenReturn(blueprintViews);
        Set<BlueprintView> result = underTest.getAllAvailableViewInWorkspaceAndFilterBySdxReady(1L, false);
        assertEquals(3, result.size());
        assertTrue(result.stream().map(BlueprintView::getName).collect(Collectors.toSet()).containsAll(Set.of("bp1", "bp2", "bp3")));
    }

    @Test
    // if the tags are null or empty that same as no an sdx ready bp
    public void testGetAllAvailableViewInWorkspaceGivenHasNullOfSdxReadyWhenWithoutSdxReady() {
        Set<BlueprintView> blueprintViews = Set.of(getBlueprintView("bp1", new Json("{}")),
                getBlueprintView("bp2", ResourceStatus.DEFAULT, true),
                getBlueprintView("bp3", ResourceStatus.DEFAULT, false));
        when(blueprintViewRepository.findAllByNotDeletedInWorkspace(1L)).thenReturn(blueprintViews);
        Set<BlueprintView> result = underTest.getAllAvailableViewInWorkspaceAndFilterBySdxReady(1L, false);
        assertEquals(2, result.size());
        assertTrue(result.stream().map(BlueprintView::getName).collect(Collectors.toSet()).containsAll(Set.of("bp1", "bp3")));
    }

    @Test
    // if the tags are null or empty that same as no an sdx ready bp
    public void testGetAllAvailableViewInWorkspaceGivenAttributeIsNullWhenWithoutSdxReady() {
        Set<BlueprintView> blueprintViews = Set.of(getBlueprintView("bp1", null));
        when(blueprintViewRepository.findAllByNotDeletedInWorkspace(1L)).thenReturn(blueprintViews);
        Set<BlueprintView> result = underTest.getAllAvailableViewInWorkspaceAndFilterBySdxReady(1L, false);
        assertEquals(1, result.size());
        assertTrue(result.stream().map(BlueprintView::getName).collect(Collectors.toSet()).contains("bp1"));
    }

    @Test
    public void testBadRequestExceptionRaisedWhenValidationIsFailing() {
        String someBlueprintText = "someText";

        doAnswer(invocation -> {
            invocation.getArgument(1, Errors.class).reject("test");
            return null;
        }).when(blueprintValidator).validate(any(), any());
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(someBlueprintText);

        Assertions.assertThrows(BadRequestException.class, () -> underTest.createForLoggedInUser(blueprint, 1L, "someAccountId", "someone"));
    }

    @Test
    public void testBadRequestExceptionRaisedWhenValidationIsFailingOnCreateWithInternalUser() {
        String someBlueprintText = "someText";

        doAnswer(invocation -> {
            invocation.getArgument(1, Errors.class).reject("test");
            return null;
        }).when(blueprintValidator).validate(any(), any());
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(someBlueprintText);

        Assertions.assertThrows(BadRequestException.class, () -> underTest.createWithInternalUser(blueprint, 1L, "someAccountId"));
    }

    @Test
    public void testCreateWithInternalUser() throws TransactionExecutionException {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText("{}");

        doAnswer(invocation -> ((Supplier<Blueprint>) invocation.getArgument(0)).get())
                .when(transactionService).required(any(Supplier.class));
        Workspace workspace = new Workspace();
        workspace.setId(1L);
        when(workspaceService.getByIdWithoutAuth(1L)).thenReturn(workspace);
        when(blueprintRepository.save(blueprint)).thenReturn(blueprint);

        Blueprint savedBlueprint = underTest.createWithInternalUser(blueprint, 1L, ACCOUNT_ID);

        assertEquals(workspace, savedBlueprint.getWorkspace());
        assertTrue(blueprint.getResourceCrn().matches("crn:cdp:datahub:us-west-1:" + ACCOUNT_ID + ":clustertemplate:.*"));
        assertEquals(ResourceStatus.SERVICE_MANAGED, savedBlueprint.getStatus());
    }

    @Test
    public void testPrepareDeletionWhenHasOneClusterDefinitionAndOneCluster() {
        Blueprint blueprint = new Blueprint();
        blueprint.setName("TemplateName");
        Cluster templateCluster = getCluster("Stack Template Name", 0L, blueprint, DetailedStackStatus.AVAILABLE);
        templateCluster.getStack().setType(StackType.TEMPLATE);
        ClusterTemplateView clusterTemplateView = new ClusterTemplateView();
        clusterTemplateView.setName("ClusterDefinition");

        Cluster workloadCluster = getCluster("Workload Name", 1L, blueprint, DetailedStackStatus.AVAILABLE);

        when(clusterTemplateViewService.findAllByStackIds(List.of(0L))).thenReturn(Set.of(clusterTemplateView));
        when(clusterService.findByBlueprint(blueprint)).thenReturn(Set.of(workloadCluster, templateCluster));
        BadRequestException actual = Assertions.assertThrows(BadRequestException.class, () -> underTest.prepareDeletion(blueprint));
        Assertions.assertEquals("There are clusters or cluster definitions associated with cluster template 'TemplateName'. "
                + "The cluster template used by 1 cluster(s) (Workload Name) and 1 cluster definitions (ClusterDefinition). "
                + "Please remove these before deleting the cluster template.", actual.getMessage());
    }

    @Test
    public void testPrepareDeletionWhenHasOneClusterDefinition() {
        Blueprint blueprint = new Blueprint();
        blueprint.setName("TemplateName");
        Cluster templateCluster = getCluster("Cluster Name", 0L, blueprint, DetailedStackStatus.AVAILABLE);
        templateCluster.getStack().setType(StackType.TEMPLATE);
        ClusterTemplateView clusterTemplateView = new ClusterTemplateView();
        clusterTemplateView.setName("ClusterDefinition");
        when(clusterService.findByBlueprint(blueprint)).thenReturn(Set.of(templateCluster));
        when(clusterTemplateViewService.findAllByStackIds(any())).thenReturn(Set.of(clusterTemplateView));
        BadRequestException actual = Assertions.assertThrows(BadRequestException.class, () -> underTest.prepareDeletion(blueprint));
        Assertions.assertEquals("There is a cluster definition ['ClusterDefinition'] which uses cluster template 'TemplateName'. "
                + "Please remove this cluster before deleting the cluster template.", actual.getMessage());
    }

    @Test
    public void testPrepareDeletionWhenHasOneCluster() {
        Blueprint blueprint = new Blueprint();
        blueprint.setName("TemplateName");
        Cluster templateCluster = getCluster("Cluster Name", 0L, blueprint, DetailedStackStatus.AVAILABLE);
        ClusterTemplateView clusterTemplateView = new ClusterTemplateView();
        clusterTemplateView.setName("ClusterDefinition");
        when(clusterService.findByBlueprint(blueprint)).thenReturn(Set.of(templateCluster));
        when(clusterTemplateViewService.findAllByStackIds(any())).thenReturn(Set.of(clusterTemplateView));
        BadRequestException actual = Assertions.assertThrows(BadRequestException.class, () -> underTest.prepareDeletion(blueprint));
        Assertions.assertEquals("There is a cluster ['ClusterDefinition'] which uses cluster template 'TemplateName'. "
                + "Please remove this cluster before deleting the cluster template.", actual.getMessage());
    }

    private Cluster getCluster(String name, Long id, Blueprint blueprint, DetailedStackStatus detailedStackStatus) {
        Cluster cluster1 = new Cluster();
        cluster1.setName(name);
        cluster1.setId(id);
        cluster1.setBlueprint(blueprint);
        patchWithStackStatus(cluster1, detailedStackStatus);
        return cluster1;
    }

    private void patchWithStackStatus(Cluster cluster1, DetailedStackStatus detailedStackStatus) {
        Stack stack = new Stack();
        stack.setStackStatus(new StackStatus(stack, detailedStackStatus));
        stack.setType(StackType.WORKLOAD);
        stack.setName(cluster1.getName());
        stack.setId(cluster1.getId());
        cluster1.setStack(stack);
    }

    private Blueprint getBlueprint(String name, ResourceStatus status) {
        Blueprint blueprint = new Blueprint();
        blueprint.setName(name);
        blueprint.setWorkspace(getWorkspace());
        blueprint.setStatus(status);
        blueprint.setCreator(CREATOR);
        blueprint.setResourceCrn("someCrn");
        return blueprint;
    }

    private BlueprintView getBlueprintView(String name, ResourceStatus status, Boolean sdxReady) {
        BlueprintView blueprint = new BlueprintView();
        blueprint.setName(name);
        blueprint.setWorkspace(getWorkspace());
        blueprint.setStatus(status);
        blueprint.setResourceCrn("someCrn");
        if (sdxReady != null) {
            blueprint.setTags(Json.silent(Map.of("shared_services_ready", sdxReady)));
        }
        return blueprint;
    }

    private BlueprintView getBlueprintView(String name, Json attribute) {
        BlueprintView blueprint = new BlueprintView();
        blueprint.setName(name);
        blueprint.setWorkspace(getWorkspace());
        blueprint.setStatus(DEFAULT);
        blueprint.setResourceCrn("someCrn");
        blueprint.setTags(attribute);
        return blueprint;
    }

    private Workspace getWorkspace() {
        Workspace workspace = new Workspace();
        workspace.setId(1L);
        return workspace;
    }
}

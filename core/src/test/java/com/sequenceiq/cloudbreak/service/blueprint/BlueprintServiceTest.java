package com.sequenceiq.cloudbreak.service.blueprint;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.DEFAULT;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.SERVICE_MANAGED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.USER_MANAGED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.Errors;

import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.BlueprintFile;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.BlueprintClusterView;
import com.sequenceiq.cloudbreak.domain.view.BlueprintView;
import com.sequenceiq.cloudbreak.domain.view.ClusterTemplateView;
import com.sequenceiq.cloudbreak.init.blueprint.BlueprintLoaderService;
import com.sequenceiq.cloudbreak.init.blueprint.DefaultBlueprintCache;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.repository.BlueprintViewRepository;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.runtimes.SupportedRuntimes;
import com.sequenceiq.cloudbreak.service.template.ClusterTemplateViewService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.LegacyRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.template.BlueprintProcessingException;
import com.sequenceiq.cloudbreak.validation.HueWorkaroundValidatorService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@ExtendWith(MockitoExtension.class)
class BlueprintServiceTest {
    private static final String ACCOUNT_ID = "ACCOUNT_ID";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + ACCOUNT_ID + ":user:accountId";

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

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Mock
    private CrnGeneratorService crnGeneratorService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private DefaultBlueprintCache defaultBlueprintCache;

    @InjectMocks
    private BlueprintService underTest;

    @BeforeEach
    void setup() {
        CrnTestUtil.mockCrnGenerator(regionAwareCrnGenerator);
        SupportedRuntimes supportedRuntimes = new SupportedRuntimes();
        ReflectionTestUtils.setField(blueprintListFilters, "supportedRuntimes", supportedRuntimes);
        ReflectionTestUtils.setField(supportedRuntimes, "imageCatalogService", imageCatalogService);
        lenient().when(legacyRestRequestThreadLocalService.getCloudbreakUser()).thenReturn(cloudbreakUser);
        lenient().when(userService.getOrCreate(cloudbreakUser)).thenReturn(user);
        lenient().when(workspaceService.get(1L, user)).thenReturn(getWorkspace());
        lenient().doNothing().when(ownerAssignmentService).notifyResourceDeleted(anyString());
    }

    @Test
    void testDeleteByWorkspaceWhenDtoNameFilledThenDeleteCalled() {
        Blueprint blueprint = getBlueprint("name", USER_MANAGED);
        when(blueprintRepository.findByNameAndWorkspaceId(blueprint.getName(),
                blueprint.getWorkspace().getId())).thenReturn(Optional.of(blueprint));

        Blueprint result = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.deleteByWorkspace(NameOrCrn.ofName(blueprint.getName()), blueprint.getWorkspace().getId()));

        assertEquals(blueprint, result);
        verify(blueprintRepository, times(1))
                .findByNameAndWorkspaceId(anyString(), anyLong());
        verify(blueprintRepository, times(1))
                .findByNameAndWorkspaceId(blueprint.getName(), blueprint.getWorkspace().getId());
        verify(blueprintRepository, times(1)).delete(any(Blueprint.class));
        verify(blueprintRepository, times(1)).delete(blueprint);
    }

    @Test
    void testDeleteServiceManaged() {
        Blueprint blueprint = getBlueprint("name", SERVICE_MANAGED);
        when(blueprintRepository.findByNameAndWorkspaceId(blueprint.getName(),
                blueprint.getWorkspace().getId())).thenReturn(Optional.of(blueprint));

        Blueprint result = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.deleteByWorkspace(NameOrCrn.ofName(blueprint.getName()), blueprint.getWorkspace().getId()));

        assertEquals(blueprint, result);
        verify(blueprintRepository, times(1))
                .findByNameAndWorkspaceId(anyString(), anyLong());
        verify(blueprintRepository, times(1))
                .findByNameAndWorkspaceId(blueprint.getName(), blueprint.getWorkspace().getId());
        verify(blueprintRepository, times(1)).delete(any(Blueprint.class));
        verify(blueprintRepository, times(1)).delete(blueprint);
    }

    @Test
    void testDeleteByWorkspaceWhenDtoCrnFilledThenDeleteCalled() {
        Blueprint blueprint = getBlueprint("name", USER_MANAGED);
        when(blueprintRepository.findByResourceCrnAndWorkspaceId(blueprint.getResourceCrn(),
                blueprint.getWorkspace().getId())).thenReturn(Optional.of(blueprint));

        Blueprint result = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.deleteByWorkspace(NameOrCrn.ofCrn(blueprint.getResourceCrn()), blueprint.getWorkspace().getId()));

        assertEquals(blueprint, result);
        verify(blueprintRepository, times(1))
                .findByResourceCrnAndWorkspaceId(anyString(), anyLong());
        verify(blueprintRepository, times(1))
                .findByResourceCrnAndWorkspaceId(blueprint.getResourceCrn(), blueprint.getWorkspace().getId());
        verify(blueprintRepository, times(1)).delete(any(Blueprint.class));
        verify(blueprintRepository, times(1)).delete(blueprint);
    }

    @Test
    void testGetByWorkspaceWhenDtoNameFilledThenProperGetCalled() {
        Blueprint blueprint = getBlueprint("name", USER_MANAGED);
        when(blueprintRepository.findByNameAndWorkspaceId(blueprint.getName(),
                blueprint.getWorkspace().getId())).thenReturn(Optional.of(blueprint));

        Blueprint result = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getByWorkspace(NameOrCrn.ofName(blueprint.getName()), blueprint.getWorkspace().getId()));

        assertEquals(blueprint, result);
        verify(blueprintRepository, times(1))
                .findByNameAndWorkspaceId(anyString(), anyLong());
        verify(blueprintRepository, times(1))
                .findByNameAndWorkspaceId(blueprint.getName(), blueprint.getWorkspace().getId());
    }

    @Test
    void testGetByWorkspaceWhenDtoCrnFilledThenProperGetCalled() {
        Blueprint blueprint = getBlueprint("name", USER_MANAGED);
        when(blueprintRepository.findByResourceCrnAndWorkspaceId(blueprint.getResourceCrn(),
                blueprint.getWorkspace().getId())).thenReturn(Optional.of(blueprint));

        Blueprint result = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getByWorkspace(NameOrCrn.ofCrn(blueprint.getResourceCrn()), blueprint.getWorkspace().getId()));

        assertEquals(blueprint, result);
        verify(blueprintRepository, times(1))
                .findByResourceCrnAndWorkspaceId(anyString(), anyLong());
        verify(blueprintRepository, times(1))
                .findByResourceCrnAndWorkspaceId(blueprint.getResourceCrn(), blueprint.getWorkspace().getId());
    }

    @Test
    void testDeletionWithZeroClusters() {
        Blueprint blueprint = getBlueprint("name", USER_MANAGED);
        blueprint.setId(1L);
        doNothing().when(clusterService).deleteBlueprintsOnSpecificClusters(anyLong());
        when(clusterService.findByStackResourceCrn(anyLong())).thenReturn(new HashSet<>());

        Blueprint deleted = underTest.delete(blueprint);

        assertNotNull(deleted);
    }

    @Test
    void testDeletionWithNonTerminatedClusterAndStack() {
        Blueprint blueprint = getBlueprint("name", USER_MANAGED);
        blueprint.setId(1L);
        BlueprintClusterView blueprintClusterView = new BlueprintClusterView();
        blueprintClusterView.setId(1L);
        blueprintClusterView.setType(StackType.WORKLOAD);
        blueprintClusterView.setName("c1");
        doNothing().when(clusterService).deleteBlueprintsOnSpecificClusters(anyLong());
        when(clusterService.findByStackResourceCrn(anyLong())).thenReturn(Set.of(blueprintClusterView));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.delete(blueprint));
        assertEquals("The cluster with name ['c1'] uses cluster template 'name'. "
                        + "Please remove the cluster before deleting the cluster template.",
                exception.getMessage());
    }

    @Test
    void testDeletionWithTerminatedClustersNonTerminatedStacks() {
        Blueprint blueprint = getBlueprint("name", USER_MANAGED);
        blueprint.setId(1L);
        BlueprintClusterView blueprintClusterView = new BlueprintClusterView();
        blueprintClusterView.setId(1L);
        blueprintClusterView.setType(StackType.WORKLOAD);
        blueprintClusterView.setName("c1");

        doNothing().when(clusterService).deleteBlueprintsOnSpecificClusters(anyLong());
        when(clusterService.findByStackResourceCrn(anyLong())).thenReturn(Set.of(blueprintClusterView));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.delete(blueprint));
        assertEquals("The cluster with name ['c1'] uses cluster template 'name'. "
                        + "Please remove the cluster before deleting the cluster template.",
                exception.getMessage());
    }

    @Test
    void testDeletionWithTerminatedAndNonTerminatedClusters() {
        Blueprint blueprint = getBlueprint("name", USER_MANAGED);
        blueprint.setId(1L);
        BlueprintClusterView blueprintClusterView = new BlueprintClusterView();
        blueprintClusterView.setId(1L);
        blueprintClusterView.setType(StackType.WORKLOAD);
        blueprintClusterView.setName("c1");

        doNothing().when(clusterService).deleteBlueprintsOnSpecificClusters(anyLong());
        when(clusterService.findByStackResourceCrn(anyLong())).thenReturn(Set.of(blueprintClusterView));

        try {
            underTest.delete(blueprint);
        } catch (BadRequestException e) {
            assertEquals("The cluster with name ['c1'] uses cluster template 'name'. "
                    + "Please remove the cluster before deleting the cluster template.", e.getMessage());
        }
    }

    @Test
    void testGetByNameForWorkspaceAndLoadDefaultsIfNecessaryWhenFound() {
        Blueprint blueprint1 = getBlueprint("One", DEFAULT);
        when(blueprintRepository.findByNameAndWorkspaceId("One", 1L)).thenReturn(Optional.of(blueprint1));

        Blueprint foundBlueprint = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getByNameForWorkspaceAndLoadDefaultsIfNecessary("One", getWorkspace()));

        assertEquals("One", foundBlueprint.getName());
        verify(blueprintRepository).findByNameAndWorkspaceId("One", 1L);
        verify(blueprintRepository, never()).findAllByWorkspaceIdAndStatusIn(anyLong(), any());
        verify(blueprintLoaderService, never()).isAddingDefaultBlueprintsNecessaryForTheUser(any());
    }

    @Test
    void testGetByNameForWorkspaceAndLoadDefaultsIfNecessaryWhenLoaded() {
        Blueprint blueprint1 = getBlueprint("One", DEFAULT);
        Blueprint blueprint2 = getBlueprint("Two", DEFAULT);
        when(blueprintRepository.findByNameAndWorkspaceId("Two", 1L))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(blueprint2));
        when(blueprintRepository.findAllByWorkspaceIdAndStatusIn(anyLong(), any())).thenReturn(Set.of(blueprint1));
        when(blueprintLoaderService.isAddingDefaultBlueprintsNecessaryForTheUser(any())).thenReturn(true);
        when(blueprintLoaderService.loadBlueprintsForTheWorkspace(any(), any(), any(), anyBoolean())).thenReturn(Set.of(blueprint1, blueprint2));

        Blueprint foundBlueprint = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getByNameForWorkspaceAndLoadDefaultsIfNecessary("Two", getWorkspace()));

        assertEquals("Two", foundBlueprint.getName());
        verify(blueprintRepository, times(2)).findByNameAndWorkspaceId("Two", 1L);
        verify(blueprintRepository).findAllByWorkspaceIdAndStatusIn(anyLong(), any());
        verify(blueprintLoaderService).isAddingDefaultBlueprintsNecessaryForTheUser(any());
        verify(blueprintLoaderService).loadBlueprintsForTheWorkspace(any(), any(), any(), anyBoolean());
    }

    @Test
    void testGetByNameForWorkspaceAndLoadDefaultsIfNecessaryWhenNotFound() {
        Blueprint blueprint1 = getBlueprint("One", DEFAULT);
        Blueprint blueprint2 = getBlueprint("Two", DEFAULT);
        when(blueprintRepository.findByNameAndWorkspaceId("Three", 1L)).thenReturn(Optional.empty());
        when(blueprintRepository.findAllByWorkspaceIdAndStatusIn(anyLong(), any())).thenReturn(Set.of(blueprint1));
        when(blueprintLoaderService.isAddingDefaultBlueprintsNecessaryForTheUser(any())).thenReturn(true);
        when(blueprintLoaderService.loadBlueprintsForTheWorkspace(any(), any(), any(), anyBoolean())).thenReturn(Set.of(blueprint1, blueprint2));

        try {
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.getByNameForWorkspaceAndLoadDefaultsIfNecessary("Three", getWorkspace()));
        } catch (NotFoundException e) {
            assertThat(e.getMessage()).containsSequence("Three");
        }

        verify(blueprintRepository, times(2)).findByNameAndWorkspaceId("Three", 1L);
        verify(blueprintRepository).findAllByWorkspaceIdAndStatusIn(anyLong(), any());
        verify(blueprintLoaderService).isAddingDefaultBlueprintsNecessaryForTheUser(any());
        verify(blueprintLoaderService).loadBlueprintsForTheWorkspace(any(), any(), any(), anyBoolean());
    }

    @Test
    void testGetAllAvailableViewInWorkspaceGivenHasSdxReadyWhenWithSdxReady() {
        Set<BlueprintView> blueprintViews = Set.of(getBlueprintView("bp1", DEFAULT, true),
                getBlueprintView("bp2", DEFAULT, false),
                getBlueprintView("bp3", DEFAULT, false));
        when(blueprintViewRepository.findAllByNotDeletedInWorkspace(1L)).thenReturn(blueprintViews);
        Set<BlueprintView> result = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getAllAvailableViewInWorkspaceAndFilterBySdxReady(1L, true));
        assertEquals(3, result.size());
        assertTrue(result.stream().map(BlueprintView::getName).collect(Collectors.toSet()).containsAll(Set.of("bp1", "bp2", "bp3")));
        assertEquals(123456789L, result.stream().map(BlueprintView::getLastUpdated).findFirst().get());
    }

    @Test
    void testGetAllAvailableViewInWorkspaceGivenSdxReadyIsNullWhenWithSdxReady() {
        Set<BlueprintView> blueprintViews = Set.of(getBlueprintView("bp1", new Json("{}")),
                getBlueprintView("bp2", DEFAULT, false),
                getBlueprintView("bp3", DEFAULT, false));
        when(blueprintViewRepository.findAllByNotDeletedInWorkspace(1L)).thenReturn(blueprintViews);
        Set<BlueprintView> result = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getAllAvailableViewInWorkspaceAndFilterBySdxReady(1L, true));
        assertEquals(3, result.size());
    }

    @Test
    void testGetAllAvailableViewInWorkspaceGivenHasNullOfSdxReadyWhenWithSdxReady() {
        Set<BlueprintView> blueprintViews = Set.of(getBlueprintView("bp1", new Json("{}")),
                getBlueprintView("bp2", DEFAULT, true),
                getBlueprintView("bp3", DEFAULT, false));
        when(blueprintViewRepository.findAllByNotDeletedInWorkspace(1L)).thenReturn(blueprintViews);
        Set<BlueprintView> result = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getAllAvailableViewInWorkspaceAndFilterBySdxReady(1L, true));
        assertEquals(3, result.size());
        assertTrue(result.stream().map(BlueprintView::getName).collect(Collectors.toSet()).containsAll(Set.of("bp1", "bp2", "bp3")));
    }

    @Test
    void testGetAllAvailableViewInWorkspaceGivenAttributeIsNullWhenWithSdxReady() {
        Set<BlueprintView> blueprintViews = Set.of(getBlueprintView("bp1", null));
        when(blueprintViewRepository.findAllByNotDeletedInWorkspace(1L)).thenReturn(blueprintViews);
        Set<BlueprintView> result = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getAllAvailableViewInWorkspaceAndFilterBySdxReady(1L, true));
        assertEquals(1, result.size());
        assertTrue(result.stream().map(BlueprintView::getName).collect(Collectors.toSet()).contains("bp1"));
    }

    @Test
    void testGetAllAvailableViewInWorkspaceGivenAttributeValueIsNullWhenWithSdxReady() {
        Set<BlueprintView> blueprintViews = Set.of(getBlueprintView("bp1", new Json(null)));
        when(blueprintViewRepository.findAllByNotDeletedInWorkspace(1L)).thenReturn(blueprintViews);
        Set<BlueprintView> result = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getAllAvailableViewInWorkspaceAndFilterBySdxReady(1L, true));
        assertEquals(1, result.size());
        assertTrue(result.stream().map(BlueprintView::getName).collect(Collectors.toSet()).contains("bp1"));
    }

    @Test
        // if the tags are null or empty that same as no an sdx ready bp
    void testGetAllAvailableViewInWorkspaceGivenAttributeValueIsNullWhenWithoutSdxReady() {
        Set<BlueprintView> blueprintViews = Set.of(getBlueprintView("bp1", new Json(null)));
        when(blueprintViewRepository.findAllByNotDeletedInWorkspace(1L)).thenReturn(blueprintViews);
        Set<BlueprintView> result = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getAllAvailableViewInWorkspaceAndFilterBySdxReady(1L, false));
        assertEquals(1, result.size());
        assertTrue(result.stream().map(BlueprintView::getName).collect(Collectors.toSet()).contains("bp1"));
    }

    @Test
    void testGetAllAvailableViewInWorkspaceGivenHasSdxReadyWhenWithoutSdxReady() {
        Set<BlueprintView> blueprintViews = Set.of(getBlueprintView("bp1", DEFAULT, true),
                getBlueprintView("bp2", DEFAULT, false),
                getBlueprintView("bp3", DEFAULT, false));
        when(blueprintViewRepository.findAllByNotDeletedInWorkspace(1L)).thenReturn(blueprintViews);
        Set<BlueprintView> result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.getAllAvailableViewInWorkspaceAndFilterBySdxReady(1L, false));
        assertEquals(2, result.size());
        assertTrue(result.stream().map(BlueprintView::getName).collect(Collectors.toSet()).containsAll(Set.of("bp2", "bp3")));
    }

    @Test
        // if the tags are null or empty that same as no an sdx ready bp
    void testGetAllAvailableViewInWorkspaceGivenSdxReadyIsNullWhenWithoutSdxReady() {
        Set<BlueprintView> blueprintViews = Set.of(getBlueprintView("bp1", new Json("{}")),
                getBlueprintView("bp2", DEFAULT, false),
                getBlueprintView("bp3", DEFAULT, false));
        when(blueprintViewRepository.findAllByNotDeletedInWorkspace(1L)).thenReturn(blueprintViews);
        Set<BlueprintView> result = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getAllAvailableViewInWorkspaceAndFilterBySdxReady(1L, false));
        assertEquals(3, result.size());
        assertTrue(result.stream().map(BlueprintView::getName).collect(Collectors.toSet()).containsAll(Set.of("bp1", "bp2", "bp3")));
    }

    @Test
        // if the tags are null or empty that same as no an sdx ready bp
    void testGetAllAvailableViewInWorkspaceGivenHasNullOfSdxReadyWhenWithoutSdxReady() {
        Set<BlueprintView> blueprintViews = Set.of(getBlueprintView("bp1", new Json("{}")),
                getBlueprintView("bp2", DEFAULT, true),
                getBlueprintView("bp3", DEFAULT, false));
        when(blueprintViewRepository.findAllByNotDeletedInWorkspace(1L)).thenReturn(blueprintViews);
        Set<BlueprintView> result = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getAllAvailableViewInWorkspaceAndFilterBySdxReady(1L, false));
        assertEquals(2, result.size());
        assertTrue(result.stream().map(BlueprintView::getName).collect(Collectors.toSet()).containsAll(Set.of("bp1", "bp3")));
    }

    @Test
        // if the tags are null or empty that same as no an sdx ready bp
    void testGetAllAvailableViewInWorkspaceGivenAttributeIsNullWhenWithoutSdxReady() {
        Set<BlueprintView> blueprintViews = Set.of(getBlueprintView("bp1", null));
        when(blueprintViewRepository.findAllByNotDeletedInWorkspace(1L)).thenReturn(blueprintViews);
        Set<BlueprintView> result = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getAllAvailableViewInWorkspaceAndFilterBySdxReady(1L, false));
        assertEquals(1, result.size());
        assertTrue(result.stream().map(BlueprintView::getName).collect(Collectors.toSet()).contains("bp1"));
    }

    @Test
    void testBadRequestExceptionRaisedWhenValidationIsFailing() {
        String someBlueprintText = "someText";

        doAnswer(invocation -> {
            invocation.getArgument(1, Errors.class).reject("test");
            return null;
        }).when(blueprintValidator).validate(any(), any());
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(someBlueprintText);

        assertThrows(BadRequestException.class, () -> underTest.createForLoggedInUser(blueprint, 1L, "someAccountId", "someone"));
    }

    @Test
    void testBadRequestExceptionRaisedWhenValidationIsFailingOnCreateWithInternalUser() {
        String someBlueprintText = "someText";

        doAnswer(invocation -> {
            invocation.getArgument(1, Errors.class).reject("test");
            return null;
        }).when(blueprintValidator).validate(any(), any());
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(someBlueprintText);

        assertThrows(BadRequestException.class, () -> underTest.createWithInternalUser(blueprint, 1L, "someAccountId"));
    }

    @Test
    void testCreateWithInternalUser() throws TransactionExecutionException {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText("{}");

        doAnswer(invocation -> ((Supplier<Blueprint>) invocation.getArgument(0)).get())
                .when(transactionService).required(any(Supplier.class));
        Workspace workspace = new Workspace();
        workspace.setId(1L);
        when(workspaceService.getByIdWithoutAuth(1L)).thenReturn(workspace);
        when(blueprintRepository.save(blueprint)).thenReturn(blueprint);
        when(crnGeneratorService.createBlueprintCrn(ACCOUNT_ID)).thenReturn("resourceCrn");

        Blueprint savedBlueprint = underTest.createWithInternalUser(blueprint, 1L, ACCOUNT_ID);

        assertEquals(workspace, savedBlueprint.getWorkspace());
        assertEquals("resourceCrn", blueprint.getResourceCrn());
        assertEquals(SERVICE_MANAGED, savedBlueprint.getStatus());
    }

    @Test
    void testPrepareDeletionWhenHasOneClusterDefinitionAndOneCluster() {
        Blueprint blueprint = new Blueprint();
        blueprint.setName("TemplateName");
        blueprint.setId(1L);
        Cluster templateCluster = getCluster("Stack Template Name", 0L, blueprint, DetailedStackStatus.AVAILABLE);
        templateCluster.getStack().setType(StackType.TEMPLATE);
        ClusterTemplateView clusterTemplateView = new ClusterTemplateView();
        clusterTemplateView.setName("ClusterDefinition");
        BlueprintClusterView blueprintClusterView1 = new BlueprintClusterView();
        blueprintClusterView1.setId(1L);
        blueprintClusterView1.setType(StackType.WORKLOAD);
        blueprintClusterView1.setName("Workload Name");
        BlueprintClusterView blueprintClusterView2 = new BlueprintClusterView();
        blueprintClusterView2.setId(0L);
        blueprintClusterView2.setType(StackType.TEMPLATE);
        blueprintClusterView2.setName("ClusterDefinition");

        when(clusterTemplateViewService.findAllByStackIds(List.of(0L))).thenReturn(Set.of(clusterTemplateView));
        doNothing().when(clusterService).deleteBlueprintsOnSpecificClusters(anyLong());
        when(clusterService.findByStackResourceCrn(anyLong())).thenReturn(Set.of(blueprintClusterView1, blueprintClusterView2));
        BadRequestException actual = assertThrows(BadRequestException.class, () -> underTest.prepareDeletion(blueprint));
        assertEquals("There are clusters or cluster definitions associated with cluster template 'TemplateName'. "
                + "The cluster template used by 1 cluster(s) (Workload Name) and 1 cluster definitions (ClusterDefinition). "
                + "Please remove these before deleting the cluster template.", actual.getMessage());
    }

    @Test
    void testPrepareDeletionWhenHasOneClusterDefinition() {
        Blueprint blueprint = new Blueprint();
        blueprint.setName("TemplateName");
        blueprint.setId(1L);
        Cluster templateCluster = getCluster("Cluster Name", 0L, blueprint, DetailedStackStatus.AVAILABLE);
        templateCluster.getStack().setType(StackType.TEMPLATE);
        ClusterTemplateView clusterTemplateView = new ClusterTemplateView();
        clusterTemplateView.setName("ClusterDefinition");
        BlueprintClusterView blueprintClusterView = new BlueprintClusterView();
        blueprintClusterView.setId(1L);
        blueprintClusterView.setType(StackType.TEMPLATE);
        blueprintClusterView.setName("ClusterDefinition");
        doNothing().when(clusterService).deleteBlueprintsOnSpecificClusters(anyLong());
        when(clusterService.findByStackResourceCrn(anyLong())).thenReturn(Set.of(blueprintClusterView));
        BadRequestException actual = assertThrows(BadRequestException.class, () -> underTest.prepareDeletion(blueprint));
        assertEquals("The cluster definition with name ['ClusterDefinition'] uses cluster template 'TemplateName'. "
                + "Please remove the cluster definition before deleting the cluster template.", actual.getMessage());
    }

    @Test
    void testPrepareDeletionWhenHasOneCluster() {
        Blueprint blueprint = new Blueprint();
        blueprint.setName("TemplateName");
        blueprint.setId(1L);
        BlueprintClusterView blueprintClusterView = new BlueprintClusterView();
        blueprintClusterView.setId(1L);
        blueprintClusterView.setType(StackType.WORKLOAD);
        blueprintClusterView.setName("Cluster Name");
        doNothing().when(clusterService).deleteBlueprintsOnSpecificClusters(anyLong());
        when(clusterService.findByStackResourceCrn(anyLong())).thenReturn(Set.of(blueprintClusterView));
        BadRequestException actual = assertThrows(BadRequestException.class, () -> underTest.prepareDeletion(blueprint));
        assertEquals("The cluster with name ['Cluster Name'] uses cluster template 'TemplateName'. "
                + "Please remove the cluster before deleting the cluster template.", actual.getMessage());
    }

    @Test
    void testAnyOfTheServiceTypesPresentOnBlueprint() {
        when(cmTemplateProcessorFactory.get("blueprint1")).thenReturn(mock());
        CmTemplateProcessor cmTemplateProcessor2 = mock();
        when(cmTemplateProcessor2.isServiceTypePresent("HIVE")).thenReturn(false);
        when(cmTemplateProcessor2.isServiceTypePresent("HBASE")).thenReturn(true);
        when(cmTemplateProcessorFactory.get("blueprint2")).thenReturn(cmTemplateProcessor2);
        when(cmTemplateProcessorFactory.get("blueprint3")).thenThrow(BlueprintProcessingException.class);


        assertFalse(underTest.anyOfTheServiceTypesPresentOnBlueprint("blueprint1", List.of("HIVE", "HBASE")));
        assertTrue(underTest.anyOfTheServiceTypesPresentOnBlueprint("blueprint2", List.of("HIVE", "HBASE")));
        assertFalse(underTest.anyOfTheServiceTypesPresentOnBlueprint("blueprint3", List.of("HIVE", "HBASE")));
    }

    @Test
    void testGetByWorkspaceByNameWhenGlobalDefaultEnabledAndDefaultBlueprint() {
        Blueprint blueprint = getBlueprint("default-bp", DEFAULT);
        when(entitlementService.isGlobalDefaultTemplateEnabled(ACCOUNT_ID)).thenReturn(true);
        when(defaultBlueprintCache.isDefaultByName("default-bp")).thenReturn(true);
        when(blueprintRepository.findGlobalDefaultByName("default-bp")).thenReturn(Optional.of(blueprint));

        Blueprint result = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getByWorkspace(NameOrCrn.ofName("default-bp"), 1L));

        assertEquals(blueprint, result);
        verify(blueprintRepository).findGlobalDefaultByName("default-bp");
        verify(blueprintRepository, never()).findByNameAndWorkspaceId(anyString(), anyLong());
    }

    @Test
    void testGetByWorkspaceByCrnWhenGlobalDefaultEnabledAndDefaultBlueprint() {
        Blueprint blueprint = getBlueprint("default-bp", DEFAULT);
        blueprint.setResourceCrn("crn:default-bp");
        when(entitlementService.isGlobalDefaultTemplateEnabled(ACCOUNT_ID)).thenReturn(true);
        when(defaultBlueprintCache.isDefaultByCrn("crn:default-bp")).thenReturn(true);
        when(blueprintRepository.findGlobalDefaultByResourceCrn("crn:default-bp")).thenReturn(Optional.of(blueprint));

        Blueprint result = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getByWorkspace(NameOrCrn.ofCrn("crn:default-bp"), 1L));

        assertEquals(blueprint, result);
        verify(blueprintRepository).findGlobalDefaultByResourceCrn("crn:default-bp");
        verify(blueprintRepository, never()).findByResourceCrnAndWorkspaceId(anyString(), anyLong());
    }

    @Test
    void testGetAllAvailableViewInWorkspaceAndFilterBySdxReadyWhenGlobalDefaultEnabled() {
        BlueprintView globalView = getBlueprintView("global-view", DEFAULT, true);
        BlueprintView workspaceView = getBlueprintView("workspace-view", USER_MANAGED, false);
        BlueprintView workspaceDefaultView = getBlueprintView("workspace-default-view", DEFAULT, false);

        when(blueprintViewRepository.findAllByNotDeletedInWorkspace(1L)).thenReturn(Set.of(workspaceView, workspaceDefaultView));
        when(entitlementService.isGlobalDefaultTemplateEnabled(ACCOUNT_ID)).thenReturn(true);
        when(blueprintViewRepository.findAllGlobalDefaults()).thenReturn(Set.of(globalView));

        Set<BlueprintView> result = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getAllAvailableViewInWorkspaceAndFilterBySdxReady(1L, true));

        assertEquals(2, result.size());
        assertTrue(result.stream().map(BlueprintView::getName).collect(Collectors.toSet()).containsAll(Set.of("global-view", "workspace-view")));
        assertFalse(result.stream().map(BlueprintView::getName).collect(Collectors.toSet()).contains("workspace-default-view"));
    }

    @Test
    void testGetAllAvailableInWorkspaceWhenGlobalDefaultEnabled() {
        Blueprint globalBp = getBlueprint("global-bp", DEFAULT);
        Blueprint userBp = getBlueprint("user-bp", USER_MANAGED);

        when(entitlementService.isGlobalDefaultTemplateEnabled(ACCOUNT_ID)).thenReturn(true);
        when(blueprintRepository.findAllGlobalDefaults(Set.of(DEFAULT))).thenReturn(Set.of(globalBp));
        when(blueprintRepository.findAllByWorkspaceIdAndStatusIn(1L, Set.of(USER_MANAGED, SERVICE_MANAGED))).thenReturn(Set.of(userBp));

        Set<Blueprint> result = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getAllAvailableInWorkspace(getWorkspace()));

        assertEquals(2, result.size());
        assertTrue(result.contains(globalBp));
        assertTrue(result.contains(userBp));
    }

    @Test
    void testGetResourceCrnListByResourceNameListWhenGlobalDefaultEnabled() {
        BlueprintFile bpFile = mock(BlueprintFile.class);
        when(bpFile.getResourceCrn()).thenReturn("crn:bp-default");

        when(entitlementService.isGlobalDefaultTemplateEnabled(ACCOUNT_ID)).thenReturn(true);
        when(defaultBlueprintCache.isDefaultByName("bp-default")).thenReturn(true);
        when(defaultBlueprintCache.isDefaultByName("bp-user")).thenReturn(false);
        when(defaultBlueprintCache.defaultBlueprints()).thenReturn(Map.of("bp-default", bpFile));
        when(blueprintRepository.findResourceCrnByNameAndAccountId("bp-user", ACCOUNT_ID)).thenReturn(Optional.of("crn:bp-user"));

        List<String> crns = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getResourceCrnListByResourceNameList(List.of("bp-default", "bp-user")));

        assertEquals(2, crns.size());
        assertEquals("crn:bp-default", crns.get(0));
        assertEquals("crn:bp-user", crns.get(1));
    }

    @Test
    void testGetByNameForWorkspaceIdWhenGlobalDefaultEnabledAndDefaultBlueprint() {
        Blueprint blueprint = getBlueprint("default-bp", DEFAULT);
        when(entitlementService.isGlobalDefaultTemplateEnabled(ACCOUNT_ID)).thenReturn(true);
        when(defaultBlueprintCache.isDefaultByName("default-bp")).thenReturn(true);
        when(blueprintRepository.findGlobalDefaultByName("default-bp")).thenReturn(Optional.of(blueprint));

        Blueprint result = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getByNameForWorkspaceId("default-bp", 1L));

        assertEquals(blueprint, result);
        verify(blueprintRepository).findGlobalDefaultByName("default-bp");
    }

    @Test
    void testGetByNamesForWorkspaceIdWhenGlobalDefaultDisabled() {
        Blueprint blueprint1 = getBlueprint("bp1", USER_MANAGED);
        Blueprint blueprint2 = getBlueprint("bp2", USER_MANAGED);
        Set<String> names = Set.of("bp1", "bp2");
        when(blueprintRepository.findByNameInAndWorkspaceId(names, 1L)).thenReturn(Set.of(blueprint1, blueprint2));

        Set<Blueprint> result = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getByNamesForWorkspaceId(names, 1L));

        assertEquals(2, result.size());
        assertTrue(result.containsAll(Set.of(blueprint1, blueprint2)));
        verify(blueprintRepository).findByNameInAndWorkspaceId(names, 1L);
        verify(blueprintRepository, never()).findGlobalDefaultByResourceNames(any());
    }

    @Test
    void testGetByNamesForWorkspaceIdWhenGlobalDefaultDisabledAndResourceNotFound() {
        Blueprint blueprint1 = getBlueprint("bp1", USER_MANAGED);
        Set<String> names = Set.of("bp1", "bp2");
        when(blueprintRepository.findByNameInAndWorkspaceId(names, 1L)).thenReturn(Set.of(blueprint1));

        NotFoundException exception = assertThrows(NotFoundException.class, () -> ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getByNamesForWorkspaceId(names, 1L)));

        assertThat(exception.getMessage()).contains("bp2");
    }

    @Test
    void testGetByNamesForWorkspaceIdWhenGlobalDefaultEnabledWithDefaultAndNonDefault() {
        Blueprint defaultBp = getBlueprint("default-bp", DEFAULT);
        Blueprint userBp = getBlueprint("user-bp", USER_MANAGED);
        Set<String> names = Set.of("default-bp", "user-bp");
        when(entitlementService.isGlobalDefaultTemplateEnabled(ACCOUNT_ID)).thenReturn(true);
        when(defaultBlueprintCache.isDefaultByName("default-bp")).thenReturn(true);
        when(defaultBlueprintCache.isDefaultByName("user-bp")).thenReturn(false);
        when(blueprintRepository.findGlobalDefaultByResourceNames(Set.of("default-bp"))).thenReturn(Set.of(defaultBp));
        when(blueprintRepository.findByNameInAndWorkspaceId(Set.of("user-bp"), 1L)).thenReturn(Set.of(userBp));

        Set<Blueprint> result = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getByNamesForWorkspaceId(names, 1L));

        assertEquals(2, result.size());
        assertTrue(result.containsAll(Set.of(defaultBp, userBp)));
    }

    @Test
    void testGetByNamesForWorkspaceIdWhenGlobalDefaultEnabledAndAllDefault() {
        Blueprint defaultBp1 = getBlueprint("default-bp1", DEFAULT);
        Blueprint defaultBp2 = getBlueprint("default-bp2", DEFAULT);
        Set<String> names = Set.of("default-bp1", "default-bp2");
        when(entitlementService.isGlobalDefaultTemplateEnabled(ACCOUNT_ID)).thenReturn(true);
        when(defaultBlueprintCache.isDefaultByName("default-bp1")).thenReturn(true);
        when(defaultBlueprintCache.isDefaultByName("default-bp2")).thenReturn(true);
        when(blueprintRepository.findGlobalDefaultByResourceNames(names)).thenReturn(Set.of(defaultBp1, defaultBp2));
        when(blueprintRepository.findByNameInAndWorkspaceId(Set.of(), 1L)).thenReturn(Set.of());

        Set<Blueprint> result = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getByNamesForWorkspaceId(names, 1L));

        assertEquals(2, result.size());
        assertTrue(result.containsAll(Set.of(defaultBp1, defaultBp2)));
        verify(blueprintRepository, never()).findByNameInAndWorkspaceId(names, 1L);
    }

    @Test
    void testGetByNamesForWorkspaceIdWhenGlobalDefaultEnabledAndDefaultNotFound() {
        Set<String> names = Set.of("default-bp");
        when(entitlementService.isGlobalDefaultTemplateEnabled(ACCOUNT_ID)).thenReturn(true);
        when(defaultBlueprintCache.isDefaultByName("default-bp")).thenReturn(true);
        when(blueprintRepository.findGlobalDefaultByResourceNames(Set.of("default-bp"))).thenReturn(Set.of());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getByNamesForWorkspaceId(names, 1L)));

        assertThat(exception.getMessage()).contains("default-bp");
        verify(blueprintRepository, never()).findByNameInAndWorkspaceId(any(), anyLong());
    }

    @Test
    void testGetByNamesForWorkspaceIdWhenGlobalDefaultEnabledAndNonDefaultNotFound() {
        Blueprint defaultBp = getBlueprint("default-bp", DEFAULT);
        Set<String> names = Set.of("default-bp", "user-bp");
        when(entitlementService.isGlobalDefaultTemplateEnabled(ACCOUNT_ID)).thenReturn(true);
        when(defaultBlueprintCache.isDefaultByName("default-bp")).thenReturn(true);
        when(defaultBlueprintCache.isDefaultByName("user-bp")).thenReturn(false);
        when(blueprintRepository.findGlobalDefaultByResourceNames(Set.of("default-bp"))).thenReturn(Set.of(defaultBp));
        when(blueprintRepository.findByNameInAndWorkspaceId(Set.of("user-bp"), 1L)).thenReturn(Set.of());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getByNamesForWorkspaceId(names, 1L)));

        assertThat(exception.getMessage()).contains("user-bp");
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
        blueprint.setResourceCrn("someCrn");
        return blueprint;
    }

    private BlueprintView getBlueprintView(String name, ResourceStatus status, Boolean sdxReady) {
        BlueprintView blueprint = new BlueprintView();
        blueprint.setName(name);
        blueprint.setWorkspace(getWorkspace());
        blueprint.setStatus(status);
        blueprint.setResourceCrn("someCrn");
        blueprint.setLastUpdated(123456789L);
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
package com.sequenceiq.distrox.v1.distrox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackAddVolumesRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackDeleteVolumesRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.SaltPasswordStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackEndpointV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.model.RotateSaltPasswordReason;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackClusterStatusViewToStatusConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.UserNamePasswordV4RequestToUpdateClusterV4RequestConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.view.StackApiViewToStackViewV4ResponseConverter;
import com.sequenceiq.cloudbreak.domain.projection.StackClusterStatusView;
import com.sequenceiq.cloudbreak.domain.projection.StackCrnView;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.StackApiView;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.ClusterCommonService;
import com.sequenceiq.cloudbreak.service.StackCommonService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.image.GenerateImageCatalogService;
import com.sequenceiq.cloudbreak.service.sharedservice.DatalakeService;
import com.sequenceiq.cloudbreak.service.stack.StackApiViewService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackImageService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.distrox.v1.distrox.service.DistroXClusterNameNormalizerService;
import com.sequenceiq.distrox.v1.distrox.service.EnvironmentServiceDecorator;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowLogService;

@ExtendWith(MockitoExtension.class)
class StackOperationsTest {

    private static final StackType STACK_TYPE = StackType.WORKLOAD;

    private static final Set<String> STACK_ENTRIES = Collections.emptySet();

    private static final String IMAGE_CATALOG = "image catalog";

    private static final String ACCOUNT_ID = "accountId";

    private static final Long WORKSPACE_ID = 1L;

    @InjectMocks
    private StackOperations underTest;

    @Mock
    private StackCommonService stackCommonService;

    @Mock
    private UserService userService;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private DistroXClusterNameNormalizerService clusterNameNormalizerService;

    @Mock
    private StackService stackService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ClusterCommonService clusterCommonService;

    @Mock
    private StackApiViewService stackApiViewService;

    @Mock
    private EnvironmentServiceDecorator environmentServiceDecorator;

    @Mock
    private CloudbreakUser cloudbreakUser;

    @Mock
    private StackApiViewToStackViewV4ResponseConverter stackApiViewToStackViewV4ResponseConverter;

    @Mock
    private StackClusterStatusViewToStatusConverter stackClusterStatusViewToStatusConverter;

    @Mock
    private UserNamePasswordV4RequestToUpdateClusterV4RequestConverter userNamePasswordV4RequestToUpdateClusterV4RequestConverter;

    @Mock
    private StackImageService stackImageService;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private GenerateImageCatalogService generateImageCatalogService;

    @Mock
    private EntitlementService entitlementService;

    private Stack stack;

    private User user;

    @Mock
    private FlowIdentifier flowIdentifier;

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private DatalakeService datalakeService;

    @Captor
    private ArgumentCaptor<List<String>> stringListCaptor;

    @BeforeEach
    void setUp() {
        user = TestUtil.user(1L, "someUserId");
        stack = TestUtil.stack();
        lenient().when(userService.getOrCreate(cloudbreakUser)).thenReturn(user);
    }

    @Test
    void testDeleteWhenForcedTrueThenDeleteCalled() {
        NameOrCrn nameOrCrn = NameOrCrn.ofName(stack.getName());

        underTest.delete(nameOrCrn, ACCOUNT_ID, true);

        verify(stackCommonService, times(1)).deleteWithKerberosInWorkspace(nameOrCrn, ACCOUNT_ID, true);
    }

    @Test
    void testDeleteWhenForcedFalseThenDeleteCalled() {
        NameOrCrn nameOrCrn = NameOrCrn.ofName(stack.getName());

        underTest.delete(nameOrCrn, ACCOUNT_ID, false);

        verify(stackCommonService, times(1)).deleteWithKerberosInWorkspace(nameOrCrn, ACCOUNT_ID, false);
    }

    @Test
    void testGetWhenNameOrCrnNameFilledThenProperGetCalled() {
        StackV4Response expected = stackResponse();
        NameOrCrn nameOrCrn = NameOrCrn.ofName(stack.getName());
        when(stackCommonService.findStackByNameOrCrnAndWorkspaceId(nameOrCrn, ACCOUNT_ID, STACK_ENTRIES, STACK_TYPE, false))
                .thenReturn(expected);

        StackV4Response result = underTest.get(nameOrCrn, ACCOUNT_ID, STACK_ENTRIES, STACK_TYPE, false);

        assertEquals(expected, result);
        verify(stackCommonService, times(1)).findStackByNameOrCrnAndWorkspaceId(nameOrCrn, ACCOUNT_ID, STACK_ENTRIES, STACK_TYPE, false);
        verify(datalakeService).decorateWithDataLakeResponseAnyPlatform(STACK_TYPE, result);
    }

    @Test
    void testGetWhenNameOrCrnCrnFilledThenProperGetCalled() {
        StackV4Response expected = stackResponse();
        NameOrCrn nameOrCrn = NameOrCrn.ofCrn(stack.getResourceCrn());
        when(stackCommonService.findStackByNameOrCrnAndWorkspaceId(nameOrCrn, ACCOUNT_ID, STACK_ENTRIES, STACK_TYPE, false))
                .thenReturn(expected);

        StackV4Response result = underTest.get(nameOrCrn, ACCOUNT_ID, STACK_ENTRIES, STACK_TYPE, false);

        assertEquals(expected, result);
        verify(stackCommonService, times(1)).findStackByNameOrCrnAndWorkspaceId(
                nameOrCrn, ACCOUNT_ID, STACK_ENTRIES, STACK_TYPE, false);
        verify(datalakeService).decorateWithDataLakeResponseAnyPlatform(STACK_TYPE, result);
    }

    @Test
    void testGetForInternalCrn() {
        when(stackApiViewService.retrieveStackByCrnAndType(anyString(), any(StackType.class))).thenReturn(new StackApiView());
        when(stackApiViewToStackViewV4ResponseConverter.convert(any(StackApiView.class))).thenReturn(new StackViewV4Response());
        doNothing().when(environmentServiceDecorator).prepareEnvironment(any(StackViewV4Response.class));

        StackViewV4Response response = underTest.getForInternalCrn(NameOrCrn.ofCrn("myCrn"), STACK_TYPE);

        assertNotNull(response);
        verify(stackApiViewService, times(1)).retrieveStackByCrnAndType(anyString(), any(StackType.class));
        verify(stackApiViewToStackViewV4ResponseConverter, times(1)).convert(any(StackApiView.class));
        verify(environmentServiceDecorator, times(1)).prepareEnvironment(any(StackViewV4Response.class));
    }

    @Test
    void testGetWithEnvironmentCrnsByResourceCrns() {
        StackCrnView stack1 = mock(StackCrnView.class);
        when(stack1.getResourceCrn()).thenReturn("crn1");
        when(stack1.getEnvironmentCrn()).thenReturn("envcrn1");
        StackCrnView stack2 = mock(StackCrnView.class);
        when(stack2.getResourceCrn()).thenReturn("crn2");
        when(stack2.getEnvironmentCrn()).thenReturn("envcrn2");
        StackCrnView stackWithoutEnv = mock(StackCrnView.class);
        when(stackWithoutEnv.getResourceCrn()).thenReturn("crn3");
        when(stackService.findAllByCrn(anySet())).thenReturn(List.of(stack1, stack2, stackWithoutEnv));

        Map<String, Optional<String>> result = ThreadBasedUserCrnProvider.doAs("crn:altus:iam:us-west-1:123:user:456",
                () -> underTest.getEnvironmentCrnsByResourceCrns(List.of("crn1", "crn2", "crn3")));

        Map<String, Optional<String>> expected = new LinkedHashMap<>();
        expected.put("crn1", Optional.of("envcrn1"));
        expected.put("crn2", Optional.of("envcrn2"));
        expected.put("crn3", Optional.empty());
        assertEquals(expected, result);
    }

    @Test
    void testChangeImageCatalogFlowNotInProgress() {
        NameOrCrn nameOrCrn = NameOrCrn.ofName(stack.getName());
        when(stackService.getByNameOrCrnInWorkspace(nameOrCrn, stack.getWorkspace().getId())).thenReturn(stack);
        when(flowLogService.isOtherFlowRunning(stack.getId())).thenReturn(false);

        underTest.changeImageCatalog(nameOrCrn, stack.getWorkspace().getId(), IMAGE_CATALOG);

        verify(stackImageService).changeImageCatalog(stack, IMAGE_CATALOG);
    }

    @Test
    void testChangeImageCatalogThrowsExceptionWhenFlowInProgress() {
        NameOrCrn nameOrCrn = NameOrCrn.ofName(stack.getName());
        when(stackService.getByNameOrCrnInWorkspace(nameOrCrn, stack.getWorkspace().getId())).thenReturn(stack);
        when(flowLogService.isOtherFlowRunning(stack.getId())).thenReturn(true);

        assertThrows(CloudbreakServiceException.class, () -> underTest.changeImageCatalog(nameOrCrn, stack.getWorkspace().getId(), IMAGE_CATALOG));
    }

    @Test
    void testGenerateImageCatalog() {
        NameOrCrn nameOrCrn = NameOrCrn.ofName(stack.getName());
        CloudbreakImageCatalogV3 imageCatalog = mock(CloudbreakImageCatalogV3.class);
        when(stackService.getByNameOrCrnInWorkspace(nameOrCrn, stack.getWorkspace().getId())).thenReturn(stack);
        when(generateImageCatalogService.generateImageCatalogForStack(stack)).thenReturn(imageCatalog);

        CloudbreakImageCatalogV3 actual = underTest.generateImageCatalog(nameOrCrn, stack.getWorkspace().getId());

        assertEquals(imageCatalog, actual);
    }

    @Test
    void rotateSaltPassword() {
        NameOrCrn nameOrCrn = NameOrCrn.ofName(stack.getName());

        underTest.rotateSaltPassword(nameOrCrn, ACCOUNT_ID, RotateSaltPasswordReason.MANUAL);

        verify(stackCommonService).rotateSaltPassword(nameOrCrn, ACCOUNT_ID, RotateSaltPasswordReason.MANUAL);
    }

    @Test
    void checkIfSaltPasswordRotationNeeded() {
        NameOrCrn nameOrCrn = NameOrCrn.ofName(stack.getName());
        SaltPasswordStatus saltPasswordStatus = SaltPasswordStatus.OK;
        when(stackCommonService.getSaltPasswordStatus(nameOrCrn, ACCOUNT_ID)).thenReturn(saltPasswordStatus);

        SaltPasswordStatus result = underTest.getSaltPasswordStatus(nameOrCrn, ACCOUNT_ID);

        assertEquals(saltPasswordStatus, result);
        verify(stackCommonService).getSaltPasswordStatus(nameOrCrn, ACCOUNT_ID);
    }

    @Test
    void modifyProxyConfig() {
        NameOrCrn nameOrCrn = NameOrCrn.ofName(stack.getName());
        String previousProxyConfigCrn = "prev-proxy-crn";

        underTest.modifyProxyConfig(nameOrCrn, ACCOUNT_ID, previousProxyConfigCrn);

        verify(stackCommonService).modifyProxyConfig(nameOrCrn, ACCOUNT_ID, previousProxyConfigCrn);
    }

    private StackV4Response stackResponse() {
        return new StackV4Response();
    }

    @Test
    void testPutDeleteVolumes() {
        NameOrCrn nameOrCrn = NameOrCrn.ofName(stack.getName());
        StackDeleteVolumesRequest stackDeleteVolumesRequest = new StackDeleteVolumesRequest();
        stackDeleteVolumesRequest.setStackId(1L);
        stackDeleteVolumesRequest.setGroup("COMPUTE");
        underTest.putDeleteVolumes(nameOrCrn, ACCOUNT_ID, stackDeleteVolumesRequest);

        verify(stackCommonService).putDeleteVolumesInWorkspace(nameOrCrn, ACCOUNT_ID, stackDeleteVolumesRequest);
    }

    @Test
    void syncTest() {
        NameOrCrn nameOrCrn = NameOrCrn.ofName("stackName");
        Set<StackType> permittedStackTypes = EnumSet.of(StackType.DATALAKE);
        when(stackCommonService.syncInWorkspace(nameOrCrn, ACCOUNT_ID, permittedStackTypes)).thenReturn(flowIdentifier);

        FlowIdentifier result = underTest.sync(nameOrCrn, ACCOUNT_ID, permittedStackTypes);

        assertThat(result).isSameAs(flowIdentifier);
    }

    @Test
    void testGetDeletedStacks() {
        StackClusterStatusView stackClusterStatusView1 = mock(StackClusterStatusView.class);
        StackClusterStatusView stackClusterStatusView2 = mock(StackClusterStatusView.class);
        StackStatusV4Response stackStatusV4Response1 = mock(StackStatusV4Response.class);
        StackStatusV4Response stackStatusV4Response2 = mock(StackStatusV4Response.class);
        Long since = 100L;
        when(stackService.getDeletedStacks(since)).thenReturn(List.of(stackClusterStatusView1, stackClusterStatusView2));
        when(stackClusterStatusViewToStatusConverter.convert(stackClusterStatusView1)).thenReturn(stackStatusV4Response1);
        when(stackClusterStatusViewToStatusConverter.convert(stackClusterStatusView2)).thenReturn(stackStatusV4Response2);
        List<StackStatusV4Response> deletedStacks = underTest.getDeletedStacks(since);
        verify(stackService).getDeletedStacks(since);
        assertEquals(2, deletedStacks.size());
        assertEquals(stackStatusV4Response1, deletedStacks.get(0));
        assertEquals(stackStatusV4Response2, deletedStacks.get(1));
    }

    @Test
    @Disabled("CB-31498")
    void testPutAddVolumes() {
        NameOrCrn nameOrCrn = NameOrCrn.ofName(stack.getName());
        StackAddVolumesRequest stackAddVolumesRequest = new StackAddVolumesRequest();
        stackAddVolumesRequest.setInstanceGroup("COMPUTE");
        stackAddVolumesRequest.setCloudVolumeUsageType("GENERAL");
        stackAddVolumesRequest.setSize(200L);
        stackAddVolumesRequest.setType("gp2");
        stackAddVolumesRequest.setNumberOfDisks(2L);

        underTest.putAddVolumes(nameOrCrn, stackAddVolumesRequest, ACCOUNT_ID);

        verify(stackCommonService).putAddVolumesInWorkspace(nameOrCrn, ACCOUNT_ID, stackAddVolumesRequest);
    }

    @Test
    void testGetEndpointsFroMultipleCrns() {
        List<String> crns = new ArrayList<>();
        crns.add("crn1");
        crns.add("crn2");

        when(stackService.getEndpointsByCrn("crn1", ACCOUNT_ID)).thenReturn(Map.of("proxy", Set.of()));
        when(stackService.getEndpointsByCrn("crn2", ACCOUNT_ID)).thenReturn(Map.of("proxy", Set.of()));

        StackEndpointV4Responses endpointV4Responses = underTest.getEndpointsCrns(crns, ACCOUNT_ID);

        assertEquals(2, endpointV4Responses.getResponses().size());
        verify(stackService).getEndpointsByCrn("crn1", ACCOUNT_ID);
        verify(stackService).getEndpointsByCrn("crn2", ACCOUNT_ID);
    }

    @Test
    void testFilterByServiceTypesPresent() {
        List<String> serviceTypes = List.of("service1", "service2");
        StackViewV4Response stackViewV4Response1 = getStackViewV4Response("crn1");
        StackViewV4Response stackViewV4Response2 = getStackViewV4Response("crn2");
        StackViewV4Response stackViewV4Response3 = getStackViewV4Response("crn3");
        StackViewV4Response stackViewV4Response4 = getStackViewV4Response("crn4");
        Set<StackViewV4Response> stackViewV4Responses = Set.of(stackViewV4Response1, stackViewV4Response2, stackViewV4Response3, stackViewV4Response4);
        StackDto stackDto1 = getStackDto("crn1", "blueprint1");
        StackDto stackDto2 = getStackDto("crn2", "blueprint2");
        StackDto stackDto3 = getStackDto("crn3", "blueprint1");
        StackDto stackDto4 = getStackDto("crn4", "blueprint2");
        when(stackDtoService.findAllByResourceCrnInWithoutResources(anyList())).thenReturn(List.of(stackDto1, stackDto2, stackDto3, stackDto4));
        when(blueprintService.anyOfTheServiceTypesPresentOnBlueprint("blueprint1", serviceTypes)).thenReturn(false);
        when(blueprintService.anyOfTheServiceTypesPresentOnBlueprint("blueprint2", serviceTypes)).thenReturn(true);

        Set<StackViewV4Response> result = underTest.filterByServiceTypesPresent(stackViewV4Responses, serviceTypes);

        verify(stackDtoService).findAllByResourceCrnInWithoutResources(stringListCaptor.capture());
        assertThat(stringListCaptor.getValue()).containsExactlyInAnyOrder("crn1", "crn2", "crn3", "crn4");
        assertThat(result).containsExactlyInAnyOrder(stackViewV4Response2, stackViewV4Response4);
    }

    @Test
    void testUpdateSalt() {
        NameOrCrn nameOrCrn = NameOrCrn.ofName(stack.getName());

        underTest.updateSalt(nameOrCrn, ACCOUNT_ID);

        verify(clusterCommonService).updateSalt(nameOrCrn, ACCOUNT_ID, false);
    }

    @Test
    void testUpdateSaltWhenSkipHighstate() {
        NameOrCrn nameOrCrn = NameOrCrn.ofName(stack.getName());

        underTest.updateSalt(nameOrCrn, ACCOUNT_ID, true);

        verify(clusterCommonService).updateSalt(nameOrCrn, ACCOUNT_ID, true);
    }

    private static StackViewV4Response getStackViewV4Response(String resourceCrn) {
        StackViewV4Response stackViewV4Response = mock(StackViewV4Response.class);
        when(stackViewV4Response.getCrn()).thenReturn(resourceCrn);
        return stackViewV4Response;
    }

    private StackDto getStackDto(String resourceCrn, String blueprintJsonText) {
        StackDto stackDto = mock();
        lenient().when(stackDto.getResourceCrn()).thenReturn(resourceCrn);
        when(stackDto.getBlueprintJsonText()).thenReturn(blueprintJsonText);
        return stackDto;
    }

}

package com.sequenceiq.distrox.v1.distrox;

import static com.sequenceiq.cloudbreak.util.TestConstants.DO_NOT_KEEP_VARIANT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.cloud.model.component.PreparedImages;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.service.cluster.ClusterDBValidationService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeAvailabilityService;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeCandidateFilterService;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradePreconditionService;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(MockitoExtension.class)
class StackUpgradeOperationsTest {

    private static final long STACK_ID = 1L;

    private static final long CLUSTER_ID = 1L;

    private static final String ACCOUNT_ID = "account-id";

    private static final String STACK_NAME = "stack-name";

    private static final String IMAGE_ID = "image-id";

    private static final boolean ROLLING_UPGRADE_ENABLED = true;

    private static final String ENVIRONMENT_CRN = "env-crn";

    @InjectMocks
    private StackUpgradeOperations underTest;

    @Mock
    private UserService userService;

    @Mock
    private UpgradeService upgradeService;

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private ClusterUpgradeAvailabilityService clusterUpgradeAvailabilityService;

    @Mock
    private UpgradePreconditionService upgradePreconditionService;

    @Mock
    private ClusterDBValidationService clusterDBValidationService;

    @Mock
    private CloudbreakUser cloudbreakUser;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    private ClusterUpgradeCandidateFilterService clusterUpgradeCandidateFilterService;

    private NameOrCrn nameOrCrn;

    @BeforeEach
    void before() {
        nameOrCrn = NameOrCrn.ofName(STACK_NAME);
    }

    @Test
    void testUpgradeOsShouldCallUpgradeService() {
        underTest.upgradeOs(nameOrCrn, ACCOUNT_ID, DO_NOT_KEEP_VARIANT);
        verify(upgradeService).upgradeOs(ACCOUNT_ID, nameOrCrn, DO_NOT_KEEP_VARIANT);
    }

    @Test
    void testUpgradeClusterShouldCallUpgradeService() {
        underTest.upgradeCluster(nameOrCrn, ACCOUNT_ID, IMAGE_ID, ROLLING_UPGRADE_ENABLED);
        verify(upgradeService).upgradeCluster(ACCOUNT_ID, nameOrCrn, IMAGE_ID, ROLLING_UPGRADE_ENABLED);
    }

    @Test
    void testCheckForOsUpgradeShouldShouldCallUpgradeServiceWhenTheClusterNameIsAvailable() {
        User user = new User();
        when(userService.getOrCreate(cloudbreakUser)).thenReturn(user);

        underTest.checkForOsUpgrade(nameOrCrn, cloudbreakUser, ACCOUNT_ID);

        verify(userService).getOrCreate(cloudbreakUser);
        verify(upgradeService).getOsUpgradeOptionByStackNameOrCrn(ACCOUNT_ID, nameOrCrn, user);
    }

    @Test
    void testCheckForOsUpgradeShouldThrowExceptionWhenTheClusterNameIsNotAvailable() {
        when(userService.getOrCreate(cloudbreakUser)).thenReturn(new User());

        assertThrows(BadRequestException.class, () -> underTest.checkForOsUpgrade(NameOrCrn.ofCrn("crn"), cloudbreakUser, ACCOUNT_ID));

        verify(userService).getOrCreate(cloudbreakUser);
        verifyNoInteractions(upgradeService);
    }

    @Test
    void testCheckForClusterUpgradeShouldReturnUpgradeCandidatesWhenTheUpgradeIsRuntimeUpgradeAndTheStackTypeIsWorkloadAndReplaceVmDisabled() {
        Stack stack = createStack(StackType.WORKLOAD);
        UpgradeV4Request request = createUpgradeRequest(null, null);
        UpgradeV4Response upgradeResponse = new UpgradeV4Response();
        upgradeResponse.setUpgradeCandidates(List.of(new ImageInfoV4Response()));
        when(instanceGroupService.getByStackAndFetchTemplates(STACK_ID)).thenReturn(Collections.emptySet());
        when(upgradePreconditionService.notUsingEphemeralVolume(stack)).thenReturn(false);
        when(clusterUpgradeAvailabilityService
                .checkForUpgradesByName(stack, false, false, request.getInternalUpgradeSettings(), false, null))
                .thenReturn(upgradeResponse);

        UpgradeV4Response actual = underTest.checkForClusterUpgrade(ACCOUNT_ID, stack, request);

        assertEquals(upgradeResponse, actual);
        verify(instanceGroupService).getByStackAndFetchTemplates(STACK_ID);
        verify(upgradePreconditionService).notUsingEphemeralVolume(stack);
        verify(clusterUpgradeAvailabilityService)
                .checkForUpgradesByName(stack, false, false, request.getInternalUpgradeSettings(), false, null);
        verify(clusterUpgradeCandidateFilterService).filterUpgradeOptions(upgradeResponse, request, false);
    }

    @Test
    void testCheckForClusterUpgradeShouldReturnUpgradeCandidatesWhenTheUpgradeIsRuntimeUpgradeAndTheStackTypeIsWorkloadAndReplaceVmEnabled() {
        Stack stack = createStack(StackType.WORKLOAD);
        UpgradeV4Request request = createUpgradeRequest(true, null);
        UpgradeV4Response upgradeResponse = createUpgradeResponse();
        when(instanceGroupService.getByStackAndFetchTemplates(STACK_ID)).thenReturn(Collections.emptySet());
        when(clusterUpgradeAvailabilityService
                .checkForUpgradesByName(stack, false, true, request.getInternalUpgradeSettings(), false, null))
                .thenReturn(upgradeResponse);

        UpgradeV4Response actual = underTest.checkForClusterUpgrade(ACCOUNT_ID, stack, request);

        assertEquals(upgradeResponse, actual);
        verify(instanceGroupService).getByStackAndFetchTemplates(STACK_ID);
        verify(instanceGroupService).getByStackAndFetchTemplates(STACK_ID);
        verify(clusterUpgradeAvailabilityService)
                .checkForUpgradesByName(stack, false, true, request.getInternalUpgradeSettings(), false, null);
        verify(clusterUpgradeCandidateFilterService).filterUpgradeOptions(upgradeResponse, request, false);
        verifyNoInteractions(upgradePreconditionService);
        verifyNoInteractions(clusterDBValidationService);
    }

    @Test
    void testCheckForClusterUpgradeShouldReturnUpgradeCandidatesWhenTheUpgradeIsRuntimeUpgradeAndTheStackTypeIsDataLakeAndReplaceVmEnabled() {
        Stack stack = createStack(StackType.DATALAKE);
        UpgradeV4Request request = createUpgradeRequest(null, true);
        UpgradeV4Response upgradeResponse = createUpgradeResponse();
        when(instanceGroupService.getByStackAndFetchTemplates(STACK_ID)).thenReturn(Collections.emptySet());
        when(clusterUpgradeAvailabilityService
                .checkForUpgradesByName(stack, false, true, request.getInternalUpgradeSettings(), false, null))
                .thenReturn(upgradeResponse);

        UpgradeV4Response actual = underTest.checkForClusterUpgrade(ACCOUNT_ID, stack, request);

        assertEquals(upgradeResponse, actual);
        verify(instanceGroupService).getByStackAndFetchTemplates(STACK_ID);
        verify(instanceGroupService).getByStackAndFetchTemplates(STACK_ID);
        verify(clusterUpgradeAvailabilityService)
                .checkForUpgradesByName(stack, false, true, request.getInternalUpgradeSettings(), false, null);
        verify(clusterUpgradeCandidateFilterService).filterUpgradeOptions(upgradeResponse, request, true);
        verify(upgradePreconditionService).checkForRunningAttachedClusters(ENVIRONMENT_CRN, request.isSkipDataHubValidation(), false, ACCOUNT_ID);
        verifyNoInteractions(clusterDBValidationService);
    }

    @Test
    void testCheckForClusterUpgradeShouldReturnUpgradeCandidatesWhenTheUpgradeIsRuntimeUpgradeAndTheStackTypeIsDataLakeAndReplaceVmEnabledAndPrepare() {
        Stack stack = createStack(StackType.DATALAKE);
        UpgradeV4Request request = createUpgradeRequest(null, null);
        request.setInternalUpgradeSettings(new InternalUpgradeSettings(false, true, false));
        UpgradeV4Response upgradeResponse = createUpgradeResponse();
        when(instanceGroupService.getByStackAndFetchTemplates(STACK_ID)).thenReturn(Collections.emptySet());
        when(clusterUpgradeAvailabilityService
                .checkForUpgradesByName(stack, false, true, request.getInternalUpgradeSettings(), false, null))
                .thenReturn(upgradeResponse);

        UpgradeV4Response actual = underTest.checkForClusterUpgrade(ACCOUNT_ID, stack, request);

        assertEquals(upgradeResponse, actual);
        verify(instanceGroupService).getByStackAndFetchTemplates(STACK_ID);
        verify(instanceGroupService).getByStackAndFetchTemplates(STACK_ID);
        verify(clusterUpgradeAvailabilityService)
                .checkForUpgradesByName(stack, false, true, request.getInternalUpgradeSettings(), false, null);
        verify(clusterUpgradeCandidateFilterService).filterUpgradeOptions(upgradeResponse, request, true);
        verifyNoInteractions(upgradePreconditionService, clusterDBValidationService);
    }

    @Test
    void testCheckForClusterUpgradeShouldReturnCompositeErrorWhenBothAttachedDataHubValidationsFail() {
        Stack stack = createStack(StackType.DATALAKE);
        UpgradeV4Request request = createUpgradeRequest(null, true);
        UpgradeV4Response upgradeResponseToReturn = createUpgradeResponse();
        UpgradeV4Response expectedResponse = createUpgradeResponse();
        expectedResponse.setReason("There are attached Data Hub clusters in incorrect state");
        when(instanceGroupService.getByStackAndFetchTemplates(STACK_ID)).thenReturn(Collections.emptySet());
        when(clusterUpgradeAvailabilityService
                .checkForUpgradesByName(stack, false, true, request.getInternalUpgradeSettings(), false, null))
                .thenReturn(upgradeResponseToReturn);
        when(upgradePreconditionService.checkForRunningAttachedClusters(ENVIRONMENT_CRN, request.isSkipDataHubValidation(), false, ACCOUNT_ID))
                .thenReturn("There are attached Data Hub clusters in incorrect state");

        UpgradeV4Response actual = underTest.checkForClusterUpgrade(ACCOUNT_ID, stack, request);

        assertEquals(expectedResponse, actual);
        verify(instanceGroupService).getByStackAndFetchTemplates(STACK_ID);
        verify(instanceGroupService).getByStackAndFetchTemplates(STACK_ID);
        verify(clusterUpgradeAvailabilityService)
                .checkForUpgradesByName(stack, false, true, request.getInternalUpgradeSettings(), false, null);
        verify(clusterUpgradeCandidateFilterService).filterUpgradeOptions(upgradeResponseToReturn, request, true);
        verify(upgradePreconditionService).checkForRunningAttachedClusters(ENVIRONMENT_CRN, request.isSkipDataHubValidation(), false, ACCOUNT_ID);
        verifyNoInteractions(clusterDBValidationService);
    }

    @Test
    void testCheckForClusterUpgradeShouldNotValidateUpgradeableDataHubsWhenDataHubUpgradeEntitlementIsGranted() {
        Stack stack = createStack(StackType.DATALAKE);
        UpgradeV4Request request = createUpgradeRequest(null, null);
        UpgradeV4Response upgradeResponseToReturn = createUpgradeResponse();
        UpgradeV4Response expectedResponse = createUpgradeResponse();
        expectedResponse.setReason("There are attached Data Hub clusters in incorrect state");
        when(instanceGroupService.getByStackAndFetchTemplates(STACK_ID)).thenReturn(Collections.emptySet());
        when(clusterUpgradeAvailabilityService
                .checkForUpgradesByName(stack, false, true, request.getInternalUpgradeSettings(), false, null))
                .thenReturn(upgradeResponseToReturn);
        when(upgradePreconditionService.checkForRunningAttachedClusters(ENVIRONMENT_CRN, request.isSkipDataHubValidation(), false, ACCOUNT_ID))
                .thenReturn("There are attached Data Hub clusters in incorrect state");

        UpgradeV4Response actual = underTest.checkForClusterUpgrade(ACCOUNT_ID, stack, request);

        assertEquals(expectedResponse, actual);
        verify(instanceGroupService).getByStackAndFetchTemplates(STACK_ID);
        verify(instanceGroupService).getByStackAndFetchTemplates(STACK_ID);
        verify(clusterUpgradeAvailabilityService)
                .checkForUpgradesByName(stack, false, true, request.getInternalUpgradeSettings(), false, null);
        verify(clusterUpgradeCandidateFilterService).filterUpgradeOptions(upgradeResponseToReturn, request, true);
        verify(upgradePreconditionService).checkForRunningAttachedClusters(ENVIRONMENT_CRN, request.isSkipDataHubValidation(), false, ACCOUNT_ID);
        verifyNoInteractions(clusterDBValidationService);
    }

    @Test
    void testCheckForClusterUpgradeShouldReturnUpgradeCandidatesWhenImageIdIsPresentInRequest() {
        Stack stack = createStack(StackType.WORKLOAD);
        UpgradeV4Request request = createUpgradeRequest(null, null);
        request.setImageId(IMAGE_ID);
        Cluster clusterRef = new Cluster();
        clusterRef.setId(STACK_ID);
        ClusterComponent component = new ClusterComponent(ComponentType.CLUSTER_UPGRADE_PREPARED_IMAGES,
                new Json(new PreparedImages(List.of(IMAGE_ID))), clusterRef);
        doReturn(component).when(clusterComponentConfigProvider).getComponent(STACK_ID, ComponentType.CLUSTER_UPGRADE_PREPARED_IMAGES,
                ComponentType.CLUSTER_UPGRADE_PREPARED_IMAGES.name());
        UpgradeV4Response upgradeResponse = new UpgradeV4Response();
        upgradeResponse.setUpgradeCandidates(List.of(new ImageInfoV4Response()));
        upgradeResponse.getUpgradeCandidates().forEach(imageInfoV4Response -> imageInfoV4Response.setImageId(IMAGE_ID));
        when(instanceGroupService.getByStackAndFetchTemplates(STACK_ID)).thenReturn(Collections.emptySet());
        when(upgradePreconditionService.notUsingEphemeralVolume(stack)).thenReturn(false);
        when(clusterUpgradeAvailabilityService
                .checkForUpgradesByName(stack, false, false, request.getInternalUpgradeSettings(), true, "image-id"))
                .thenReturn(upgradeResponse);

        UpgradeV4Response actual = underTest.checkForClusterUpgrade(ACCOUNT_ID, stack, request);

        assertEquals(upgradeResponse, actual);
        assertTrue(upgradeResponse.getUpgradeCandidates().getFirst().isPrepared());
        verify(instanceGroupService).getByStackAndFetchTemplates(STACK_ID);
        verify(upgradePreconditionService).notUsingEphemeralVolume(stack);
        verify(clusterUpgradeAvailabilityService)
                .checkForUpgradesByName(stack, false, false, request.getInternalUpgradeSettings(), true, "image-id");
        verify(clusterUpgradeCandidateFilterService).filterUpgradeOptions(upgradeResponse, request, false);
    }

    @Test
    void testCheckForClusterUpgradeWhenClusterComponentIsNull() {
        Stack stack = createStack(StackType.WORKLOAD);
        UpgradeV4Request request = createUpgradeRequest(null, null);
        request.setImageId(IMAGE_ID);
        doReturn(null).when(clusterComponentConfigProvider).getComponent(STACK_ID, ComponentType.CLUSTER_UPGRADE_PREPARED_IMAGES,
                ComponentType.CLUSTER_UPGRADE_PREPARED_IMAGES.name());
        UpgradeV4Response upgradeResponse = new UpgradeV4Response();
        upgradeResponse.setUpgradeCandidates(List.of(new ImageInfoV4Response()));
        upgradeResponse.getUpgradeCandidates().forEach(imageInfoV4Response -> imageInfoV4Response.setImageId(IMAGE_ID));
        when(instanceGroupService.getByStackAndFetchTemplates(STACK_ID)).thenReturn(Collections.emptySet());
        when(upgradePreconditionService.notUsingEphemeralVolume(stack)).thenReturn(false);
        when(clusterUpgradeAvailabilityService
                .checkForUpgradesByName(stack, false, false, request.getInternalUpgradeSettings(), true, "image-id"))
                .thenReturn(upgradeResponse);

        UpgradeV4Response actual = underTest.checkForClusterUpgrade(ACCOUNT_ID, stack, request);

        assertEquals(upgradeResponse, actual);
        assertFalse(upgradeResponse.getUpgradeCandidates().getFirst().isPrepared());
        verify(instanceGroupService).getByStackAndFetchTemplates(STACK_ID);
        verify(upgradePreconditionService).notUsingEphemeralVolume(stack);
        verify(clusterUpgradeAvailabilityService)
                .checkForUpgradesByName(stack, false, false, request.getInternalUpgradeSettings(), true, "image-id");
        verify(clusterUpgradeCandidateFilterService).filterUpgradeOptions(upgradeResponse, request, false);
    }

    @Test
    void testCheckForClusterUpgradeWhenJsonParseExceptionIsThrown() {
        Stack stack = createStack(StackType.WORKLOAD);
        UpgradeV4Request request = createUpgradeRequest(null, null);
        request.setImageId(IMAGE_ID);
        Cluster clusterRef = new Cluster();
        clusterRef.setId(STACK_ID);
        ClusterComponent component = new ClusterComponent(ComponentType.CLUSTER_UPGRADE_PREPARED_IMAGES,
                new Json(new PreparedImages(List.of(IMAGE_ID + "1"))), clusterRef);
        doReturn(component).when(clusterComponentConfigProvider).getComponent(CLUSTER_ID, ComponentType.CLUSTER_UPGRADE_PREPARED_IMAGES,
                ComponentType.CLUSTER_UPGRADE_PREPARED_IMAGES.name());
        UpgradeV4Response upgradeResponse = new UpgradeV4Response();
        upgradeResponse.setUpgradeCandidates(List.of(new ImageInfoV4Response()));
        upgradeResponse.getUpgradeCandidates().forEach(imageInfoV4Response -> imageInfoV4Response.setImageId(IMAGE_ID));
        when(instanceGroupService.getByStackAndFetchTemplates(STACK_ID)).thenReturn(Collections.emptySet());
        when(upgradePreconditionService.notUsingEphemeralVolume(stack)).thenReturn(false);
        when(clusterUpgradeAvailabilityService
                .checkForUpgradesByName(stack, false, false, request.getInternalUpgradeSettings(), true, "image-id"))
                .thenReturn(upgradeResponse);

        UpgradeV4Response actual = underTest.checkForClusterUpgrade(ACCOUNT_ID, stack, request);

        assertEquals(upgradeResponse, actual);
        assertFalse(upgradeResponse.getUpgradeCandidates().getFirst().isPrepared());
        verify(instanceGroupService).getByStackAndFetchTemplates(STACK_ID);
        verify(upgradePreconditionService).notUsingEphemeralVolume(stack);
        verify(clusterUpgradeAvailabilityService)
                .checkForUpgradesByName(stack, false, false, request.getInternalUpgradeSettings(), true, "image-id");
        verify(clusterUpgradeCandidateFilterService).filterUpgradeOptions(upgradeResponse, request, false);
    }

    @Test
    public void testPrepareUpgrade() {
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, "pollId");
        when(upgradeService.prepareClusterUpgrade(ACCOUNT_ID, nameOrCrn, IMAGE_ID)).thenReturn(flowIdentifier);

        FlowIdentifier result = underTest.prepareClusterUpgrade(nameOrCrn, ACCOUNT_ID, IMAGE_ID);

        assertEquals(flowIdentifier, result);
    }

    @Test
    void testCheckForClusterUpgradeShouldReturnReturnErrorMessageWhenFailedToGetAvailableCandidates() {
        Stack stack = createStack(StackType.WORKLOAD);
        UpgradeV4Request request = createUpgradeRequest(null, null);
        UpgradeV4Response upgradeResponse = new UpgradeV4Response(new ImageInfoV4Response(), null, "Failed to collect upgrade candidates.");
        when(instanceGroupService.getByStackAndFetchTemplates(STACK_ID)).thenReturn(Collections.emptySet());
        when(upgradePreconditionService.notUsingEphemeralVolume(stack)).thenReturn(false);
        when(clusterUpgradeAvailabilityService
                .checkForUpgradesByName(stack, false, false, request.getInternalUpgradeSettings(), false, null))
                .thenReturn(upgradeResponse);

        UpgradeV4Response actual = underTest.checkForClusterUpgrade(ACCOUNT_ID, stack, request);

        assertEquals(upgradeResponse, actual);
        verify(instanceGroupService).getByStackAndFetchTemplates(STACK_ID);
        verify(upgradePreconditionService).notUsingEphemeralVolume(stack);
        verify(clusterUpgradeAvailabilityService)
                .checkForUpgradesByName(stack, false, false, request.getInternalUpgradeSettings(), false, null);
        verify(clusterUpgradeCandidateFilterService, never()).filterUpgradeOptions(upgradeResponse, request, false);
        verifyNoInteractions(clusterComponentConfigProvider);
    }

    private UpgradeV4Response createUpgradeResponse() {
        UpgradeV4Response upgradeResponse = new UpgradeV4Response();
        upgradeResponse.setUpgradeCandidates(List.of(new ImageInfoV4Response()));
        return upgradeResponse;
    }

    private UpgradeV4Request createUpgradeRequest(Boolean replaceVm, Boolean skipDataHubValidation) {
        UpgradeV4Request upgradeRequest = new UpgradeV4Request();
        upgradeRequest.setReplaceVms(replaceVm);
        upgradeRequest.setSkipDataHubValidation(skipDataHubValidation);
        upgradeRequest.setInternalUpgradeSettings(new InternalUpgradeSettings(true));
        return upgradeRequest;
    }

    private Stack createStack(StackType stackType) {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setType(stackType);
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        Workspace workspace = new Workspace();
        workspace.setTenant(new Tenant());
        stack.setWorkspace(workspace);
        Cluster cluster = new Cluster();
        cluster.setId(STACK_ID);
        stack.setCluster(cluster);
        return stack;
    }

}

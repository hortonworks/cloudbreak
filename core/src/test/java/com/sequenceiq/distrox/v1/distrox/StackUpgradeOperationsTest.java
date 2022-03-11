package com.sequenceiq.distrox.v1.distrox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.BadRequestException;

import org.junit.jupiter.api.Assertions;
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
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.conf.LimitConfiguration;
import com.sequenceiq.cloudbreak.domain.projection.StackInstanceCount;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.cluster.ClusterDBValidationService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeAvailabilityService;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradePreconditionService;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.workspace.model.User;

@ExtendWith(MockitoExtension.class)
class StackUpgradeOperationsTest {

    private static final long STACK_ID = 1L;

    private static final long WORKSPACE_ID = 2L;

    private static final String ACCOUNT_ID = "account-id";

    private static final String STACK_NAME = "stack-name";

    private static final String IMAGE_ID = "image-id";

    private static final int INSTANCE_COUNT = 10;

    private static final int NODE_LIMIT = 15;

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
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private LimitConfiguration limitConfiguration;

    @Mock
    private ClusterUpgradeAvailabilityService clusterUpgradeAvailabilityService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private UpgradePreconditionService upgradePreconditionService;

    @Mock
    private ClusterDBValidationService clusterDBValidationService;

    @Mock
    private StackOperations stackOperations;

    @Mock
    private CloudbreakUser cloudbreakUser;

    private NameOrCrn nameOrCrn;

    @BeforeEach
    void before() {
        nameOrCrn = NameOrCrn.ofName(STACK_NAME);
    }

    @Test
    void testUpgradeOsShouldCallUpgradeService() {
        underTest.upgradeOs(nameOrCrn, WORKSPACE_ID);
        verify(upgradeService).upgradeOs(WORKSPACE_ID, nameOrCrn);
    }

    @Test
    void testUpgradeClusterShouldCallUpgradeService() {
        underTest.upgradeCluster(nameOrCrn, WORKSPACE_ID, IMAGE_ID);
        verify(upgradeService).upgradeCluster(WORKSPACE_ID, nameOrCrn, IMAGE_ID);
    }

    @Test
    void testCheckForOsUpgradeShouldShouldCallUpgradeServiceWhenTheClusterNameIsAvailable() {
        User user = new User();
        when(userService.getOrCreate(cloudbreakUser)).thenReturn(user);

        underTest.checkForOsUpgrade(nameOrCrn, cloudbreakUser, WORKSPACE_ID);

        verify(userService).getOrCreate(cloudbreakUser);
        verify(upgradeService).getOsUpgradeOptionByStackNameOrCrn(WORKSPACE_ID, nameOrCrn, user);
    }

    @Test
    void testCheckForOsUpgradeShouldThrowExceptionWhenTheClusterNameIsNotAvailable() {
        when(userService.getOrCreate(cloudbreakUser)).thenReturn(new User());

        assertThrows(BadRequestException.class, () -> underTest.checkForOsUpgrade(NameOrCrn.ofCrn("crn"), cloudbreakUser, WORKSPACE_ID));

        verify(userService).getOrCreate(cloudbreakUser);
        verifyNoInteractions(upgradeService);
    }

    @Test
    void throwsBadRequestIfClusterHasMoreNodesThanTheLimit() {
        UpgradeV4Request request = createUpgradeRequest(true);
        StackInstanceCount stackInstanceCount = mock(StackInstanceCount.class);
        when(stackInstanceCount.getInstanceCount()).thenReturn(201);
        when(limitConfiguration.getUpgradeNodeCountLimit()).thenReturn(200);
        when(instanceMetaDataService.countByStackId(any())).thenReturn(stackInstanceCount);

        BadRequestException exception = Assertions.assertThrows(BadRequestException.class,
                () -> underTest.checkForClusterUpgrade("accId", new Stack(), WORKSPACE_ID, request));

        assertEquals("There are 201 nodes in the cluster. Upgrade has a limit of 200 nodes, above the limit it is unstable. " +
                "Please downscale the cluster below the limit and retry the upgrade.", exception.getMessage());
    }

    @Test
    void testCheckForClusterUpgradeShouldReturnUpgradeCandidatesWhenTheUpgradeIsRuntimeUpgradeAndTheStackTypeIsWorkloadAndReplaceVmDisabled() {
        Stack stack = createStack(StackType.WORKLOAD);
        UpgradeV4Request request = createUpgradeRequest(null);
        UpgradeV4Response upgradeResponse = new UpgradeV4Response();
        upgradeResponse.setUpgradeCandidates(List.of(new ImageInfoV4Response()));
        when(instanceGroupService.getByStackAndFetchTemplates(STACK_ID)).thenReturn(Collections.emptySet());
        when(upgradeService.isOsUpgrade(request)).thenReturn(false);
        when(upgradePreconditionService.notUsingEphemeralVolume(stack)).thenReturn(false);
        when(clusterUpgradeAvailabilityService.checkForUpgradesByName(stack, false, false, request.getInternalUpgradeSettings())).thenReturn(upgradeResponse);
        when(entitlementService.runtimeUpgradeEnabled(ACCOUNT_ID)).thenReturn(true);

        UpgradeV4Response actual = underTest.checkForClusterUpgrade(ACCOUNT_ID, stack, WORKSPACE_ID, request);

        assertEquals(upgradeResponse, actual);
        verify(instanceGroupService).getByStackAndFetchTemplates(STACK_ID);
        verify(upgradeService).isOsUpgrade(request);
        verify(upgradePreconditionService).notUsingEphemeralVolume(stack);
        verify(clusterUpgradeAvailabilityService).checkForUpgradesByName(stack, false, false, request.getInternalUpgradeSettings());
        verify(clusterUpgradeAvailabilityService).filterUpgradeOptions(ACCOUNT_ID, upgradeResponse, request, false);
        verify(entitlementService).runtimeUpgradeEnabled(ACCOUNT_ID);
        verifyNoInteractions(stackOperations);
    }

    @Test
    void testCheckForClusterUpgradeShouldReturnUpgradeCandidatesWhenTheUpgradeIsRuntimeUpgradeAndTheStackTypeIsWorkloadAndReplaceVmEnabled() {
        Stack stack = createStack(StackType.WORKLOAD);
        UpgradeV4Request request = createUpgradeRequest(true);
        UpgradeV4Response upgradeResponse = createUpgradeResponse();
        when(instanceGroupService.getByStackAndFetchTemplates(STACK_ID)).thenReturn(Collections.emptySet());
        when(upgradeService.isOsUpgrade(request)).thenReturn(false);
        StackInstanceCount instanceCount = mock(StackInstanceCount.class);
        when(instanceCount.getInstanceCount()).thenReturn(INSTANCE_COUNT);
        when(instanceMetaDataService.countByStackId(STACK_ID)).thenReturn(instanceCount);
        when(limitConfiguration.getUpgradeNodeCountLimit()).thenReturn(NODE_LIMIT);
        when(clusterUpgradeAvailabilityService.checkForUpgradesByName(stack, false, true, request.getInternalUpgradeSettings())).thenReturn(upgradeResponse);
        when(entitlementService.runtimeUpgradeEnabled(ACCOUNT_ID)).thenReturn(true);

        UpgradeV4Response actual = underTest.checkForClusterUpgrade(ACCOUNT_ID, stack, WORKSPACE_ID, request);

        assertEquals(upgradeResponse, actual);
        verify(instanceGroupService).getByStackAndFetchTemplates(STACK_ID);
        verify(upgradeService).isOsUpgrade(request);
        verify(instanceGroupService).getByStackAndFetchTemplates(STACK_ID);
        verify(limitConfiguration).getUpgradeNodeCountLimit();
        verify(clusterUpgradeAvailabilityService).checkForUpgradesByName(stack, false, true, request.getInternalUpgradeSettings());
        verify(clusterUpgradeAvailabilityService).filterUpgradeOptions(ACCOUNT_ID, upgradeResponse, request, false);
        verify(entitlementService).runtimeUpgradeEnabled(ACCOUNT_ID);
        verifyNoInteractions(upgradePreconditionService);
        verifyNoInteractions(clusterDBValidationService);
        verifyNoInteractions(stackOperations);
    }

    @Test
    void testCheckForClusterUpgradeShouldReturnUpgradeCandidatesWhenTheUpgradeIsRuntimeUpgradeAndTheStackTypeIsDataLakeAndReplaceVmEnabled() {
        Stack stack = createStack(StackType.DATALAKE);
        StackViewV4Responses stackViewV4Responses = new StackViewV4Responses();
        UpgradeV4Request request = createUpgradeRequest(null);
        UpgradeV4Response upgradeResponse = createUpgradeResponse();
        when(instanceGroupService.getByStackAndFetchTemplates(STACK_ID)).thenReturn(Collections.emptySet());
        when(upgradeService.isOsUpgrade(request)).thenReturn(false);
        StackInstanceCount instanceCount = mock(StackInstanceCount.class);
        when(instanceCount.getInstanceCount()).thenReturn(INSTANCE_COUNT);
        when(instanceMetaDataService.countByStackId(STACK_ID)).thenReturn(instanceCount);
        when(limitConfiguration.getUpgradeNodeCountLimit()).thenReturn(NODE_LIMIT);
        when(clusterUpgradeAvailabilityService.checkForUpgradesByName(stack, false, true, request.getInternalUpgradeSettings())).thenReturn(upgradeResponse);
        when(entitlementService.runtimeUpgradeEnabled(ACCOUNT_ID)).thenReturn(true);
        when(stackOperations.listByEnvironmentCrn(eq(WORKSPACE_ID), eq(ENVIRONMENT_CRN), any())).thenReturn(stackViewV4Responses);

        UpgradeV4Response actual = underTest.checkForClusterUpgrade(ACCOUNT_ID, stack, WORKSPACE_ID, request);

        assertEquals(upgradeResponse, actual);
        verify(instanceGroupService).getByStackAndFetchTemplates(STACK_ID);
        verify(upgradeService).isOsUpgrade(request);
        verify(instanceGroupService).getByStackAndFetchTemplates(STACK_ID);
        verify(limitConfiguration).getUpgradeNodeCountLimit();
        verify(clusterUpgradeAvailabilityService).checkForUpgradesByName(stack, false, true, request.getInternalUpgradeSettings());
        verify(clusterUpgradeAvailabilityService).filterUpgradeOptions(ACCOUNT_ID, upgradeResponse, request, true);
        verify(entitlementService).runtimeUpgradeEnabled(ACCOUNT_ID);
        verify(stackOperations).listByEnvironmentCrn(eq(WORKSPACE_ID), eq(ENVIRONMENT_CRN), any());
        verify(upgradePreconditionService).checkForRunningAttachedClusters(stackViewV4Responses, stack);
        verify(upgradePreconditionService).checkForNonUpgradeableAttachedClusters(stackViewV4Responses);
        verifyNoInteractions(clusterDBValidationService);
    }

    @Test
    void testCheckForClusterUpgradeShouldReturnCompositeErrorWhenBothAttachedDataHubValidationsFail() {
        Stack stack = createStack(StackType.DATALAKE);
        StackViewV4Responses stackViewV4Responses = new StackViewV4Responses();
        UpgradeV4Request request = createUpgradeRequest(null);
        UpgradeV4Response upgradeResponseToReturn = createUpgradeResponse();
        UpgradeV4Response expectedResponse = createUpgradeResponse();
        expectedResponse.setReason("There are attached Data Hub clusters that are non-upgradeable There are attached Data Hub clusters in incorrect state");
        when(instanceGroupService.getByStackAndFetchTemplates(STACK_ID)).thenReturn(Collections.emptySet());
        when(upgradeService.isOsUpgrade(request)).thenReturn(false);
        StackInstanceCount instanceCount = mock(StackInstanceCount.class);
        when(instanceCount.getInstanceCount()).thenReturn(INSTANCE_COUNT);
        when(instanceMetaDataService.countByStackId(STACK_ID)).thenReturn(instanceCount);
        when(limitConfiguration.getUpgradeNodeCountLimit()).thenReturn(NODE_LIMIT);
        when(clusterUpgradeAvailabilityService.checkForUpgradesByName(stack, false, true, request.getInternalUpgradeSettings()))
                .thenReturn(upgradeResponseToReturn);
        when(entitlementService.runtimeUpgradeEnabled(ACCOUNT_ID)).thenReturn(true);
        when(stackOperations.listByEnvironmentCrn(eq(WORKSPACE_ID), eq(ENVIRONMENT_CRN), any())).thenReturn(stackViewV4Responses);
        when(upgradePreconditionService.checkForRunningAttachedClusters(stackViewV4Responses, stack))
                .thenReturn("There are attached Data Hub clusters in incorrect state");
        when(upgradePreconditionService.checkForNonUpgradeableAttachedClusters(stackViewV4Responses))
                .thenReturn("There are attached Data Hub clusters that are non-upgradeable");

        UpgradeV4Response actual = underTest.checkForClusterUpgrade(ACCOUNT_ID, stack, WORKSPACE_ID, request);

        assertEquals(expectedResponse, actual);
        verify(instanceGroupService).getByStackAndFetchTemplates(STACK_ID);
        verify(upgradeService).isOsUpgrade(request);
        verify(instanceGroupService).getByStackAndFetchTemplates(STACK_ID);
        verify(limitConfiguration).getUpgradeNodeCountLimit();
        verify(clusterUpgradeAvailabilityService).checkForUpgradesByName(stack, false, true, request.getInternalUpgradeSettings());
        verify(clusterUpgradeAvailabilityService).filterUpgradeOptions(ACCOUNT_ID, upgradeResponseToReturn, request, true);
        verify(entitlementService).runtimeUpgradeEnabled(ACCOUNT_ID);
        verify(stackOperations).listByEnvironmentCrn(eq(WORKSPACE_ID), eq(ENVIRONMENT_CRN), any());
        verify(upgradePreconditionService).checkForRunningAttachedClusters(stackViewV4Responses, stack);
        verify(upgradePreconditionService).checkForNonUpgradeableAttachedClusters(stackViewV4Responses);
        verifyNoInteractions(clusterDBValidationService);
    }

    @Test
    void testCheckForClusterUpgradeShouldNotValidateUpgradeableDataHubsWhenDataHubUpgradeEntitlementIsGranted() {
        Stack stack = createStack(StackType.DATALAKE);
        StackViewV4Responses stackViewV4Responses = new StackViewV4Responses();
        UpgradeV4Request request = createUpgradeRequest(null);
        UpgradeV4Response upgradeResponseToReturn = createUpgradeResponse();
        UpgradeV4Response expectedResponse = createUpgradeResponse();
        expectedResponse.setReason("There are attached Data Hub clusters in incorrect state");
        when(instanceGroupService.getByStackAndFetchTemplates(STACK_ID)).thenReturn(Collections.emptySet());
        when(upgradeService.isOsUpgrade(request)).thenReturn(false);
        StackInstanceCount instanceCount = mock(StackInstanceCount.class);
        when(instanceCount.getInstanceCount()).thenReturn(INSTANCE_COUNT);
        when(instanceMetaDataService.countByStackId(STACK_ID)).thenReturn(instanceCount);
        when(limitConfiguration.getUpgradeNodeCountLimit()).thenReturn(NODE_LIMIT);
        when(clusterUpgradeAvailabilityService.checkForUpgradesByName(stack, false, true, request.getInternalUpgradeSettings()))
                .thenReturn(upgradeResponseToReturn);
        when(entitlementService.runtimeUpgradeEnabled(ACCOUNT_ID)).thenReturn(true);
        when(entitlementService.datahubRuntimeUpgradeEnabled(ACCOUNT_ID)).thenReturn(true);
        when(stackOperations.listByEnvironmentCrn(eq(WORKSPACE_ID), eq(ENVIRONMENT_CRN), any())).thenReturn(stackViewV4Responses);
        when(upgradePreconditionService.checkForRunningAttachedClusters(stackViewV4Responses, stack))
                .thenReturn("There are attached Data Hub clusters in incorrect state");

        UpgradeV4Response actual = underTest.checkForClusterUpgrade(ACCOUNT_ID, stack, WORKSPACE_ID, request);

        assertEquals(expectedResponse, actual);
        verify(instanceGroupService).getByStackAndFetchTemplates(STACK_ID);
        verify(upgradeService).isOsUpgrade(request);
        verify(instanceGroupService).getByStackAndFetchTemplates(STACK_ID);
        verify(limitConfiguration).getUpgradeNodeCountLimit();
        verify(clusterUpgradeAvailabilityService).checkForUpgradesByName(stack, false, true, request.getInternalUpgradeSettings());
        verify(clusterUpgradeAvailabilityService).filterUpgradeOptions(ACCOUNT_ID, upgradeResponseToReturn, request, true);
        verify(entitlementService).runtimeUpgradeEnabled(ACCOUNT_ID);
        verify(stackOperations).listByEnvironmentCrn(eq(WORKSPACE_ID), eq(ENVIRONMENT_CRN), any());
        verify(upgradePreconditionService).checkForRunningAttachedClusters(stackViewV4Responses, stack);
        verify(upgradePreconditionService, times(0)).checkForNonUpgradeableAttachedClusters(stackViewV4Responses);
        verifyNoInteractions(clusterDBValidationService);
    }

    private UpgradeV4Response createUpgradeResponse() {
        UpgradeV4Response upgradeResponse = new UpgradeV4Response();
        upgradeResponse.setUpgradeCandidates(List.of(new ImageInfoV4Response()));
        return upgradeResponse;
    }

    private UpgradeV4Request createUpgradeRequest(Boolean replaceVm) {
        UpgradeV4Request upgradeRequest = new UpgradeV4Request();
        upgradeRequest.setReplaceVms(replaceVm);
        upgradeRequest.setInternalUpgradeSettings(new InternalUpgradeSettings(true, true, true));
        return upgradeRequest;
    }

    private Stack createStack(StackType stackType) {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setType(stackType);
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        return stack;
    }

}
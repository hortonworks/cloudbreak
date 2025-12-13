package com.sequenceiq.cloudbreak.service.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.domain.StopRestrictionReason;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.spot.SpotInstanceUsageCondition;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackStopRestrictionService;

@ExtendWith(MockitoExtension.class)
class UpgradePreconditionServiceTest {

    private static final String ACCOUNT_ID = "accountId";

    private static final String ENVIRONMENT_CRN = "env-crn";

    @InjectMocks
    private UpgradePreconditionService underTest;

    @Mock
    private SpotInstanceUsageCondition spotInstanceUsageCondition;

    @Mock
    private StackStopRestrictionService stackStopRestrictionService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private StackDtoService stackDtoService;

    private StackDto stackDto = new StackDto();

    @Test
    void testCheckForRunningAttachedClustersShouldReturnErrorMessageWhenThereAreClustersInNotProperState() {
        StackDto dataHubStack1 = createStackDtoDelegate(Status.AVAILABLE, "stack-1", "stack-crn-1");
        StackDto dataHubStack2 = createStackDtoDelegate(Status.DELETE_COMPLETED, "stack-2", "stack-crn-2");
        StackDto dataHubStack3 = createStackDtoDelegate(Status.STOPPED, "stack-3", "stack-crn-2");
        List<StackDto> datahubs = List.of(dataHubStack1, dataHubStack2, dataHubStack3);
        when(stackDtoService.findAllByEnvironmentCrnAndStackType(ENVIRONMENT_CRN, List.of(StackType.WORKLOAD))).thenReturn(datahubs);

        String actualRunning = underTest.checkForRunningAttachedClusters(ENVIRONMENT_CRN, null, false, ACCOUNT_ID);

        assertEquals("There are attached Data Hub clusters in incorrect state: stack-1. Please stop those to be able to perform the upgrade.", actualRunning);

        verify(spotInstanceUsageCondition).isStackRunsOnSpotInstances(dataHubStack1);
    }

    @Test
    void testCheckForRunningAttachedClustersShouldNotReturnErrorMessageWhenEntitlementIsTrue() {
        when(entitlementService.isUpgradeAttachedDatahubsCheckSkipped(ACCOUNT_ID)).thenReturn(Boolean.TRUE);

        StackDto dataHubStack1 = createStackDtoDelegate(Status.AVAILABLE, "stack-1", "stack-crn-1");
        StackDto dataHubStack2 = createStackDtoDelegate(Status.DELETE_COMPLETED, "stack-2", "stack-crn-2");
        StackDto dataHubStack3 = createStackDtoDelegate(Status.STOPPED, "stack-3", "stack-crn-2");
        List<StackDto> datahubs = List.of(dataHubStack1, dataHubStack2, dataHubStack3);
        when(stackDtoService.findAllByEnvironmentCrnAndStackType(ENVIRONMENT_CRN, List.of(StackType.WORKLOAD))).thenReturn(datahubs);

        String actual = underTest.checkForRunningAttachedClusters(ENVIRONMENT_CRN, null, false, ACCOUNT_ID);

        assertEquals("", actual);

        verify(spotInstanceUsageCondition).isStackRunsOnSpotInstances(dataHubStack1);
    }

    @Test
    void testCheckForRunningAttachedClustersShouldNotReturnErrorMessageWhenSkipDataHubValidationParameterIsTrue() {
        StackDto dataHubStack1 = createStackDtoDelegate(Status.AVAILABLE, "stack-1", "stack-crn-1");
        StackDto dataHubStack2 = createStackDtoDelegate(Status.DELETE_COMPLETED, "stack-2", "stack-crn-2");
        StackDto dataHubStack3 = createStackDtoDelegate(Status.STOPPED, "stack-3", "stack-crn-2");
        List<StackDto> datahubs = List.of(dataHubStack1, dataHubStack2, dataHubStack3);
        when(stackDtoService.findAllByEnvironmentCrnAndStackType(ENVIRONMENT_CRN, List.of(StackType.WORKLOAD))).thenReturn(datahubs);

        String actual = underTest.checkForRunningAttachedClusters(ENVIRONMENT_CRN, Boolean.TRUE, false, ACCOUNT_ID);

        assertEquals("", actual);

        verify(spotInstanceUsageCondition).isStackRunsOnSpotInstances(dataHubStack1);
        verifyNoInteractions(entitlementService);
    }

    @Test
    void testCheckForRunningAttachedClustersShouldNotReturnErrorMessageWhenRollingUpgradeIsEnabled() {
        StackDto dataHubStack1 = createStackDtoDelegate(Status.AVAILABLE, "stack-1", "stack-crn-1");
        StackDto dataHubStack2 = createStackDtoDelegate(Status.DELETE_COMPLETED, "stack-2", "stack-crn-2");
        StackDto dataHubStack3 = createStackDtoDelegate(Status.STOPPED, "stack-3", "stack-crn-2");
        List<StackDto> datahubs = List.of(dataHubStack1, dataHubStack2, dataHubStack3);
        when(stackDtoService.findAllByEnvironmentCrnAndStackType(ENVIRONMENT_CRN, List.of(StackType.WORKLOAD))).thenReturn(datahubs);

        String actual = underTest.checkForRunningAttachedClusters(ENVIRONMENT_CRN, Boolean.FALSE, true, ACCOUNT_ID);

        assertEquals("", actual);

        verify(spotInstanceUsageCondition).isStackRunsOnSpotInstances(dataHubStack1);
        verifyNoInteractions(entitlementService);
    }

    @Test
    void testCheckForRunningAttachedClustersShouldNotReturnErrorMessageWhenThereAreOnlyOneClusterIsRunningWithEphemeralVolume() {
        StackDto dataHubStack1 = createStackDtoDelegate(Status.AVAILABLE, "stack-1", "stack-crn-1");
        StackDto dataHubStack2 = createStackDtoDelegate(Status.DELETE_COMPLETED, "stack-2", "stack-crn-2");
        StackDto dataHubStack3 = createStackDtoDelegate(Status.STOPPED, "stack-3", "stack-crn-3");
        List<StackDto> datahubs = List.of(dataHubStack1, dataHubStack2, dataHubStack3);
        when(stackDtoService.findAllByEnvironmentCrnAndStackType(ENVIRONMENT_CRN, List.of(StackType.WORKLOAD))).thenReturn(datahubs);
        when(stackStopRestrictionService.isInfrastructureStoppable(any())).thenReturn(StopRestrictionReason.EPHEMERAL_VOLUMES);

        String actual = underTest.checkForRunningAttachedClusters(ENVIRONMENT_CRN, Boolean.FALSE, false, ACCOUNT_ID);

        assertEquals("", actual);

        verifyNoInteractions(spotInstanceUsageCondition);
    }

    @Test
    void testCheckForRunningAttachedClustersShouldNotReturnErrorMessageWhenThereAreOnlyOneClusterIsRunningWithSpotInstance() {
        StackDto dataHubStack1 = createStackDtoDelegate(Status.AVAILABLE, "stack-1", "stack-crn-1");
        StackDto dataHubStack2 = createStackDtoDelegate(Status.DELETE_COMPLETED, "stack-2", "stack-crn-2");
        StackDto dataHubStack3 = createStackDtoDelegate(Status.STOPPED, "stack-3", "stack-crn-3");
        List<StackDto> datahubs = List.of(dataHubStack1, dataHubStack2, dataHubStack3);
        when(stackDtoService.findAllByEnvironmentCrnAndStackType(ENVIRONMENT_CRN, List.of(StackType.WORKLOAD))).thenReturn(datahubs);
        when(stackStopRestrictionService.isInfrastructureStoppable(any())).thenReturn(StopRestrictionReason.NONE);
        when(spotInstanceUsageCondition.isStackRunsOnSpotInstances(dataHubStack1)).thenReturn(true);

        String actualRunning = underTest.checkForRunningAttachedClusters(ENVIRONMENT_CRN, Boolean.FALSE, false, ACCOUNT_ID);

        assertEquals("", actualRunning);

        verify(spotInstanceUsageCondition).isStackRunsOnSpotInstances(dataHubStack1);
    }

    @Test
    void testCheckForUpgradeableAttachedClustersShouldReturnErrorMessageWhenThereIsNoNonUpgradeableCluster() {
        StackDto dataHubStack1 = createStackDtoDelegate(Status.STOPPED, "stack-1", "stack-crn-1");
        StackDto dataHubStack2 = createStackDtoDelegate(Status.DELETE_COMPLETED, "stack-2", "stack-crn-2");
        List<StackDto> datahubs = List.of(dataHubStack1, dataHubStack2);
        when(stackDtoService.findAllByEnvironmentCrnAndStackType(ENVIRONMENT_CRN, List.of(StackType.WORKLOAD))).thenReturn(datahubs);

        String actualRunning = underTest.checkForRunningAttachedClusters(ENVIRONMENT_CRN, Boolean.FALSE, false, ACCOUNT_ID);

        assertEquals("", actualRunning);
    }

    @Test
    void testCheckForUpgradeableAttachedClustersShouldNotReturnErrorMessageWhenThereIsNoNonUpgradeableCluster() {
        StackDto dataHubStack1 = createStackDtoDelegate(Status.STOPPED, "stack-1", "stack-crn-1");
        StackDto dataHubStack2 = createStackDtoDelegate(Status.DELETE_COMPLETED, "stack-2", "stack-crn-2");
        StackDto dataHubStack3 = createStackDtoDelegate(Status.STOPPED, "stack-3", "stack-crn-3");
        List<StackDto> datahubs = List.of(dataHubStack1, dataHubStack2, dataHubStack3);
        when(stackDtoService.findAllByEnvironmentCrnAndStackType(ENVIRONMENT_CRN, List.of(StackType.WORKLOAD))).thenReturn(datahubs);

        String actual = underTest.checkForRunningAttachedClusters(ENVIRONMENT_CRN, Boolean.FALSE, false, ACCOUNT_ID);

        assertEquals("", actual);

        verifyNoInteractions(spotInstanceUsageCondition);
    }

    @Test
    void testCheckForAttachedClustersShouldReturnBothErrorMessagesWhenBothValidationsFail() {
        StackDto dataHubStack1 = createStackDtoDelegate(Status.AVAILABLE, "stack-1", "stack-crn-1");
        StackDto dataHubStack2 = createStackDtoDelegate(Status.DELETE_COMPLETED, "stack-2", "stack-crn-2");
        StackDto dataHubStack3 = createStackDtoDelegate(Status.STOPPED, "stack-3", "stack-crn-3");
        List<StackDto> datahubs = List.of(dataHubStack1, dataHubStack2, dataHubStack3);
        when(stackDtoService.findAllByEnvironmentCrnAndStackType(ENVIRONMENT_CRN, List.of(StackType.WORKLOAD))).thenReturn(datahubs);

        String actualRunning = underTest.checkForRunningAttachedClusters(ENVIRONMENT_CRN, Boolean.FALSE, false, ACCOUNT_ID);

        assertEquals("There are attached Data Hub clusters in incorrect state: stack-1. Please stop those to be able to perform the upgrade.", actualRunning);

        verify(spotInstanceUsageCondition).isStackRunsOnSpotInstances(dataHubStack1);
    }

    @Test
    void testCheckClustersShouldNotReturnErrorMessageForCustomBlueprints() {
        StackDto dataHubStack1 = createStackDtoDelegate(Status.AVAILABLE, "stack-1", "stack-crn-1");
        StackDto dataHubStack2 = createStackDtoDelegate(Status.DELETE_COMPLETED, "stack-2", "stack-crn-2");
        StackDto dataHubStack3 = createStackDtoDelegate(Status.STOPPED, "stack-3", "stack-crn-3");
        List<StackDto> datahubs = List.of(dataHubStack1, dataHubStack2, dataHubStack3);
        when(stackDtoService.findAllByEnvironmentCrnAndStackType(ENVIRONMENT_CRN, List.of(StackType.WORKLOAD))).thenReturn(datahubs);

        String actualRunning = underTest.checkForRunningAttachedClusters(ENVIRONMENT_CRN, Boolean.FALSE, false, ACCOUNT_ID);

        assertEquals("There are attached Data Hub clusters in incorrect state: stack-1. Please stop those to be able to perform the upgrade.", actualRunning);

        verify(spotInstanceUsageCondition).isStackRunsOnSpotInstances(dataHubStack1);
    }

    @Test
    void testDataHubsNotAttached() {
        List<StackDto> datahubs = List.of();
        when(stackDtoService.findAllByEnvironmentCrnAndStackType(ENVIRONMENT_CRN, List.of(StackType.WORKLOAD))).thenReturn(datahubs);

        String actual = underTest.checkForRunningAttachedClusters(ENVIRONMENT_CRN, Boolean.FALSE, false, ACCOUNT_ID);

        assertEquals("", actual);

        verifyNoInteractions(spotInstanceUsageCondition);
    }

    private StackDto createStackDtoDelegate(Status stackStatus, String stackName, String stackCrn) {
        StackDto dataHubStack = mock(StackDto.class);
        when(dataHubStack.getStatus()).thenReturn(stackStatus);
        when(dataHubStack.getName()).thenReturn(stackName);
        lenient().when(dataHubStack.getResourceCrn()).thenReturn(stackCrn);
        return dataHubStack;
    }
}
package com.sequenceiq.cloudbreak.service.upgrade;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.domain.StopRestrictionReason;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.spot.SpotInstanceUsageCondition;
import com.sequenceiq.cloudbreak.service.stack.StackStopRestrictionService;

@RunWith(MockitoJUnitRunner.class)
public class UpgradePreconditionServiceTest {

    private static final String ACCOUNT_ID = "accountId";

    @InjectMocks
    private UpgradePreconditionService underTest;

    @Mock
    private SpotInstanceUsageCondition spotInstanceUsageCondition;

    @Mock
    private StackStopRestrictionService stackStopRestrictionService;

    @Mock
    private EntitlementService entitlementService;

    @Test
    public void testCheckForRunningAttachedClustersShouldReturnErrorMessageWhenThereAreClustersInNotProperState() {
        StackDtoDelegate dataHubStack1 = createStackDtoDelegate(Status.AVAILABLE, "stack-1", "stack-crn-1");
        StackDtoDelegate dataHubStack2 = createStackDtoDelegate(Status.DELETE_COMPLETED, "stack-2", "stack-crn-2");
        StackDtoDelegate dataHubStack3 = createStackDtoDelegate(Status.STOPPED, "stack-3", "stack-crn-2");
        List<StackDtoDelegate> datahubs = List.of(dataHubStack1, dataHubStack2, dataHubStack3);

        String actualRunning = underTest.checkForRunningAttachedClusters(datahubs, null, false, ACCOUNT_ID);

        assertEquals("There are attached Data Hub clusters in incorrect state: stack-1. Please stop those to be able to perform the upgrade.", actualRunning);

        verify(spotInstanceUsageCondition).isStackRunsOnSpotInstances(dataHubStack1);
    }

    @Test
    public void testCheckForRunningAttachedClustersShouldNotReturnErrorMessageWhenEntitlementIsTrue() {
        when(entitlementService.isUpgradeAttachedDatahubsCheckSkipped(ACCOUNT_ID)).thenReturn(Boolean.TRUE);

        StackDtoDelegate dataHubStack1 = createStackDtoDelegate(Status.AVAILABLE, "stack-1", "stack-crn-1");
        StackDtoDelegate dataHubStack2 = createStackDtoDelegate(Status.DELETE_COMPLETED, "stack-2", "stack-crn-2");
        StackDtoDelegate dataHubStack3 = createStackDtoDelegate(Status.STOPPED, "stack-3", "stack-crn-2");
        List<StackDtoDelegate> datahubs = List.of(dataHubStack1, dataHubStack2, dataHubStack3);

        String actual = underTest.checkForRunningAttachedClusters(datahubs, null, false, ACCOUNT_ID);

        assertEquals("", actual);

        verify(spotInstanceUsageCondition).isStackRunsOnSpotInstances(dataHubStack1);
    }

    @Test
    public void testCheckForRunningAttachedClustersShouldNotReturnErrorMessageWhenSkipDataHubValidationParameterIsTrue() {
        StackDtoDelegate dataHubStack1 = createStackDtoDelegate(Status.AVAILABLE, "stack-1", "stack-crn-1");
        StackDtoDelegate dataHubStack2 = createStackDtoDelegate(Status.DELETE_COMPLETED, "stack-2", "stack-crn-2");
        StackDtoDelegate dataHubStack3 = createStackDtoDelegate(Status.STOPPED, "stack-3", "stack-crn-2");
        List<StackDtoDelegate> datahubs = List.of(dataHubStack1, dataHubStack2, dataHubStack3);

        String actual = underTest.checkForRunningAttachedClusters(datahubs, Boolean.TRUE, false, ACCOUNT_ID);

        assertEquals("", actual);

        verify(spotInstanceUsageCondition).isStackRunsOnSpotInstances(dataHubStack1);
        verifyNoInteractions(entitlementService);
    }

    @Test
    public void testCheckForRunningAttachedClustersShouldNotReturnErrorMessageWhenRollingUpgradeIsEnabled() {
        StackDtoDelegate dataHubStack1 = createStackDtoDelegate(Status.AVAILABLE, "stack-1", "stack-crn-1");
        StackDtoDelegate dataHubStack2 = createStackDtoDelegate(Status.DELETE_COMPLETED, "stack-2", "stack-crn-2");
        StackDtoDelegate dataHubStack3 = createStackDtoDelegate(Status.STOPPED, "stack-3", "stack-crn-2");
        List<StackDtoDelegate> datahubs = List.of(dataHubStack1, dataHubStack2, dataHubStack3);

        String actual = underTest.checkForRunningAttachedClusters(datahubs, Boolean.FALSE, true, ACCOUNT_ID);

        assertEquals("", actual);

        verify(spotInstanceUsageCondition).isStackRunsOnSpotInstances(dataHubStack1);
        verifyNoInteractions(entitlementService);
    }

    @Test
    public void testCheckForRunningAttachedClustersShouldNotReturnErrorMessageWhenThereAreOnlyOneClusterIsRunningWithEphemeralVolume() {
        StackDtoDelegate dataHubStack1 = createStackDtoDelegate(Status.AVAILABLE, "stack-1", "stack-crn-1");
        StackDtoDelegate dataHubStack2 = createStackDtoDelegate(Status.DELETE_COMPLETED, "stack-2", "stack-crn-2");
        StackDtoDelegate dataHubStack3 = createStackDtoDelegate(Status.STOPPED, "stack-3", "stack-crn-3");

        List<StackDtoDelegate> datahubs = List.of(dataHubStack1, dataHubStack2, dataHubStack3);
        when(stackStopRestrictionService.isInfrastructureStoppable(any())).thenReturn(StopRestrictionReason.EPHEMERAL_VOLUMES);

        String actual = underTest.checkForRunningAttachedClusters(datahubs, Boolean.FALSE, false, ACCOUNT_ID);

        assertEquals("", actual);

        verifyNoInteractions(spotInstanceUsageCondition);
    }

    @Test
    public void testCheckForRunningAttachedClustersShouldNotReturnErrorMessageWhenThereAreOnlyOneClusterIsRunningWithSpotInstance() {
        StackDtoDelegate dataHubStack1 = createStackDtoDelegate(Status.AVAILABLE, "stack-1", "stack-crn-1");
        StackDtoDelegate dataHubStack2 = createStackDtoDelegate(Status.DELETE_COMPLETED, "stack-2", "stack-crn-2");
        StackDtoDelegate dataHubStack3 = createStackDtoDelegate(Status.STOPPED, "stack-3", "stack-crn-3");
        List<StackDtoDelegate> datahubs = List.of(dataHubStack1, dataHubStack2, dataHubStack3);
        when(stackStopRestrictionService.isInfrastructureStoppable(any())).thenReturn(StopRestrictionReason.NONE);
        when(spotInstanceUsageCondition.isStackRunsOnSpotInstances(dataHubStack1)).thenReturn(true);

        String actualRunning = underTest.checkForRunningAttachedClusters(datahubs, Boolean.FALSE, false, ACCOUNT_ID);

        assertEquals("", actualRunning);

        verify(spotInstanceUsageCondition).isStackRunsOnSpotInstances(dataHubStack1);
    }

    @Test
    public void testCheckForUpgradeableAttachedClustersShouldReturnErrorMessageWhenThereIsNoNonUpgradeableCluster() {
        StackDtoDelegate dataHubStack1 = createStackDtoDelegate(Status.STOPPED, "stack-1", "stack-crn-1");
        StackDtoDelegate dataHubStack2 = createStackDtoDelegate(Status.DELETE_COMPLETED, "stack-2", "stack-crn-2");
        List<StackDtoDelegate> datahubs = List.of(dataHubStack1, dataHubStack2);

        String actualRunning = underTest.checkForRunningAttachedClusters(datahubs, Boolean.FALSE, false, ACCOUNT_ID);

        assertEquals("", actualRunning);
    }

    @Test
    public void testCheckForUpgradeableAttachedClustersShouldNotReturnErrorMessageWhenThereIsNoNonUpgradeableCluster() {
        StackDtoDelegate dataHubStack1 = createStackDtoDelegate(Status.STOPPED, "stack-1", "stack-crn-1");
        StackDtoDelegate dataHubStack2 = createStackDtoDelegate(Status.DELETE_COMPLETED, "stack-2", "stack-crn-2");
        StackDtoDelegate dataHubStack3 = createStackDtoDelegate(Status.STOPPED, "stack-3", "stack-crn-3");
        List<StackDtoDelegate> datahubs = List.of(dataHubStack1, dataHubStack2, dataHubStack3);

        String actual = underTest.checkForRunningAttachedClusters(datahubs, Boolean.FALSE, false, ACCOUNT_ID);

        assertEquals("", actual);

        verifyNoInteractions(spotInstanceUsageCondition);
    }

    @Test
    public void testCheckForAttachedClustersShouldReturnBothErrorMessagesWhenBothValidationsFail() {
        StackDtoDelegate dataHubStack1 = createStackDtoDelegate(Status.AVAILABLE, "stack-1", "stack-crn-1");
        StackDtoDelegate dataHubStack2 = createStackDtoDelegate(Status.DELETE_COMPLETED, "stack-2", "stack-crn-2");
        StackDtoDelegate dataHubStack3 = createStackDtoDelegate(Status.STOPPED, "stack-3", "stack-crn-3");
        List<StackDtoDelegate> datahubs = List.of(dataHubStack1, dataHubStack2, dataHubStack3);

        String actualRunning = underTest.checkForRunningAttachedClusters(datahubs, Boolean.FALSE, false, ACCOUNT_ID);

        assertEquals("There are attached Data Hub clusters in incorrect state: stack-1. Please stop those to be able to perform the upgrade.",
                actualRunning);

        verify(spotInstanceUsageCondition).isStackRunsOnSpotInstances(dataHubStack1);
    }

    @Test
    public void testCheckClustersShouldNotReturnErrorMessageForCustomBlueprints() {
        StackDtoDelegate dataHubStack1 = createStackDtoDelegate(Status.AVAILABLE, "stack-1", "stack-crn-1");
        StackDtoDelegate dataHubStack2 = createStackDtoDelegate(Status.DELETE_COMPLETED, "stack-2", "stack-crn-2");
        StackDtoDelegate dataHubStack3 = createStackDtoDelegate(Status.STOPPED, "stack-3", "stack-crn-3");
        List<StackDtoDelegate> datahubs = List.of(dataHubStack1, dataHubStack2, dataHubStack3);

        String actualRunning = underTest.checkForRunningAttachedClusters(datahubs, Boolean.FALSE, false, ACCOUNT_ID);

        assertEquals("There are attached Data Hub clusters in incorrect state: stack-1. Please stop those to be able to perform the upgrade.",
                actualRunning);

        verify(spotInstanceUsageCondition).isStackRunsOnSpotInstances(dataHubStack1);
    }

    @Test
    public void testDataHubsNotAttached() {
        List<StackDtoDelegate> datahubs = List.of();

        String actual = underTest.checkForRunningAttachedClusters(datahubs, Boolean.FALSE, false, ACCOUNT_ID);

        assertEquals("", actual);

        verifyNoInteractions(spotInstanceUsageCondition);
    }

    private StackDtoDelegate createStackDtoDelegate(Status stackStatus, String stackName, String stackCrn) {
        StackDtoDelegate dataHubStack = mock(StackDtoDelegate.class);
        when(dataHubStack.getStatus()).thenReturn(stackStatus);
        when(dataHubStack.getName()).thenReturn(stackName);
        lenient().when(dataHubStack.getResourceCrn()).thenReturn(stackCrn);
        return dataHubStack;
    }
}
package com.sequenceiq.cloudbreak.service.upgrade;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4ViewResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.BlueprintBasedUpgradeOption;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.ClusterViewV4Response;
import com.sequenceiq.cloudbreak.domain.StopRestrictionReason;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.spot.SpotInstanceUsageCondition;
import com.sequenceiq.cloudbreak.service.stack.StackStopRestrictionService;

@RunWith(MockitoJUnitRunner.class)
public class UpgradePreconditionServiceTest {

    @InjectMocks
    private UpgradePreconditionService underTest;

    @Mock
    private SpotInstanceUsageCondition spotInstanceUsageCondition;

    @Mock
    private StackStopRestrictionService stackStopRestrictionService;

    @Test
    public void testCheckForRunningAttachedClustersShouldReturnErrorMessageWhenThereAreClustersInNotProperState() {
        StackViewV4Response dataHubStack1 = createStackResponse(Status.AVAILABLE, "stack-1", "stack-crn-1");
        dataHubStack1.setCluster(createClusterResponse(Status.AVAILABLE, BlueprintBasedUpgradeOption.UPGRADE_ENABLED));
        StackViewV4Response dataHubStack2 = createStackResponse(Status.DELETE_COMPLETED, "stack-2", "stack-crn-2");
        dataHubStack2.setCluster(createClusterResponse(Status.DELETE_COMPLETED, BlueprintBasedUpgradeOption.UPGRADE_ENABLED));
        StackViewV4Response dataHubStack3 = createStackResponse(Status.STOPPED, "stack-3", "stack-crn-2");
        dataHubStack3.setCluster(createClusterResponse(Status.DELETE_COMPLETED, BlueprintBasedUpgradeOption.UPGRADE_ENABLED));
        StackViewV4Responses stackViewV4Responses = new StackViewV4Responses(Set.of(dataHubStack1, dataHubStack2, dataHubStack3));
        Stack stack = new Stack();

        String actualRunning = underTest.checkForRunningAttachedClusters(stackViewV4Responses, stack);
        String actualNonUpgradeable = underTest.checkForNonUpgradeableAttachedClusters(stackViewV4Responses);

        assertEquals("There are attached Data Hub clusters in incorrect state: stack-1. Please stop those to be able to perform the upgrade.",
                actualRunning);
        assertEquals("", actualNonUpgradeable);

        verify(spotInstanceUsageCondition).isStackRunsOnSpotInstances(stack);
    }

    @Test
    public void testCheckForRunningAttachedClustersShouldNotReturnErrorMessageWhenThereAreNoClustersInNotProperState() {
        StackViewV4Response dataHubStack1 = createStackResponse(Status.STOPPED, "stack-1", "stack-crn-1");
        dataHubStack1.setCluster(createClusterResponse(Status.STOPPED, BlueprintBasedUpgradeOption.UPGRADE_ENABLED));
        StackViewV4Response dataHubStack2 = createStackResponse(Status.DELETE_COMPLETED, "stack-2", "stack-crn-2");
        dataHubStack2.setCluster(createClusterResponse(Status.DELETE_COMPLETED, BlueprintBasedUpgradeOption.UPGRADE_ENABLED));
        StackViewV4Response dataHubStack3 = createStackResponse(Status.STOPPED, "stack-3", "stack-crn-3");
        dataHubStack3.setCluster(createClusterResponse(Status.DELETE_COMPLETED, BlueprintBasedUpgradeOption.UPGRADE_ENABLED));

        StackViewV4Responses stackViewV4Responses = new StackViewV4Responses(Set.of(dataHubStack1, dataHubStack2, dataHubStack3));

        String actualRunning = underTest.checkForRunningAttachedClusters(stackViewV4Responses, new Stack());
        String actualNonUpgradeable = underTest.checkForNonUpgradeableAttachedClusters(stackViewV4Responses);

        assertEquals("", actualRunning);
        assertEquals("", actualNonUpgradeable);

        verifyNoInteractions(spotInstanceUsageCondition);
    }

    @Test
    public void testCheckForRunningAttachedClustersShouldNotReturnErrorMessageWhenThereAreOnlyOneClusterIsRunningWithEphemeralVolume() {
        StackViewV4Response dataHubStack1 = createStackResponse(Status.AVAILABLE, "stack-1", "stack-crn-1");
        dataHubStack1.setCluster(createClusterResponse(Status.AVAILABLE, BlueprintBasedUpgradeOption.UPGRADE_ENABLED));
        StackViewV4Response dataHubStack2 = createStackResponse(Status.DELETE_COMPLETED, "stack-2", "stack-crn-2");
        dataHubStack2.setCluster(createClusterResponse(Status.DELETE_COMPLETED, BlueprintBasedUpgradeOption.UPGRADE_ENABLED));
        StackViewV4Response dataHubStack3 = createStackResponse(Status.STOPPED, "stack-3", "stack-crn-3");
        dataHubStack3.setCluster(createClusterResponse(Status.DELETE_COMPLETED, BlueprintBasedUpgradeOption.UPGRADE_ENABLED));

        StackViewV4Responses stackViewV4Responses = new StackViewV4Responses(Set.of(dataHubStack1, dataHubStack2, dataHubStack3));
        when(stackStopRestrictionService.isInfrastructureStoppable(any())).thenReturn(StopRestrictionReason.EPHEMERAL_VOLUMES);

        String actual = underTest.checkForRunningAttachedClusters(stackViewV4Responses, new Stack());
        String actualNonUpgradeable = underTest.checkForNonUpgradeableAttachedClusters(stackViewV4Responses);

        assertEquals("", actual);
        assertEquals("", actualNonUpgradeable);

        verifyNoInteractions(spotInstanceUsageCondition);
    }

    @Test
    public void testCheckForRunningAttachedClustersShouldNotReturnErrorMessageWhenThereAreOnlyOneClusterIsRunningWithSpotInstance() {
        StackViewV4Response dataHubStack1 = createStackResponse(Status.AVAILABLE, "stack-1", "stack-crn-1");
        dataHubStack1.setCluster(createClusterResponse(Status.AVAILABLE, BlueprintBasedUpgradeOption.UPGRADE_ENABLED));
        StackViewV4Response dataHubStack2 = createStackResponse(Status.DELETE_COMPLETED, "stack-2", "stack-crn-2");
        dataHubStack2.setCluster(createClusterResponse(Status.DELETE_COMPLETED, BlueprintBasedUpgradeOption.UPGRADE_ENABLED));
        StackViewV4Response dataHubStack3 = createStackResponse(Status.STOPPED, "stack-3", "stack-crn-3");
        dataHubStack3.setCluster(createClusterResponse(Status.DELETE_COMPLETED, BlueprintBasedUpgradeOption.UPGRADE_ENABLED));
        StackViewV4Responses stackViewV4Responses = new StackViewV4Responses(Set.of(dataHubStack1, dataHubStack2, dataHubStack3));
        Stack stack = new Stack();
        when(stackStopRestrictionService.isInfrastructureStoppable(any())).thenReturn(StopRestrictionReason.NONE);
        when(spotInstanceUsageCondition.isStackRunsOnSpotInstances(stack)).thenReturn(true);

        String actualRunning = underTest.checkForRunningAttachedClusters(stackViewV4Responses, stack);
        String actualNonUpgradeable = underTest.checkForNonUpgradeableAttachedClusters(stackViewV4Responses);

        assertEquals("", actualRunning);
        assertEquals("", actualNonUpgradeable);

        verify(spotInstanceUsageCondition).isStackRunsOnSpotInstances(stack);
    }

    @Test
    public void testCheckForUpgradeableAttachedClustersShouldReturnErrorMessageWhenThereIsNoNonUpgradeableCluster() {
        StackViewV4Response dataHubStack1 = createStackResponse(Status.STOPPED, "stack-1", "stack-crn-1");
        dataHubStack1.setCluster(createClusterResponse(Status.STOPPED, BlueprintBasedUpgradeOption.UPGRADE_ENABLED));
        StackViewV4Response dataHubStack2 = createStackResponse(Status.DELETE_COMPLETED, "stack-2", "stack-crn-2");
        dataHubStack2.setCluster(createClusterResponse(Status.DELETE_COMPLETED, BlueprintBasedUpgradeOption.UPGRADE_DISABLED));
        StackViewV4Responses stackViewV4Responses = new StackViewV4Responses(Set.of(dataHubStack1, dataHubStack2));
        Stack stack = new Stack();

        String actualRunning = underTest.checkForRunningAttachedClusters(stackViewV4Responses, stack);
        String actualNonUpgradeable = underTest.checkForNonUpgradeableAttachedClusters(stackViewV4Responses);

        assertEquals("There are attached Data Hub clusters that are non-upgradeable: stack-2. Please delete those to be able to perform the upgrade.",
                actualNonUpgradeable);
        assertEquals("", actualRunning);
    }

    @Test
    public void testCheckForUpgradeableAttachedClustersShouldNotReturnErrorMessageWhenThereIsNoNonUpgradeableCluster() {
        StackViewV4Response dataHubStack1 = createStackResponse(Status.STOPPED, "stack-1", "stack-crn-1");
        dataHubStack1.setCluster(createClusterResponse(Status.STOPPED, BlueprintBasedUpgradeOption.UPGRADE_ENABLED));
        StackViewV4Response dataHubStack2 = createStackResponse(Status.DELETE_COMPLETED, "stack-2", "stack-crn-2");
        dataHubStack2.setCluster(createClusterResponse(Status.DELETE_COMPLETED, BlueprintBasedUpgradeOption.UPGRADE_ENABLED));
        StackViewV4Response dataHubStack3 = createStackResponse(Status.STOPPED, "stack-3", "stack-crn-3");
        dataHubStack3.setCluster(createClusterResponse(Status.STOPPED, BlueprintBasedUpgradeOption.UPGRADE_ENABLED));
        StackViewV4Responses stackViewV4Responses = new StackViewV4Responses(Set.of(dataHubStack1, dataHubStack2, dataHubStack3));

        String actual = underTest.checkForRunningAttachedClusters(stackViewV4Responses, new Stack());
        String actualNonUpgradeable = underTest.checkForNonUpgradeableAttachedClusters(stackViewV4Responses);

        assertEquals("", actual);
        assertEquals("", actualNonUpgradeable);

        verifyNoInteractions(spotInstanceUsageCondition);
    }

    @Test
    public void testCheckForAttachedClustersShouldReturnBothErrorMessagesWhenBothValidationsFail() {
        StackViewV4Response dataHubStack1 = createStackResponse(Status.AVAILABLE, "stack-1", "stack-crn-1");
        dataHubStack1.setCluster(createClusterResponse(Status.AVAILABLE, BlueprintBasedUpgradeOption.UPGRADE_ENABLED));
        StackViewV4Response dataHubStack2 = createStackResponse(Status.DELETE_COMPLETED, "stack-2", "stack-crn-2");
        dataHubStack2.setCluster(createClusterResponse(Status.DELETE_COMPLETED, BlueprintBasedUpgradeOption.UPGRADE_DISABLED));
        StackViewV4Response dataHubStack3 = createStackResponse(Status.STOPPED, "stack-3", "stack-crn-2");
        StackViewV4Responses stackViewV4Responses = new StackViewV4Responses(Set.of(dataHubStack1, dataHubStack2, dataHubStack3));
        Stack stack = new Stack();

        String actualRunning = underTest.checkForRunningAttachedClusters(stackViewV4Responses, stack);
        String actualNonUpgradeable = underTest.checkForNonUpgradeableAttachedClusters(stackViewV4Responses);

        assertEquals("There are attached Data Hub clusters in incorrect state: stack-1. Please stop those to be able to perform the upgrade.",
                actualRunning);
        assertEquals("There are attached Data Hub clusters that are non-upgradeable: stack-2,stack-3. Please delete those to be able to perform the upgrade.",
                actualNonUpgradeable);

        verify(spotInstanceUsageCondition).isStackRunsOnSpotInstances(stack);
    }

    @Test
    public void testDataHubsNotAttached() {
        StackViewV4Responses stackViewV4Responses = new StackViewV4Responses(Set.of());

        String actual = underTest.checkForRunningAttachedClusters(stackViewV4Responses, new Stack());
        String actualNonUpgradeable = underTest.checkForNonUpgradeableAttachedClusters(stackViewV4Responses);

        assertEquals("", actual);
        assertEquals("", actualNonUpgradeable);

        verifyNoInteractions(spotInstanceUsageCondition);
    }

    private ClusterViewV4Response createClusterResponse(Status clusterStatus, BlueprintBasedUpgradeOption upgradeable) {
        ClusterViewV4Response dataHubCluster = new ClusterViewV4Response();
        dataHubCluster.setStatus(clusterStatus);
        BlueprintV4ViewResponse blueprint = new BlueprintV4ViewResponse();
        blueprint.setUpgradeable(upgradeable);
        dataHubCluster.setBlueprint(blueprint);

        return dataHubCluster;
    }

    private StackViewV4Response createStackResponse(Status stackStatus, String stackName, String stackCrn) {
        StackViewV4Response dataHubStack = new StackViewV4Response();
        dataHubStack.setStatus(stackStatus);
        dataHubStack.setName(stackName);
        dataHubStack.setCrn(stackCrn);
        return dataHubStack;
    }
}
package com.sequenceiq.cloudbreak.service.upgrade;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.ClusterViewV4Response;
import com.sequenceiq.cloudbreak.domain.StopRestrictionReason;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.spot.SpotInstanceUsageCondition;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.StackStopRestrictionService;

@RunWith(MockitoJUnitRunner.class)
public class UpgradePreconditionServiceTest {

    @InjectMocks
    private UpgradePreconditionService underTest;

    @Mock
    private StackService stackService;

    @Mock
    private SpotInstanceUsageCondition spotInstanceUsageCondition;

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private StackStopRestrictionService stackStopRestrictionService;

    @Test
    public void testCheckForRunningAttachedClustersShouldReturnErrorMessageWhenThereAreClustersInNotProperState() {
        StackViewV4Response dataHubStack1 = createStackResponse(Status.AVAILABLE, "stack-1", "stack-crn-1");
        dataHubStack1.setCluster(createClusterResponse(Status.AVAILABLE));
        StackViewV4Response dataHubStack2 = createStackResponse(Status.DELETE_COMPLETED, "stack-2", "stack-crn-2");
        dataHubStack2.setCluster(createClusterResponse(Status.DELETE_COMPLETED));
        StackViewV4Response dataHubStack3 = createStackResponse(Status.STOPPED, "stack-3", "stack-crn-2");
        StackViewV4Responses stackViewV4Responses = new StackViewV4Responses(Set.of(dataHubStack1, dataHubStack2, dataHubStack3));
        Stack stack = new Stack();
        when(stackService.getByCrn(dataHubStack1.getCrn())).thenReturn(stack);
        when(spotInstanceUsageCondition.isStackRunsOnSpotInstances(stack)).thenReturn(false);

        UpgradeV4Response actual = underTest.checkForRunningAttachedClusters(stackViewV4Responses, new UpgradeV4Response());

        assertNull(actual.getUpgradeCandidates());
        assertEquals("There are attached Data Hub clusters in incorrect state: stack-1. Please stop those to be able to perform the upgrade.",
                actual.getReason());
        verify(stackService).getByCrn(dataHubStack1.getCrn());
        verify(instanceGroupService).getByStackAndFetchTemplates(any());
        verify(spotInstanceUsageCondition).isStackRunsOnSpotInstances(stack);
    }

    @Test
    public void testCheckForRunningAttachedClustersShouldNotReturnErrorMessageWhenThereAreNoClustersInNotProperState() {
        StackViewV4Response dataHubStack1 = createStackResponse(Status.STOPPED, "stack-1", "stack-crn-1");
        dataHubStack1.setCluster(createClusterResponse(Status.STOPPED));
        StackViewV4Response dataHubStack2 = createStackResponse(Status.DELETE_COMPLETED, "stack-2", "stack-crn-2");
        dataHubStack2.setCluster(createClusterResponse(Status.DELETE_COMPLETED));
        StackViewV4Response dataHubStack3 = createStackResponse(Status.STOPPED, "stack-3", "stack-crn-3");
        StackViewV4Responses stackViewV4Responses = new StackViewV4Responses(Set.of(dataHubStack1, dataHubStack2, dataHubStack3));

        UpgradeV4Response actual = underTest.checkForRunningAttachedClusters(stackViewV4Responses, new UpgradeV4Response());

        assertNull(actual.getReason());
        verifyNoInteractions(stackService);
        verifyNoInteractions(instanceGroupService);
        verifyNoInteractions(spotInstanceUsageCondition);
    }

    @Test
    public void testCheckForRunningAttachedClustersShouldNotReturnErrorMessageWhenThereAreOnlyOneClusterIsRunningWithEphemeralVolume() {
        StackViewV4Response dataHubStack1 = createStackResponse(Status.AVAILABLE, "stack-1", "stack-crn-1");
        dataHubStack1.setCluster(createClusterResponse(Status.AVAILABLE));
        StackViewV4Response dataHubStack2 = createStackResponse(Status.DELETE_COMPLETED, "stack-2", "stack-crn-2");
        dataHubStack2.setCluster(createClusterResponse(Status.DELETE_COMPLETED));
        StackViewV4Response dataHubStack3 = createStackResponse(Status.STOPPED, "stack-3", "stack-crn-3");
        StackViewV4Responses stackViewV4Responses = new StackViewV4Responses(Set.of(dataHubStack1, dataHubStack2, dataHubStack3));
        when(stackStopRestrictionService.isInfrastructureStoppable(any())).thenReturn(StopRestrictionReason.EPHEMERAL_VOLUMES);
        when(stackService.getByCrn(dataHubStack1.getCrn())).thenReturn(new Stack());

        UpgradeV4Response actual = underTest.checkForRunningAttachedClusters(stackViewV4Responses, new UpgradeV4Response());

        assertNull(actual.getReason());
        verify(stackService).getByCrn(dataHubStack1.getCrn());
        verify(instanceGroupService).getByStackAndFetchTemplates(any());
        verifyNoInteractions(spotInstanceUsageCondition);
    }

    @Test
    public void testCheckForRunningAttachedClustersShouldNotReturnErrorMessageWhenThereAreOnlyOneClusterIsRunningWithSpotInstance() {
        StackViewV4Response dataHubStack1 = createStackResponse(Status.AVAILABLE, "stack-1", "stack-crn-1");
        dataHubStack1.setCluster(createClusterResponse(Status.AVAILABLE));
        StackViewV4Response dataHubStack2 = createStackResponse(Status.DELETE_COMPLETED, "stack-2", "stack-crn-2");
        dataHubStack2.setCluster(createClusterResponse(Status.DELETE_COMPLETED));
        StackViewV4Response dataHubStack3 = createStackResponse(Status.STOPPED, "stack-3", "stack-crn-3");
        StackViewV4Responses stackViewV4Responses = new StackViewV4Responses(Set.of(dataHubStack1, dataHubStack2, dataHubStack3));
        Stack stack = new Stack();
        when(stackStopRestrictionService.isInfrastructureStoppable(any())).thenReturn(StopRestrictionReason.NONE);
        when(stackService.getByCrn(dataHubStack1.getCrn())).thenReturn(stack);
        when(spotInstanceUsageCondition.isStackRunsOnSpotInstances(stack)).thenReturn(true);

        UpgradeV4Response actual = underTest.checkForRunningAttachedClusters(stackViewV4Responses, new UpgradeV4Response());

        assertNull(actual.getReason());
        verify(stackService).getByCrn(dataHubStack1.getCrn());
        verify(instanceGroupService).getByStackAndFetchTemplates(any());
        verify(spotInstanceUsageCondition).isStackRunsOnSpotInstances(stack);
    }

    @Test
    public void tesDataHubsNotAttached() {
        StackViewV4Responses stackViewV4Responses = new StackViewV4Responses(Set.of());
        UpgradeV4Response response = new UpgradeV4Response();

        UpgradeV4Response actual = underTest.checkForRunningAttachedClusters(stackViewV4Responses, response);

        assertNull(actual.getReason());
        verifyNoInteractions(stackService);
        verifyNoInteractions(instanceGroupService);
        verifyNoInteractions(spotInstanceUsageCondition);
    }

    private ClusterViewV4Response createClusterResponse(Status clusterStatus) {
        ClusterViewV4Response dataHubCluster = new ClusterViewV4Response();
        dataHubCluster.setStatus(clusterStatus);
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
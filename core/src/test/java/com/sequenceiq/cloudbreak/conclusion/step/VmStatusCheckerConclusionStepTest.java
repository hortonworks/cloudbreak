package com.sequenceiq.cloudbreak.conclusion.step;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterStatusService;
import com.sequenceiq.cloudbreak.cluster.status.ExtendedHostStatuses;
import com.sequenceiq.cloudbreak.common.type.HealthCheck;
import com.sequenceiq.cloudbreak.common.type.HealthCheckResult;
import com.sequenceiq.cloudbreak.common.type.HealthCheckType;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackInstanceStatusChecker;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class VmStatusCheckerConclusionStepTest {

    @Mock
    private StackService stackService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private InstanceMetaDataToCloudInstanceConverter cloudInstanceConverter;

    @Mock
    private StackInstanceStatusChecker stackInstanceStatusChecker;

    @InjectMocks
    private VmStatusCheckerConclusionStep underTest;

    private ClusterApi connector = Mockito.mock(ClusterApi.class);

    private ClusterStatusService clusterStatusService = Mockito.mock(ClusterStatusService.class);

    @BeforeEach
    public void setUp() {
        Stack stack = new Stack();
        stack.setId(1L);
        when(stackService.getById(anyLong())).thenReturn(stack);
        when(clusterApiConnectors.getConnector(any(Stack.class))).thenReturn(connector);
        when(connector.clusterStatusService()).thenReturn(clusterStatusService);
    }

    @Test
    public void checkShouldBeSuccessfulIfCMIsRunningAndAllInstanceIsHealthy() {
        when(clusterStatusService.isClusterManagerRunningQuickCheck()).thenReturn(Boolean.TRUE);
        when(instanceMetaDataService.findNotTerminatedForStack(anyLong()))
                .thenReturn(Set.of(createInstanceMetadata("host1"), createInstanceMetadata("host2")));
        when(clusterStatusService.getExtendedHostStatuses()).thenReturn(createExtendedHostStatuses(true));

        Conclusion conclusion = underTest.check(1L);
        assertFalse(conclusion.isFailureFound());
        assertNull(conclusion.getConclusion());
        assertNull(conclusion.getDetails());
        assertEquals(VmStatusCheckerConclusionStep.class, conclusion.getConclusionStepClass());
    }

    @Test
    public void checkShouldFailIfCMIsRunningAndUnhealthyOrUnknownInstanceExists() {
        when(clusterStatusService.isClusterManagerRunningQuickCheck()).thenReturn(Boolean.TRUE);
        when(instanceMetaDataService.findNotTerminatedForStack(anyLong()))
                .thenReturn(Set.of(createInstanceMetadata("host1"), createInstanceMetadata("host2"), createInstanceMetadata("host3")));
        when(clusterStatusService.getExtendedHostStatuses()).thenReturn(createExtendedHostStatuses(false));

        Conclusion conclusion = underTest.check(1L);
        assertTrue(conclusion.isFailureFound());
        assertEquals("Unhealthy and/or unknown VMs found based on CM status. Unhealthy VMs: {host1=error, host2=error}, unknown VMs: [host3]. " +
                "Please check the instances on your cloud provider for further details.", conclusion.getConclusion());
        assertEquals("Unhealthy and/or unknown VMs found based on CM status. Unhealthy VMs: {host1=error, host2=error}, unknown VMs: [host3]",
                conclusion.getDetails());
        assertEquals(VmStatusCheckerConclusionStep.class, conclusion.getConclusionStepClass());
    }

    @Test
    public void checkShouldBeSuccessfulIfCMIsNotRunningAndAllInstanceIsHealthy() {
        when(clusterStatusService.isClusterManagerRunningQuickCheck()).thenReturn(Boolean.FALSE);
        when(instanceMetaDataService.findNotTerminatedForStack(anyLong()))
                .thenReturn(Set.of(createInstanceMetadata("host1"), createInstanceMetadata("host2")));
        when(stackInstanceStatusChecker.queryInstanceStatuses(any(), anyList()))
                .thenReturn(List.of(createCloudVmInstanceStatus("host1", true), createCloudVmInstanceStatus("host2", true)));

        Conclusion conclusion = underTest.check(1L);
        assertFalse(conclusion.isFailureFound());
        assertNull(conclusion.getConclusion());
        assertNull(conclusion.getDetails());
        assertEquals(VmStatusCheckerConclusionStep.class, conclusion.getConclusionStepClass());
    }

    @Test
    public void checkShouldFailIfCMIsNotRunningAndUnhealthyInstanceExists() {
        when(clusterStatusService.isClusterManagerRunningQuickCheck()).thenReturn(Boolean.FALSE);
        when(instanceMetaDataService.findNotTerminatedForStack(anyLong()))
                .thenReturn(Set.of(createInstanceMetadata("host1"), createInstanceMetadata("host2")));
        when(stackInstanceStatusChecker.queryInstanceStatuses(any(), anyList()))
                .thenReturn(List.of(createCloudVmInstanceStatus("host1", false), createCloudVmInstanceStatus("host2", false)));

        Conclusion conclusion = underTest.check(1L);
        assertTrue(conclusion.isFailureFound());
        assertEquals("Not running VMs found based on provider status: [host1, host2]. " +
                "Please check the instances on your cloud provider for further details.", conclusion.getConclusion());
        assertEquals("Not running VMs found based on provider status: [host1, host2]",
                conclusion.getDetails());
        assertEquals(VmStatusCheckerConclusionStep.class, conclusion.getConclusionStepClass());
    }

    private ExtendedHostStatuses createExtendedHostStatuses(boolean healthy) {
        Map<HostName, Set<HealthCheck>> hostStatuses = new HashMap<>();
        HealthCheckResult status = healthy ? HealthCheckResult.HEALTHY : HealthCheckResult.UNHEALTHY;
        String reason = healthy ? null : "error";
        Set<HealthCheck> healthChecks = Sets.newHashSet(new HealthCheck(HealthCheckType.HOST, status, Optional.ofNullable(reason)));
        hostStatuses.put(HostName.hostName("host1"), healthChecks);
        hostStatuses.put(HostName.hostName("host2"), healthChecks);
        return new ExtendedHostStatuses(hostStatuses);
    }

    private InstanceMetaData createInstanceMetadata(String host) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN(host);
        instanceMetaData.setInstanceId(host);
        instanceMetaData.setInstanceStatus(InstanceStatus.SERVICES_RUNNING);
        return instanceMetaData;
    }

    private CloudVmInstanceStatus createCloudVmInstanceStatus(String instanceId, boolean healthy) {
        com.sequenceiq.cloudbreak.cloud.model.InstanceStatus instanceStatus
                = healthy ? com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.STARTED : com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.FAILED;
        return new CloudVmInstanceStatus(new CloudInstance(instanceId, null, null, null, null),
                instanceStatus);
    }
}
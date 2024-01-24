package com.sequenceiq.cloudbreak.service.stopstart;

import static com.sequenceiq.cloudbreak.cloud.model.HostName.hostName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterHealthService;
import com.sequenceiq.cloudbreak.cluster.status.DetailedHostStatuses;
import com.sequenceiq.cloudbreak.converter.CloudInstanceIdToInstanceMetaDataConverter;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.RuntimeVersionService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@ExtendWith(MockitoExtension.class)
class RecoveryCandidateCollectionServiceTest {

    private static final Long STACK_ID = 1L;

    private static final String INSTANCE_GROUP_NAME = "compute";

    private static final String MOCK_INSTANCEID_PREFIX = "i-";

    private static final String MOCK_FQDN_PREFIX = "fqdn-";

    private static final String RUNTIME = "7.2.15";

    private static final Integer ALL_INSTANCES_IN_HG_COUNT = 20;

    private static final Integer RUNNING_INSTANCES_COUNT = 10;

    private static final Integer RECOVERY_CANDIDATES_COUNT = 3;

    private static final Integer STOPPED_INSTANCES_COUNT = 7;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private RuntimeVersionService runtimeVersionService;

    @Mock
    private CloudInstanceIdToInstanceMetaDataConverter cloudInstanceIdToInstanceMetaDataConverter;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private ClusterHealthService clusterHealthService;

    @Mock
    private DetailedHostStatuses detailedHostStatuses;

    @Mock
    private StackDto stack;

    @InjectMocks
    private RecoveryCandidateCollectionService underTest;

    @Test
    void testGetInstancesWithServicesNotRunningWhenHostsInMaintenanceMode() {
        List<InstanceMetadataView> allInstancesInHg = generateInstanceMetadata(ALL_INSTANCES_IN_HG_COUNT);
        List<InstanceMetadataView> runningInstances = generateInstanceMetadata(RUNNING_INSTANCES_COUNT);

        setupBasicMocks(allInstancesInHg, runningInstances);
        mockDecommissionedHosts(runningInstances);

        List<InstanceMetadataView> recoveryCandidates = underTest.getStartedInstancesWithServicesNotRunning(stack, INSTANCE_GROUP_NAME,
                generateCloudInstanceIds(RUNNING_INSTANCES_COUNT), false);

        verify(clusterHealthService, times(1)).getDetailedHostStatuses(any(Optional.class));
        assertThat(recoveryCandidates).hasSize(RECOVERY_CANDIDATES_COUNT);
    }

    @Test
    void testGetInstancesWithServicesUnhealthyWhenHostsInUnhealthy() {
        List<InstanceMetadataView> allInstancesInHg = generateInstanceMetadata(ALL_INSTANCES_IN_HG_COUNT);
        List<InstanceMetadataView> runningInstances = generateInstanceMetadata(RUNNING_INSTANCES_COUNT);

        setupBasicMocks(allInstancesInHg, runningInstances);
        mockHostsWithServicesUnhealthyAndUnhealthyHosts(runningInstances);

        List<InstanceMetadataView> recoveryCandidates = underTest.getStartedInstancesWithServicesNotRunning(stack, INSTANCE_GROUP_NAME,
                generateCloudInstanceIds(RUNNING_INSTANCES_COUNT), true);

        verify(clusterHealthService, times(1)).getDetailedHostStatuses(any(Optional.class));
        assertThat(recoveryCandidates).isEmpty();
    }

    @Test
    void testGetInstancesWithServicesHealthyWhenHostsInUnhealthy() {
        List<InstanceMetadataView> allInstancesInHg = generateInstanceMetadata(ALL_INSTANCES_IN_HG_COUNT);
        List<InstanceMetadataView> runningInstances = generateInstanceMetadata(RUNNING_INSTANCES_COUNT);

        setupBasicMocks(allInstancesInHg, runningInstances);
        mockHostsWithServicesHealthyAndUnhealthyHosts(runningInstances);

        List<InstanceMetadataView> recoveryCandidates = underTest.getStartedInstancesWithServicesNotRunning(stack, INSTANCE_GROUP_NAME,
                generateCloudInstanceIds(RUNNING_INSTANCES_COUNT), true);

        verify(clusterHealthService, times(1)).getDetailedHostStatuses(any(Optional.class));
        assertThat(recoveryCandidates).hasSize(RECOVERY_CANDIDATES_COUNT);
    }

    @Test
    void testNoRecoveryCandidatesCollectedForUnhealthyInstances() {
        List<InstanceMetadataView> allInstancesInHg = generateInstanceMetadata(ALL_INSTANCES_IN_HG_COUNT);
        List<InstanceMetadataView> runningInstances = generateInstanceMetadata(RUNNING_INSTANCES_COUNT);

        setupBasicMocks(allInstancesInHg, runningInstances);
        mockUnhealthyHosts(runningInstances);

        List<InstanceMetadataView> recoveryCandidates = underTest.getStartedInstancesWithServicesNotRunning(stack, INSTANCE_GROUP_NAME,
                generateCloudInstanceIds(RUNNING_INSTANCES_COUNT), false);

        verify(clusterHealthService, times(1)).getDetailedHostStatuses(any(Optional.class));
        assertThat(recoveryCandidates).isEmpty();
    }

    @Test
    void testNoRecoveryCandidatesCollectedForInstancesWithServicesRunning() {
        List<InstanceMetadataView> allInstancesInHg = generateInstanceMetadata(ALL_INSTANCES_IN_HG_COUNT);
        List<InstanceMetadataView> runningInstances = generateInstanceMetadata(RUNNING_INSTANCES_COUNT);

        setupBasicMocks(allInstancesInHg, runningInstances);
        mockHostsWithServicesRunning(runningInstances);

        List<InstanceMetadataView> recoveryCandidates = underTest.getStartedInstancesWithServicesNotRunning(stack, INSTANCE_GROUP_NAME,
                generateCloudInstanceIds(RUNNING_INSTANCES_COUNT), false);

        verify(clusterHealthService, times(1)).getDetailedHostStatuses(any(Optional.class));
        assertThat(recoveryCandidates).isEmpty();
    }

    @Test
    void testNoRecoveryCandidatesCollectedWhenCmIsNotRunning() {
        doReturn(clusterApi).when(clusterApiConnectors).getConnector(any(StackDtoDelegate.class));
        doReturn(clusterHealthService).when(clusterApi).clusterHealthService();
        doReturn(Boolean.FALSE).when(clusterHealthService).isClusterManagerRunning();

        List<InstanceMetadataView> recoveryCandidates = underTest.getStartedInstancesWithServicesNotRunning(stack, INSTANCE_GROUP_NAME,
                generateCloudInstanceIds(RUNNING_INSTANCES_COUNT), false);

        assertThat(recoveryCandidates).isEmpty();
        verify(clusterHealthService, never()).getDetailedHostStatuses(any(Optional.class));
        verify(cloudInstanceIdToInstanceMetaDataConverter, never()).getNotDeletedAndNotZombieInstances(anyList(), anyString(), anySet());
        verify(runtimeVersionService, never()).getRuntimeVersion(anyLong());
    }

    @Test
    void testCollectStartedInstancesOnCloudProvider() {
        CloudConnector cloudConnector = mock(CloudConnector.class);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        InstanceConnector instanceConnector = mock(InstanceConnector.class);

        List<CloudVmInstanceStatus> startedInstanceStatuses = generateCloudVmInstances(generateCloudInstances(RUNNING_INSTANCES_COUNT), InstanceStatus.STARTED);
        List<CloudVmInstanceStatus> notStartedInstances = generateCloudVmInstances(generateCloudInstances(STOPPED_INSTANCES_COUNT), InstanceStatus.STOPPED);

        List<CloudVmInstanceStatus> combined = Stream.of(startedInstanceStatuses, notStartedInstances).flatMap(Collection::stream).collect(Collectors.toList());

        doReturn(instanceConnector).when(cloudConnector).instances();
        doReturn(combined).when(instanceConnector).checkWithoutRetry(any(AuthenticatedContext.class), anyList());

        Set<String> result = underTest.collectStartedInstancesFromCloudProvider(cloudConnector, authenticatedContext,
                generateCloudInstances(ALL_INSTANCES_IN_HG_COUNT));

        verify(instanceConnector).checkWithoutRetry(any(AuthenticatedContext.class), anyList());
        assertThat(result).hasSize(RUNNING_INSTANCES_COUNT);
    }

    private void setupBasicMocks(List<InstanceMetadataView> allInstancesInHg, List<InstanceMetadataView> runningInstances) {
        ClusterView clusterView = mock(ClusterView.class);
        lenient().doReturn(clusterView).when(stack).getCluster();
        lenient().doReturn(STACK_ID).when(clusterView).getId();
        lenient().doReturn(STACK_ID).when(stack).getId();
        doReturn(clusterApi).when(clusterApiConnectors).getConnector(any(StackDtoDelegate.class));
        doReturn(clusterHealthService).when(clusterApi).clusterHealthService();
        doReturn(Optional.of(RUNTIME)).when(runtimeVersionService).getRuntimeVersion(anyLong());
        doReturn(Boolean.TRUE).when(clusterHealthService).isClusterManagerRunning();
        doReturn(detailedHostStatuses).when(clusterHealthService).getDetailedHostStatuses(any(Optional.class));
        doReturn(allInstancesInHg).when(stack).getAllAvailableInstances();
        doReturn(runningInstances).when(cloudInstanceIdToInstanceMetaDataConverter).getNotDeletedAndNotZombieInstances(
                anyList(), anyString(), anySet());
    }

    private Set<String> generateCloudInstanceIds(int numInstances) {
        Set<String> instanceIds = new LinkedHashSet<>();
        IntStream.range(0, numInstances).forEach(i -> {
            instanceIds.add(MOCK_INSTANCEID_PREFIX + i);
        });
        return instanceIds;
    }

    private List<InstanceMetadataView> generateInstanceMetadata(int count) {
        List<InstanceMetadataView> instances = new ArrayList<>(count);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(INSTANCE_GROUP_NAME);
        IntStream.range(0, count).forEach(i -> {
            InstanceMetaData instanceMetaData = new InstanceMetaData();
            instanceMetaData.setInstanceId(MOCK_INSTANCEID_PREFIX + i);
            instanceMetaData.setInstanceGroup(instanceGroup);
            instanceMetaData.setDiscoveryFQDN(MOCK_FQDN_PREFIX + i);
            instanceMetaData.setPrivateId((long) i);
            instances.add(instanceMetaData);
        });

        return instances;
    }

    private List<CloudVmInstanceStatus> generateCloudVmInstances(List<CloudInstance> cloudInstances, InstanceStatus status) {
        List<CloudVmInstanceStatus> cloudVmInstanceStatusList = new LinkedList<>();
        for (CloudInstance cloudInstance : cloudInstances) {
            cloudVmInstanceStatusList.add(new CloudVmInstanceStatus(cloudInstance, status));
        }
        return cloudVmInstanceStatusList;
    }

    private List<CloudInstance> generateCloudInstances(int numInstances) {
        List<CloudInstance> instances = new LinkedList<>();
        IntStream.range(0, numInstances).forEach(i -> {
            CloudInstance instance = mock(CloudInstance.class);
            lenient().doReturn(MOCK_INSTANCEID_PREFIX + i).when(instance).getInstanceId();
            instances.add(instance);
        });
        return instances;
    }

    private void mockDecommissionedHosts(List<InstanceMetadataView> instances) {
        Collections.shuffle(instances);
        instances.subList(0, RECOVERY_CANDIDATES_COUNT).forEach(i -> {
            doReturn(Boolean.FALSE).when(detailedHostStatuses).isHostUnHealthy(hostName(i.getDiscoveryFQDN()));
            doReturn(Boolean.TRUE).when(detailedHostStatuses).isHostDecommissioned(hostName(i.getDiscoveryFQDN()));
            doReturn(Boolean.TRUE).when(detailedHostStatuses).areServicesNotRunning(hostName(i.getDiscoveryFQDN()));
        });
    }

    private void mockUnhealthyHosts(List<InstanceMetadataView> instances) {
        Collections.shuffle(instances);
        instances.subList(0, RECOVERY_CANDIDATES_COUNT).forEach(i -> {
            doReturn(Boolean.TRUE).when(detailedHostStatuses).isHostUnHealthy(hostName(i.getDiscoveryFQDN()));
            doReturn(Boolean.TRUE).when(detailedHostStatuses).isHostDecommissioned(hostName(i.getDiscoveryFQDN()));
            doReturn(Boolean.TRUE).when(detailedHostStatuses).areServicesNotRunning(hostName(i.getDiscoveryFQDN()));
        });
    }

    private void mockHostsWithServicesRunning(List<InstanceMetadataView> instances) {
        Collections.shuffle(instances);
        instances.subList(0, RECOVERY_CANDIDATES_COUNT).forEach(i -> {
            doReturn(Boolean.FALSE).when(detailedHostStatuses).isHostUnHealthy(hostName(i.getDiscoveryFQDN()));
            doReturn(Boolean.TRUE).when(detailedHostStatuses).isHostDecommissioned(hostName(i.getDiscoveryFQDN()));
            doReturn(Boolean.FALSE).when(detailedHostStatuses).areServicesNotRunning(hostName(i.getDiscoveryFQDN()));
        });
    }

    private void mockHostsWithServicesUnhealthyAndUnhealthyHosts(List<InstanceMetadataView> instances) {
        Collections.shuffle(instances);
        instances.subList(0, RECOVERY_CANDIDATES_COUNT).forEach(i -> {
            doReturn(Boolean.TRUE).when(detailedHostStatuses).areServicesIrrecoverable(hostName(i.getDiscoveryFQDN()), null);
        });
    }

    private void mockHostsWithServicesHealthyAndUnhealthyHosts(List<InstanceMetadataView> instances) {
        Collections.shuffle(instances);
        instances.subList(0, RECOVERY_CANDIDATES_COUNT).forEach(i -> {
            doReturn(Boolean.FALSE).when(detailedHostStatuses).areServicesIrrecoverable(hostName(i.getDiscoveryFQDN()), null);
            doReturn(Boolean.FALSE).when(detailedHostStatuses).areServicesNotRunning(hostName(i.getDiscoveryFQDN()));
            doReturn(Boolean.TRUE).when(detailedHostStatuses).areServicesUnhealthy(hostName(i.getDiscoveryFQDN()));
        });
    }
}
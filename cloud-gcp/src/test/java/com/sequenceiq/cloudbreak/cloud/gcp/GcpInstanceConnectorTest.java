package com.sequenceiq.cloudbreak.cloud.gcp;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.NetworkInterface;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpComputeFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.template.ResourceContextBuilder;
import com.sequenceiq.cloudbreak.cloud.template.compute.ComputeResourceService;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.template.init.ContextBuilders;

@ExtendWith(MockitoExtension.class)
class GcpInstanceConnectorTest {

    private static final String MOCK_INSTANCEID_PREFIX = "i-";

    private static final List<InstanceStatus> EXCLUDED_STATUSES = List.of(
            InstanceStatus.STOPPED,
            InstanceStatus.ZOMBIE,
            InstanceStatus.TERMINATED,
            InstanceStatus.TERMINATED_BY_PROVIDER,
            InstanceStatus.DELETE_REQUESTED
    );

    private static final Long RESTART_POLL_TIMEBOUND_MS = 1_200_000L;

    private static final String PROJECT_ID = "projectId";

    private static final String AVAILABILITY_ZONE = null;

    private static final String INSTANCE_NAME = "i-0";

    @InjectMocks
    private GcpInstanceConnector underTest;

    @Mock
    private GcpComputeFactory gcpComputeFactory;

    @Mock
    private Compute compute;

    @Mock
    private Compute.Instances instancesMock;

    @Mock
    private Compute.Instances.Get computeInstancesGet;

    @Mock
    private GcpStackUtil gcpStackUtil;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private ComputeResourceService computeResourceService;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private ContextBuilders contextBuilders;

    @BeforeEach
    public void before() {
        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(compute.instances()).thenReturn(instancesMock);
        when(gcpComputeFactory.buildCompute(authenticatedContext.getCloudCredential())).thenReturn(compute);
        when(gcpStackUtil.getProjectId(cloudCredential)).thenReturn(PROJECT_ID);
    }

    @Test
    public void testReboot() throws IOException {
        List<CloudInstance> vms1 = generateCloudInstances(1, 0);
        ResourceContextBuilder resourceContextBuilder = mock(ResourceContextBuilder.class);
        ResourceBuilderContext resourceBuilderContext = mock(ResourceBuilderContext.class);
        List<CloudVmInstanceStatus> startedVmInstanceStatus = generateCloudVmInstanceStatuses(vms1, InstanceStatus.STARTED);
        List<CloudVmInstanceStatus> stoppedVmInstanceStatus = generateCloudVmInstanceStatuses(vms1, InstanceStatus.STOPPED);
        List<CloudResource> allResources = mock(List.class);
        CloudContext cloudContext = mock(CloudContext.class);
        Compute compute = mock(Compute.class);
        Instance runningInstance = createInstance(INSTANCE_NAME, "RUNNING");
        Instance stoppedInstance = createInstance(INSTANCE_NAME, "TERMINATED");

        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getPlatform()).thenReturn(platform("GCP"));
        doReturn(resourceContextBuilder).when(contextBuilders).get(any(Platform.class));
        when(resourceContextBuilder.contextInit(
                cloudContext,
                authenticatedContext,
                null,
                false)).thenReturn(resourceBuilderContext);
        when(resourceContextBuilder.contextInit(
                cloudContext,
                authenticatedContext,
                null,
                true)).thenReturn(resourceBuilderContext);
        when(computeResourceService.stopInstances(resourceBuilderContext, authenticatedContext, vms1)).thenReturn(stoppedVmInstanceStatus);
        when(computeResourceService.startInstances(resourceBuilderContext, authenticatedContext, vms1)).thenReturn(startedVmInstanceStatus);
        when(instancesMock.get(PROJECT_ID, AVAILABILITY_ZONE, INSTANCE_NAME)).thenReturn(computeInstancesGet);
        when(computeInstancesGet.execute())
                .thenReturn(runningInstance)
                .thenReturn(stoppedInstance);

        List<CloudVmInstanceStatus> result = underTest.
                reboot(authenticatedContext, allResources, vms1);

        verify(computeResourceService, times(1)).startInstances(resourceBuilderContext, authenticatedContext, vms1);
        verify(computeResourceService, times(1)).stopInstances(resourceBuilderContext, authenticatedContext, vms1);
        assertThat(result, hasItem(hasProperty("status", is(InstanceStatus.STARTED))));
    }

    @Test
    public void testRestartInstanceWithLimitedRetry() throws IOException {
        List<CloudInstance> vms1 = generateCloudInstances(1, 0);
        ResourceContextBuilder resourceContextBuilder = mock(ResourceContextBuilder.class);
        ResourceBuilderContext resourceBuilderContext = mock(ResourceBuilderContext.class);
        List<CloudVmInstanceStatus> startedVmInstanceStatus = generateCloudVmInstanceStatuses(vms1, InstanceStatus.STARTED);
        List<CloudVmInstanceStatus> stoppedVmInstanceStatus = generateCloudVmInstanceStatuses(vms1, InstanceStatus.STOPPED);
        List<CloudResource> allResources = mock(List.class);
        CloudContext cloudContext = mock(CloudContext.class);
        Compute compute = mock(Compute.class);
        Instance runningInstance = createInstance(INSTANCE_NAME, "RUNNING");
        Instance stoppedInstance = createInstance(INSTANCE_NAME, "TERMINATED");

        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getPlatform()).thenReturn(platform("GCP"));
        doReturn(resourceContextBuilder).when(contextBuilders).get(any(Platform.class));
        when(resourceContextBuilder.contextInit(
                cloudContext,
                authenticatedContext,
                null,
                false)).thenReturn(resourceBuilderContext);
        when(resourceContextBuilder.contextInit(
                cloudContext,
                authenticatedContext,
                null,
                true)).thenReturn(resourceBuilderContext);
        when(computeResourceService.stopInstances(resourceBuilderContext, authenticatedContext, vms1)).thenReturn(stoppedVmInstanceStatus);
        when(computeResourceService.startInstances(resourceBuilderContext, authenticatedContext, vms1)).thenReturn(startedVmInstanceStatus);
        when(instancesMock.get(PROJECT_ID, AVAILABILITY_ZONE, INSTANCE_NAME)).thenReturn(computeInstancesGet);
        when(computeInstancesGet.execute())
                .thenReturn(runningInstance)
                .thenReturn(stoppedInstance);

        List<CloudVmInstanceStatus> result = underTest.
                restartWithLimitedRetry(authenticatedContext, allResources, vms1, RESTART_POLL_TIMEBOUND_MS, EXCLUDED_STATUSES);

        verify(computeResourceService, times(1)).startInstances(resourceBuilderContext, authenticatedContext, vms1);
        verify(computeResourceService, times(1)).stopInstances(resourceBuilderContext, authenticatedContext, vms1);
        assertThat(result, hasItem(hasProperty("status", is(InstanceStatus.STARTED))));
    }

    @Test
    public void testRestartInstanceWithLimitedRetryWithInstanceAlreadyStopped() throws IOException {
        List<CloudInstance> vms1 = generateCloudInstances(1, 0);
        ResourceContextBuilder resourceContextBuilder = mock(ResourceContextBuilder.class);
        ResourceBuilderContext resourceBuilderContext = mock(ResourceBuilderContext.class);
        List<CloudVmInstanceStatus> startedVmInstanceStatus = generateCloudVmInstanceStatuses(vms1, InstanceStatus.STARTED);
        List<CloudVmInstanceStatus> stoppedVmInstanceStatus = generateCloudVmInstanceStatuses(vms1, InstanceStatus.STOPPED);
        List<CloudResource> allResources = mock(List.class);
        CloudContext cloudContext = mock(CloudContext.class);
        Compute compute = mock(Compute.class);
        Instance stoppedInstance = createInstance(INSTANCE_NAME, "TERMINATED");

        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getPlatform()).thenReturn(platform("GCP"));
        doReturn(resourceContextBuilder).when(contextBuilders).get(any(Platform.class));
        when(resourceContextBuilder.contextInit(
                cloudContext,
                authenticatedContext,
                null,
                true)).thenReturn(resourceBuilderContext);
        when(computeResourceService.startInstances(resourceBuilderContext, authenticatedContext, vms1)).thenReturn(startedVmInstanceStatus);
        when(instancesMock.get(PROJECT_ID, AVAILABILITY_ZONE, INSTANCE_NAME)).thenReturn(computeInstancesGet);
        when(computeInstancesGet.execute())
                .thenReturn(stoppedInstance);

        List<CloudVmInstanceStatus> result = underTest.
                restartWithLimitedRetry(authenticatedContext, allResources, vms1, RESTART_POLL_TIMEBOUND_MS, EXCLUDED_STATUSES);

        verify(computeResourceService, times(1)).startInstances(resourceBuilderContext, authenticatedContext, vms1);
        verify(computeResourceService, times(0)).stopInstances(resourceBuilderContext, authenticatedContext, vms1);
        assertThat(result, hasItem(hasProperty("status", is(InstanceStatus.STARTED))));
    }

    private Instance createInstance(String name, String status) {
        Instance instance = new Instance();
        instance.setName(name);
        instance.setStatus(status);
        instance.setNetworkInterfaces(List.of(new NetworkInterface()));
        return instance;
    }

    private List<CloudInstance> generateCloudInstances(int numInstances, int startIndex) {
        List<CloudInstance> instances = new LinkedList<>();

        for (int i = startIndex; i < numInstances + startIndex; i++) {
            CloudInstance cloudInstance = mock(CloudInstance.class);
            cloudInstance.setAvailabilityZone(AVAILABILITY_ZONE);
            lenient().when(cloudInstance.getInstanceId()).thenReturn(MOCK_INSTANCEID_PREFIX + i);
            instances.add(cloudInstance);
        }
        return instances;
    }

    private List<CloudVmInstanceStatus> generateCloudVmInstanceStatuses(List<CloudInstance> cloudInstances, InstanceStatus instanceStatus) {
        List<CloudVmInstanceStatus> cloudVmInstanceStatusList = new LinkedList<>();
        for (CloudInstance cloudInstance : cloudInstances) {
            cloudVmInstanceStatusList.add(new CloudVmInstanceStatus(cloudInstance, instanceStatus));
        }
        return cloudVmInstanceStatusList;
    }
}
package com.sequenceiq.cloudbreak.cloud.azure;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.resourcemanager.compute.models.VirtualMachine;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureCapacityErrorMessageProvider;
import com.sequenceiq.cloudbreak.cloud.azure.util.SchedulerProvider;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.InsufficientCapacityException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@ExtendWith(MockitoExtension.class)
class AzureInstanceConnectorTest {

    private List<CloudInstance> inputList;

    @Mock
    private AzureVirtualMachineService azureVirtualMachineService;

    @Mock
    private AzureClient azureClient;

    @Mock
    private SchedulerProvider schedulerProvider;

    @Mock
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Mock
    private AzureCapacityErrorMessageProvider azureCapacityErrorMessageProvider;

    @InjectMocks
    private AzureInstanceConnector underTest;

    @BeforeEach
    void setUp() {
        lenient().when(schedulerProvider.io()).thenReturn(Schedulers.immediate());
        inputList = getCloudInstances("i-1", "i-2");
    }

    @Test
    void startWithLimitedRetry() {
        AuthenticatedContext ac = mock();
        List<CloudInstance> vms = List.of(
                new CloudInstance("vm1", mock(), mock(), "", ""),
                new CloudInstance("vm2", mock(), mock(), "", ""),
                new CloudInstance("vm3", mock(), mock(), "", ""),
                new CloudInstance("vm4", mock(), mock(), "", "")
        );
        long timeboundInMs = 1L;

        String resourceGroup = "resourceGroup";
        when(azureResourceGroupMetadataProvider.getResourceGroupName(any(), any(DynamicModel.class))).thenReturn(resourceGroup);
        String instanceCapacityError = "instance capacity error";
        when(azureCapacityErrorMessageProvider.getInstanceCapacityErrorMessage(any())).thenReturn(instanceCapacityError);

        when(ac.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureClient.startVirtualMachineAsync(resourceGroup, vms.get(0).getInstanceId(), timeboundInMs))
                .thenReturn(Mono.empty());
        when(azureClient.startVirtualMachineAsync(resourceGroup, vms.get(1).getInstanceId(), timeboundInMs))
                .thenReturn(Mono.error(new TimeoutException("timeout")));
        when(azureClient.startVirtualMachineAsync(resourceGroup, vms.get(2).getInstanceId(), timeboundInMs))
                .thenReturn(Mono.error(new InsufficientCapacityException("capacity")));
        when(azureClient.startVirtualMachineAsync(resourceGroup, vms.get(3).getInstanceId(), timeboundInMs))
                .thenReturn(Mono.error(new RuntimeException("other error")));

        List<CloudVmInstanceStatus> result = underTest.startWithLimitedRetry(ac, List.of(), vms, timeboundInMs);

        Assertions.assertThat(result).containsExactlyInAnyOrder(
                new CloudVmInstanceStatus(vms.get(0), InstanceStatus.STARTED),
                new CloudVmInstanceStatus(vms.get(1), InstanceStatus.UNKNOWN, "timeout"),
                new CloudVmInstanceStatus(vms.get(2), InstanceStatus.FAILED, instanceCapacityError),
                new CloudVmInstanceStatus(vms.get(3), InstanceStatus.FAILED, "other error"));
    }

    @Test
    public void testReStartEveryInstancesStarted() {
        when(azureResourceGroupMetadataProvider.getResourceGroupName(any(), any(DynamicModel.class))).thenReturn("resourceGroup");
        when(azureVirtualMachineService.getVmsAndVmStatusesFromAzure(any(), anyList()))
                .thenReturn(new AzureVirtualMachinesWithStatuses(createVirtualMachineMap("instance1", "instance2"),
                        List.of(
                                new CloudVmInstanceStatus(inputList.get(0), InstanceStatus.STARTED),
                                new CloudVmInstanceStatus(inputList.get(1), InstanceStatus.STARTED))));

        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        azureClient = mock(AzureClient.class);
        when(ac.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureClient.stopVirtualMachineAsync(anyString(), anyString())).thenReturn(Mono.empty());
        when(azureClient.startVirtualMachineAsync(anyString(), anyString(), anyLong())).thenReturn(Mono.empty());
        List<CloudVmInstanceStatus> result = underTest.restartWithLimitedRetry(ac, new ArrayList<>(),
                inputList, 600_000L, List.of(InstanceStatus.STOPPED, InstanceStatus.ZOMBIE, InstanceStatus.TERMINATED,
                        InstanceStatus.TERMINATED_BY_PROVIDER, InstanceStatus.DELETE_REQUESTED));

        verify(azureClient, times(2)).stopVirtualMachineAsync(anyString(), anyString());
        verify(azureClient, times(2)).startVirtualMachineAsync(anyString(), anyString(), anyLong());
        assertThat(result, hasItem(allOf(hasProperty("status", is(InstanceStatus.STARTED)))));
    }

    @Test
    public void testReStartOneInstanceTerminated() {
        when(azureResourceGroupMetadataProvider.getResourceGroupName(any(), any(DynamicModel.class))).thenReturn("resourceGroup");
        when(azureVirtualMachineService.getVmsAndVmStatusesFromAzure(any(), anyList()))
                .thenReturn(new AzureVirtualMachinesWithStatuses(createVirtualMachineMap("instance1", "instance2"),
                        List.of(
                                new CloudVmInstanceStatus(inputList.get(0), InstanceStatus.STARTED),
                                new CloudVmInstanceStatus(inputList.get(1), InstanceStatus.TERMINATED))));

        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        azureClient = mock(AzureClient.class);
        when(ac.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureClient.stopVirtualMachineAsync(anyString(), anyString())).thenReturn(Mono.empty());
        when(azureClient.startVirtualMachineAsync(anyString(), anyString(), anyLong())).thenReturn(Mono.empty());
        List<CloudVmInstanceStatus> result = underTest.restartWithLimitedRetry(ac, new ArrayList<>(),
                inputList, 600_000L, List.of(InstanceStatus.STOPPED, InstanceStatus.ZOMBIE, InstanceStatus.TERMINATED,
                        InstanceStatus.TERMINATED_BY_PROVIDER, InstanceStatus.DELETE_REQUESTED));

        verify(azureClient, times(1)).stopVirtualMachineAsync(anyString(), anyString());
        verify(azureClient, times(1)).startVirtualMachineAsync(anyString(), anyString(), anyLong());
        assertThat(result, hasItem(hasProperty("status", is(InstanceStatus.STARTED))));
    }

    @Test
    void testRebootWithMixedResultsThrowsWithPerVmStatuses() {
        List<CloudInstance> vms = getCloudInstances("i-1", "i-2", "i-3");
        AuthenticatedContext ac = mockCheckAndClient(vms, InstanceStatus.STOPPED);
        when(azureClient.startVirtualMachineAsync("resourceGroup", "i-1", null)).thenReturn(Mono.empty());
        when(azureClient.startVirtualMachineAsync("resourceGroup", "i-2", null)).thenReturn(Mono.error(new TimeoutException("timeout")));
        when(azureClient.startVirtualMachineAsync("resourceGroup", "i-3", null)).thenReturn(Mono.error(new InsufficientCapacityException("capacity")));

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.reboot(ac, List.of(), vms));

        Assertions.assertThat(exception.getMessage()).isEqualTo("Error(s) occurred while waiting for vm reboot: "
                + "statuses: [i-1 - STARTED, i-2 - FAILED - timeout, i-3 - FAILED - capacity]");
    }

    @Test
    void testRestartWithLimitedRetryWithMixedResultsThrowsWithPerVmStatuses() {
        List<CloudInstance> vms = getCloudInstances("i-1", "i-2", "i-3");
        AuthenticatedContext ac = mockCheckAndClient(vms, InstanceStatus.STARTED);
        when(azureClient.stopVirtualMachineAsync(anyString(), anyString())).thenReturn(Mono.empty());
        when(azureClient.startVirtualMachineAsync("resourceGroup", "i-1", 600_000L)).thenReturn(Mono.empty());
        when(azureClient.startVirtualMachineAsync("resourceGroup", "i-2", 600_000L)).thenReturn(Mono.error(new TimeoutException("timeout")));
        when(azureClient.startVirtualMachineAsync("resourceGroup", "i-3", 600_000L))
                .thenReturn(Mono.error(new InsufficientCapacityException("capacity")));

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.restartWithLimitedRetry(ac, List.of(), vms, 600_000L, List.of(InstanceStatus.STOPPED)));

        Assertions.assertThat(exception.getMessage()).isEqualTo("Error(s) occurred while waiting for vm restart: "
                + "statuses: [i-1 - STARTED, i-2 - FAILED - timeout, i-3 - FAILED - capacity]");
    }

    @Test
    void testRebootWithSingleFailureThrowsWithPerVmStatuses() {
        List<CloudInstance> vms = getCloudInstances("i-1", "i-2", "i-3");
        AuthenticatedContext ac = mockCheckAndClient(vms, InstanceStatus.STOPPED);
        when(azureClient.startVirtualMachineAsync(anyString(), anyString(), any())).thenReturn(Mono.empty());
        when(azureClient.startVirtualMachineAsync("resourceGroup", "i-2", null)).thenReturn(Mono.error(new RuntimeException("start failed")));

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.reboot(ac, List.of(), vms));

        Assertions.assertThat(exception.getMessage()).isEqualTo("Error(s) occurred while waiting for vm reboot: "
                + "statuses: [i-1 - STARTED, i-2 - FAILED - start failed, i-3 - STARTED]");
    }

    private AuthenticatedContext mockCheckAndClient(List<CloudInstance> vms, InstanceStatus currentStatus) {
        when(azureResourceGroupMetadataProvider.getResourceGroupName(any(), any(DynamicModel.class))).thenReturn("resourceGroup");
        when(azureVirtualMachineService.getVmsAndVmStatusesFromAzure(any(), anyList()))
                .thenReturn(new AzureVirtualMachinesWithStatuses(createVirtualMachineMap("i-1", "i-2", "i-3"),
                        vms.stream().map(vm -> new CloudVmInstanceStatus(vm, currentStatus)).toList()));
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        when(ac.getParameter(AzureClient.class)).thenReturn(azureClient);
        return ac;
    }

    private List<CloudInstance> getCloudInstances(String... instanceIds) {
        return Arrays.stream(instanceIds)
                .map(instanceId -> new CloudInstance(instanceId, null, null, "subnet-123", "az1"))
                .toList();
    }

    private Map<String, VirtualMachine> createVirtualMachineMap(String... instanceIds) {
        Map<String, VirtualMachine> virtualMachineMap = new HashMap<>();
        if (instanceIds != null) {
            for (String instanceId : instanceIds) {
                virtualMachineMap.put(instanceId, null);
            }
        }
        return virtualMachineMap;
    }
}
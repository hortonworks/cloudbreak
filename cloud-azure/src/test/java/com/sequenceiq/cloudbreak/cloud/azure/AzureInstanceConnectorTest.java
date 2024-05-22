package com.sequenceiq.cloudbreak.cloud.azure;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.resourcemanager.compute.models.VirtualMachine;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.util.SchedulerProvider;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;

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

    @InjectMocks
    private AzureInstanceConnector underTest;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        lenient().when(schedulerProvider.io()).thenReturn(Schedulers.immediate());
        inputList = getCloudInstances();
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

    private List<CloudInstance> getCloudInstances() {
        CloudInstance instance1 = new CloudInstance("i-1", null, null, "subnet-123", "az1");
        CloudInstance instance2 = new CloudInstance("i-2", null, null, "subnet-123", "az1");
        return List.of(instance1, instance2);
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
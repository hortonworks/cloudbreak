package com.sequenceiq.cloudbreak.cloud.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.azure.CloudError;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.Page;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.compute.PowerState;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.rest.RestException;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

import rx.Observable;

@ExtendWith(MockitoExtension.class)
public class AzureVirtualMachineServiceTest {

    private static final String INSTANCE_1 = "instance-1";

    private static final String INSTANCE_2 = "instance-2";

    private static final String INSTANCE_3 = "instance-3";

    private static final String RESOURCE_GROUP = "resource-group";

    @Mock
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @InjectMocks
    private AzureVirtualMachineService underTest;

    @Mock
    private AzureClient azureClient;

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private CloudContext cloudContext;

    @Test
    public void testGetVirtualMachinesByIdShouldReturnTheVirtualMachines() {
        Set<String> privateInstanceIds = createPrivateInstanceIds();
        PagedList<VirtualMachine> virtualMachines = createPagedList();

        when(azureClient.getVirtualMachines(RESOURCE_GROUP)).thenReturn(virtualMachines);

        Map<String, VirtualMachine> actual = underTest.getVirtualMachinesByName(azureClient, RESOURCE_GROUP, privateInstanceIds);

        assertEquals(INSTANCE_1, actual.get(INSTANCE_1).name());
        assertEquals(INSTANCE_2, actual.get(INSTANCE_2).name());
        assertEquals(INSTANCE_3, actual.get(INSTANCE_3).name());
    }

    @Test
    public void testGetVirtualMachinesByIdShouldReturnOneVirtualMachinesWhenOnlyTheFirstPageAvailable() {
        Set<String> privateInstanceIds = createPrivateInstanceIds();
        PagedList<VirtualMachine> virtualMachinesWithOnePage = createPagedListWithOnePage();

        when(azureClient.getVirtualMachines(RESOURCE_GROUP)).thenReturn(virtualMachinesWithOnePage);

        Map<String, VirtualMachine> actual = underTest.getVirtualMachinesByName(azureClient, RESOURCE_GROUP, privateInstanceIds);

        assertEquals(INSTANCE_1, actual.get(INSTANCE_1).name());
        assertEquals(1, actual.size());
    }

    @Test
    public void testGetVirtualMachinesByResourceGroupShouldThrowExceptionWhenListIsEmpty() {
        Set<String> privateInstanceIds = createPrivateInstanceIds();
        PagedList<VirtualMachine> virtualMachinesEmpty = createEmptyPagedList();

        when(azureClient.getVirtualMachines(RESOURCE_GROUP)).thenReturn(virtualMachinesEmpty);
        when(azureClient.getVirtualMachineByResourceGroup(any(), any())).thenReturn(null);


        CloudConnectorException exception = assertThrows(
                CloudConnectorException.class,
                () -> underTest.getVirtualMachinesByName(azureClient, RESOURCE_GROUP, privateInstanceIds));
        assertEquals("Operation failed, azure returned an empty list while trying to list vms in resource group.", exception.getMessage());
    }

    @Test
    public void testGetVmStatuses() {
        when(ac.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(ac.getCloudContext()).thenReturn(cloudContext);
        CloudInstance cloudInstance = cloudInstance(INSTANCE_1);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, cloudInstance)).thenReturn(RESOURCE_GROUP);
        PagedList<VirtualMachine> virtualMachines = createPagedListWithOnePage();
        when(azureClient.getVirtualMachines(RESOURCE_GROUP)).thenReturn(virtualMachines);

        AzureVirtualMachinesWithStatuses result = underTest.getVmsAndVmStatusesFromAzure(ac, List.of(cloudInstance));

        assertNotNull(result);
        assertEquals(1, result.getStatuses().size());
        CloudVmInstanceStatus vmStatus = result.getStatuses().get(0);
        assertEquals(cloudInstance, vmStatus.getCloudInstance());
        assertEquals(InstanceStatus.STARTED, vmStatus.getStatus());
    }

    @Test
    public void testGetVmStatusesWhenVmIsNotFoundOnProvider() {
        when(ac.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(ac.getCloudContext()).thenReturn(cloudContext);
        CloudInstance cloudInstance = cloudInstance(INSTANCE_1);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, cloudInstance)).thenReturn(RESOURCE_GROUP);
        when(azureClient.getVirtualMachines(RESOURCE_GROUP))
                .thenThrow(new CloudException(INSTANCE_1 + " not found.", null, new CloudError().withCode("ResourceNotFound")));

        AzureVirtualMachinesWithStatuses result = underTest.getVmsAndVmStatusesFromAzure(ac, List.of(cloudInstance));

        assertNotNull(result);
        assertEquals(1, result.getStatuses().size());
        CloudVmInstanceStatus vmStatus = result.getStatuses().get(0);
        assertEquals(cloudInstance, vmStatus.getCloudInstance());
        assertEquals(InstanceStatus.TERMINATED, vmStatus.getStatus());
    }

    @Test
    public void testGetVmStatusesWhenProviderThrowsUnknownErrorAboutTheInstance() {
        when(ac.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(ac.getCloudContext()).thenReturn(cloudContext);
        CloudInstance cloudInstance = cloudInstance(INSTANCE_1);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, cloudInstance)).thenReturn(RESOURCE_GROUP);
        when(azureClient.getVirtualMachines(RESOURCE_GROUP))
                .thenThrow(new CloudException(INSTANCE_1 + " is bad.", null, new CloudError().withCode("StrangeErrorCode")));

        AzureVirtualMachinesWithStatuses result = underTest.getVmsAndVmStatusesFromAzure(ac, List.of(cloudInstance));

        assertNotNull(result);
        assertEquals(1, result.getStatuses().size());
        CloudVmInstanceStatus vmStatus = result.getStatuses().get(0);
        assertEquals(cloudInstance, vmStatus.getCloudInstance());
        assertEquals(InstanceStatus.UNKNOWN, vmStatus.getStatus());
        assertEquals("Failed to get VM's state from Azure: com.microsoft.azure.CloudException: instance-1 is bad.", vmStatus.getStatusReason());
    }

    @Test
    public void testGetVmStatusesWhenProviderReturnsEmptyVmList() {
        when(ac.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(ac.getCloudContext()).thenReturn(cloudContext);
        CloudInstance cloudInstance = cloudInstance(INSTANCE_1);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, cloudInstance)).thenReturn(RESOURCE_GROUP);
        PagedList<VirtualMachine> virtualMachines = createEmptyPagedList();
        when(azureClient.getVirtualMachines(RESOURCE_GROUP)).thenReturn(virtualMachines);

        AzureVirtualMachinesWithStatuses result = underTest.getVmsAndVmStatusesFromAzure(ac, List.of(cloudInstance));

        assertNotNull(result);
        assertEquals(1, result.getStatuses().size());
        CloudVmInstanceStatus vmStatus = result.getStatuses().get(0);
        assertEquals(cloudInstance, vmStatus.getCloudInstance());
        assertEquals(InstanceStatus.TERMINATED, vmStatus.getStatus());
    }

    private PagedList<VirtualMachine> createPagedList() {
        PagedList<VirtualMachine> pagedList = new PagedList<>() {
            @Override
            public Page<VirtualMachine> nextPage(String nextPageLink) throws RestException {
                return null;
            }
        };
        pagedList.add(createVirtualMachine(INSTANCE_1));
        pagedList.add(createVirtualMachine(INSTANCE_2));
        pagedList.add(createVirtualMachine(INSTANCE_3));
        return pagedList;
    }

    private PagedList<VirtualMachine> createEmptyPagedList() {
        return new PagedList<>() {
            @Override
            public Page<VirtualMachine> nextPage(String nextPageLink) throws RestException {
                return null;
            }
        };
    }

    private PagedList<VirtualMachine> createPagedListWithOnePage() {
        PagedList<VirtualMachine> pagedList = new PagedList<>() {
            @Override
            public Page<VirtualMachine> nextPage(String nextPageLink) throws RestException {
                return null;
            }

            @Override
            public void loadNextPage() {
            }

            @Override
            public boolean hasNextPage() {
                return false;
            }
        };
        pagedList.add(createVirtualMachine(INSTANCE_1));
        return pagedList;
    }

    private VirtualMachine createVirtualMachine(String vmId) {
        VirtualMachine virtualMachine = mock(VirtualMachine.class);
        when(virtualMachine.name()).thenReturn(vmId);
        lenient().when(virtualMachine.powerState()).thenReturn(PowerState.RUNNING);
        lenient().when(virtualMachine.refreshInstanceViewAsync()).thenReturn(Observable.just(null));
        return virtualMachine;
    }

    private CloudInstance cloudInstance(String instanceId) {
        return new CloudInstance(instanceId, null, null, null, null);
    }

    private Set<String> createPrivateInstanceIds() {
        return Set.of(INSTANCE_1, INSTANCE_2, INSTANCE_3);
    }

}
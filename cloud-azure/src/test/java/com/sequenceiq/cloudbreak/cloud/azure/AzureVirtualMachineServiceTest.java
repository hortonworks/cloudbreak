package com.sequenceiq.cloudbreak.cloud.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.core.http.HttpResponse;
import com.azure.resourcemanager.compute.models.ApiError;
import com.azure.resourcemanager.compute.models.ApiErrorException;
import com.azure.resourcemanager.compute.models.PowerState;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureListResult;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureExceptionHandler;
import com.sequenceiq.cloudbreak.cloud.azure.util.SchedulerProvider;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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

    @Mock
    private SchedulerProvider schedulerProvider;

    @Mock
    private AzureExceptionHandler azureExceptionHandler;

    @BeforeEach
    public void setUp() {
        lenient().when(schedulerProvider.io()).thenReturn(Schedulers.immediate());
    }

    @Test
    public void testGetVirtualMachinesByIdShouldReturnTheVirtualMachines() {
        Set<String> privateInstanceIds = createPrivateInstanceIds();
        AzureListResult<VirtualMachine> virtualMachines = createPagedList();

        when(azureClient.getVirtualMachines(RESOURCE_GROUP)).thenReturn(virtualMachines);

        Map<String, VirtualMachine> actual = underTest.getVirtualMachinesByName(azureClient, RESOURCE_GROUP, privateInstanceIds);

        assertEquals(INSTANCE_1, actual.get(INSTANCE_1).name());
        assertEquals(INSTANCE_2, actual.get(INSTANCE_2).name());
        assertEquals(INSTANCE_3, actual.get(INSTANCE_3).name());
    }

    @Test
    public void testGetVirtualMachinesByIdShouldReturnOneVirtualMachinesWhenOnlyTheFirstPageAvailable() {
        Set<String> privateInstanceIds = createPrivateInstanceIds();
        AzureListResult<VirtualMachine> virtualMachinesWithOnePage = createPagedListWithOnePage();

        when(azureClient.getVirtualMachines(RESOURCE_GROUP)).thenReturn(virtualMachinesWithOnePage);

        Map<String, VirtualMachine> actual = underTest.getVirtualMachinesByName(azureClient, RESOURCE_GROUP, privateInstanceIds);

        assertEquals(INSTANCE_1, actual.get(INSTANCE_1).name());
        assertEquals(1, actual.size());
    }

    @Test
    public void testGetVirtualMachinesByResourceGroupShouldThrowExceptionWhenListIsEmpty() {
        Set<String> privateInstanceIds = createPrivateInstanceIds();
        AzureListResult<VirtualMachine> virtualMachinesEmpty = createEmptyPagedList();

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
        AzureListResult<VirtualMachine> virtualMachines = createPagedListWithOnePage();
        when(azureClient.getVirtualMachines(RESOURCE_GROUP)).thenReturn(virtualMachines);

        AzureVirtualMachinesWithStatuses result = underTest.getVmsAndVmStatusesFromAzure(ac, List.of(cloudInstance));

        verify(azureClient, times(0)).getVirtualMachineByResourceGroup(any(), any());
        assertNotNull(result);
        assertEquals(1, result.getStatuses().size());
        CloudVmInstanceStatus vmStatus = result.getStatuses().get(0);
        assertEquals(cloudInstance, vmStatus.getCloudInstance());
        assertEquals(InstanceStatus.STARTED, vmStatus.getStatus());
    }

    @Test
    public void testGetVmStatusesIfVmsByResourceGroupReturnsWithEmptyList() {
        when(ac.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(ac.getCloudContext()).thenReturn(cloudContext);
        CloudInstance cloudInstance = cloudInstance(INSTANCE_1);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, cloudInstance)).thenReturn(RESOURCE_GROUP);
        AzureListResult<VirtualMachine> virtualMachines = createEmptyPagedList();
        when(azureClient.getVirtualMachines(RESOURCE_GROUP)).thenReturn(virtualMachines);
        VirtualMachine virtualMachine = createVirtualMachine(INSTANCE_1);
        when(azureClient.getVirtualMachineByResourceGroup(RESOURCE_GROUP, INSTANCE_1)).thenReturn(virtualMachine);

        AzureVirtualMachinesWithStatuses result = underTest.getVmsAndVmStatusesFromAzure(ac, List.of(cloudInstance));

        verify(azureClient, times(1)).getVirtualMachineByResourceGroup(RESOURCE_GROUP, INSTANCE_1);
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
        ApiError apiError = new ApiError();
        AzureTestUtils.setField(apiError, "code", "ResourceNotFound");
        HttpResponse httpResponse = mock(HttpResponse.class);
        when(azureExceptionHandler.isNotFound(any())).thenReturn(true);
        when(azureClient.getVirtualMachines(RESOURCE_GROUP))
                .thenThrow(new ApiErrorException(INSTANCE_1 + " not found.", httpResponse, apiError));

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
        ApiError apiError = new ApiError();
        AzureTestUtils.setField(apiError, "code", "StrangeErrorCode");
        when(azureClient.getVirtualMachines(RESOURCE_GROUP))
                .thenThrow(new ApiErrorException(INSTANCE_1 + " is bad.", null, apiError));

        AzureVirtualMachinesWithStatuses result = underTest.getVmsAndVmStatusesFromAzure(ac, List.of(cloudInstance));

        assertNotNull(result);
        assertEquals(1, result.getStatuses().size());
        CloudVmInstanceStatus vmStatus = result.getStatuses().get(0);
        assertEquals(cloudInstance, vmStatus.getCloudInstance());
        assertEquals(InstanceStatus.UNKNOWN, vmStatus.getStatus());
        assertEquals("Failed to get VM's state from Azure: com.azure.resourcemanager.compute.models.ApiErrorException: instance-1 is bad.",
                vmStatus.getStatusReason());
    }

    @Test
    public void testGetVmStatusesWhenProviderReturnsEmptyVmList() {
        when(ac.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(ac.getCloudContext()).thenReturn(cloudContext);
        CloudInstance cloudInstance = cloudInstance(INSTANCE_1);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, cloudInstance)).thenReturn(RESOURCE_GROUP);
        AzureListResult<VirtualMachine> virtualMachines = createEmptyPagedList();
        when(azureClient.getVirtualMachines(RESOURCE_GROUP)).thenReturn(virtualMachines);

        AzureVirtualMachinesWithStatuses result = underTest.getVmsAndVmStatusesFromAzure(ac, List.of(cloudInstance));

        assertNotNull(result);
        assertEquals(1, result.getStatuses().size());
        CloudVmInstanceStatus vmStatus = result.getStatuses().get(0);
        assertEquals(cloudInstance, vmStatus.getCloudInstance());
        assertEquals(InstanceStatus.TERMINATED, vmStatus.getStatus());
    }

    private AzureListResult<VirtualMachine> createPagedList() {
        List<VirtualMachine> list = new ArrayList<>();
        list.add(createVirtualMachine(INSTANCE_1));
        list.add(createVirtualMachine(INSTANCE_2));
        list.add(createVirtualMachine(INSTANCE_3));
        AzureListResult<VirtualMachine> azureListResult = mock(AzureListResult.class);
        lenient().when(azureListResult.getAll()).thenReturn(list);
        lenient().when(azureListResult.getStream()).thenReturn(list.stream());
        lenient().when(azureListResult.getWhile(any())).thenReturn(list);
        return azureListResult;
    }

    private AzureListResult<VirtualMachine> createEmptyPagedList() {
        AzureListResult<VirtualMachine> azureListResult = mock(AzureListResult.class);
        lenient().when(azureListResult.getAll()).thenReturn(new ArrayList<>());
        lenient().when(azureListResult.getStream()).thenAnswer(i -> Stream.empty());
        lenient().when(azureListResult.getWhile(any())).thenReturn(new ArrayList<>());
        return azureListResult;
    }

    private AzureListResult<VirtualMachine> createPagedListWithOnePage() {
        List<VirtualMachine> list = new ArrayList<>();
        list.add(createVirtualMachine(INSTANCE_1));
        AzureListResult<VirtualMachine> azureListResult = mock(AzureListResult.class);
        lenient().when(azureListResult.getAll()).thenReturn(list);
        lenient().when(azureListResult.getStream()).thenReturn(list.stream());
        lenient().when(azureListResult.getWhile(any())).thenReturn(list);
        return azureListResult;
    }

    private VirtualMachine createVirtualMachine(String vmId) {
        VirtualMachine virtualMachine = mock(VirtualMachine.class);
        when(virtualMachine.name()).thenReturn(vmId);
        lenient().when(virtualMachine.powerState()).thenReturn(PowerState.RUNNING);
        lenient().when(virtualMachine.refreshInstanceViewAsync()).thenReturn(Mono.empty());
        return virtualMachine;
    }

    private CloudInstance cloudInstance(String instanceId) {
        return new CloudInstance(instanceId, null, null, null, null);
    }

    private Set<String> createPrivateInstanceIds() {
        return Set.of(INSTANCE_1, INSTANCE_2, INSTANCE_3);
    }
}
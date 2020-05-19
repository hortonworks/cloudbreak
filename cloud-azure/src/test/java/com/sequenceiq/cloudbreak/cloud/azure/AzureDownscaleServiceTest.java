package com.sequenceiq.cloudbreak.cloud.azure;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.azure.connector.resource.AzureComputeResourceService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class AzureDownscaleServiceTest {

    private static final String RESOURCE_GROUP_NAME = "resourceGroupName";

    private static final List<String> AVAILABILITY_SET_NAME_LIST = List.of("availabilitySetName");

    private static final List<String> PUBLIC_ADDRESS_NAME_LIST = List.of("publicAddressName");

    private static final List<String> NETWORK_INTERFACE_NAME_LIST = List.of("networkInterfaceName");

    private static final List<String> MANAGED_DISK_ID_LIST = List.of("managedDiskId");

    @Mock
    private AzureStorage azureStorage;

    @Mock
    private CloudResourceHelper cloudResourceHelper;

    @Mock
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Mock
    private AzureVirtualMachineService azureVirtualMachineService;

    @Mock
    private AzureUtils azureUtils;

    @Mock
    private AzureComputeResourceService azureComputeResourceService;

    @Mock
    private AzureResourceConnector azureResourceConnector;

    @InjectMocks
    private AzureDownscaleService underTest;

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private CloudStack cloudStack;

    private final List<CloudResource> resourcesToRemoveList = setupCloudResources();

    @BeforeEach
    void setup() {
        when(azureResourceGroupMetadataProvider.getResourceGroupName((CloudContext) any(), (CloudStack) any())).thenReturn(RESOURCE_GROUP_NAME);
    }

    @Test
    void testWhenDownscaleThenNoAvailabilitySets() {
        underTest.downscale(ac, cloudStack, List.of(), List.of(), getResourceToRemove());

        verify(azureUtils).deleteInstances(any(), any());
        verify(azureUtils).waitForDetachNetworkInterfaces(eq(ac), any(), eq(RESOURCE_GROUP_NAME), eq(NETWORK_INTERFACE_NAME_LIST));
        verify(azureUtils).deleteNetworkInterfaces(any(), eq(RESOURCE_GROUP_NAME), eq(NETWORK_INTERFACE_NAME_LIST));
        verify(azureUtils).deletePublicIps(any(), eq(RESOURCE_GROUP_NAME), eq(PUBLIC_ADDRESS_NAME_LIST));
        verify(azureUtils).deleteManagedDisks(any(), eq(MANAGED_DISK_ID_LIST));
        verify(azureUtils, never()).deleteAvailabilitySets(any(), eq(RESOURCE_GROUP_NAME), anyCollection());
    }

    @Test
    void testWhenTerminateThenAlsoAvailabilitySets() {
        underTest.terminate(ac, cloudStack, List.of(), List.of(), getResourceToRemove());

        verify(azureUtils).deleteInstances(any(), any());
        verify(azureUtils).waitForDetachNetworkInterfaces(eq(ac), any(), eq(RESOURCE_GROUP_NAME), eq(NETWORK_INTERFACE_NAME_LIST));
        verify(azureUtils).deleteNetworkInterfaces(any(), eq(RESOURCE_GROUP_NAME), eq(NETWORK_INTERFACE_NAME_LIST));
        verify(azureUtils).deletePublicIps(any(), eq(RESOURCE_GROUP_NAME), eq(PUBLIC_ADDRESS_NAME_LIST));
        verify(azureUtils).deleteManagedDisks(any(), eq(MANAGED_DISK_ID_LIST));
        verify(azureUtils).deleteAvailabilitySets(any(), any(), eq(AVAILABILITY_SET_NAME_LIST));
    }

    private List<CloudResource> setupCloudResources() {
        return List.of(
                CloudResource.builder().type(ResourceType.AZURE_INSTANCE).status(CommonStatus.CREATED).name("myInstance").params(Map.of()).build()
        );
    }

    private Map<String, Map<String, Object>> getResourceToRemove() {
        return Map.of("name",
                Map.of(
                        "NETWORK_INTERFACES_NAMES", NETWORK_INTERFACE_NAME_LIST,
                        "PUBLIC_ADDRESS_NAME", PUBLIC_ADDRESS_NAME_LIST,
                        "AVAILABILITY_SET_NAME", AVAILABILITY_SET_NAME_LIST,
                        "MANAGED_DISK_IDS", MANAGED_DISK_ID_LIST,
                        "STORAGE_PROFILE_DISK_NAMES", List.of("storageProfileDiskNames"),
                        "ATTACHED_DISK_STORAGE_NAME", List.of("attachedDiskStorageName")
                )
        );
    }
}

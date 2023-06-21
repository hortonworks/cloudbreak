package com.sequenceiq.cloudbreak.cloud.azure.resource;

import static com.sequenceiq.cloudbreak.cloud.model.CloudInstance.FQDN;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_INSTANCE;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_VOLUMESET;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.resourcemanager.compute.models.Disk;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.context.AzureContext;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
public class AzureAttachmentResourceBuilderTest {

    @Mock
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Spy
    private AzureInstanceFinder azureInstanceFinder;

    @InjectMocks
    private AzureAttachmentResourceBuilder underTest;

    @Test
    public void testSuccessfulAttachmentWithFqdn() {
        List<VolumeSetAttributes.Volume> volumes = List.of(
                new VolumeSetAttributes.Volume("disk", null, null, null, null)
        );
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes(null, null, null,
                volumes, null, null);
        volumeSetAttributes.setDiscoveryFQDN("fqdn1");
        CloudResource instanceResource = createResource(AZURE_INSTANCE, Map.of());
        List<CloudResource> buildableResource = Lists.newArrayList(
                instanceResource,
                createResource(AZURE_VOLUMESET, Map.of(CloudResource.ATTRIBUTES, volumeSetAttributes))
        );
        CloudInstance cloudInstance = new CloudInstance("instance", null, null, null, null, Map.of(FQDN, "fqdn1"));
        AuthenticatedContext auth = mock(AuthenticatedContext.class);
        AzureClient azureClient = mock(AzureClient.class);
        VirtualMachine virtualMachine = mock(VirtualMachine.class);
        Disk disk = mock(Disk.class);
        when(virtualMachine.dataDisks()).thenReturn(Map.of());
        when(auth.getCloudContext()).thenReturn(mock(CloudContext.class));
        when(auth.getParameter(eq(AzureClient.class))).thenReturn(azureClient);
        when(azureClient.getVirtualMachineByResourceGroup(any(), any())).thenReturn(virtualMachine);
        when(azureClient.getDiskById(any())).thenReturn(disk);
        when(disk.id()).thenReturn("diskid");
        when(azureResourceGroupMetadataProvider.getResourceGroupName(any(CloudContext.class), any(CloudStack.class))).thenReturn("resourceGroup");
        AzureContext azureContext = mock(AzureContext.class);
        when(azureContext.getComputeResources(1L)).thenReturn(List.of(instanceResource));
        underTest.build(azureContext, cloudInstance, 1L, auth, null, buildableResource, mock(CloudStack.class));
    }

    @Test
    public void testSuccessfulAttachmentWithoutFqdn() {
        List<VolumeSetAttributes.Volume> volumes = List.of(
                new VolumeSetAttributes.Volume("disk", null, null, null, null)
        );
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes(null, null, null,
                volumes, null, null);
        CloudResource instanceResource = createResource(AZURE_INSTANCE, Map.of());
        List<CloudResource> buildableResource = Lists.newArrayList(
                instanceResource,
                createResource(AZURE_VOLUMESET, Map.of(CloudResource.ATTRIBUTES, volumeSetAttributes))
        );
        CloudInstance cloudInstance = new CloudInstance("instance", null, null, null, null, Map.of());
        AuthenticatedContext auth = mock(AuthenticatedContext.class);
        AzureClient azureClient = mock(AzureClient.class);
        VirtualMachine virtualMachine = mock(VirtualMachine.class);
        Disk disk = mock(Disk.class);
        when(virtualMachine.dataDisks()).thenReturn(Map.of());
        when(auth.getCloudContext()).thenReturn(mock(CloudContext.class));
        when(auth.getParameter(eq(AzureClient.class))).thenReturn(azureClient);
        when(azureClient.getVirtualMachineByResourceGroup(any(), any())).thenReturn(virtualMachine);
        when(azureClient.getDiskById(any())).thenReturn(disk);
        when(disk.id()).thenReturn("diskid");
        when(azureResourceGroupMetadataProvider.getResourceGroupName(any(CloudContext.class), any(CloudStack.class))).thenReturn("resourceGroup");
        AzureContext azureContext = mock(AzureContext.class);
        when(azureContext.getComputeResources(1L)).thenReturn(List.of(instanceResource));
        underTest.build(azureContext, cloudInstance, 1L, auth, null, buildableResource, mock(CloudStack.class));
    }

    @Test
    public void testFailingAttachmentWithNonMatchingFqdns() {
        List<VolumeSetAttributes.Volume> volumes = List.of(
                new VolumeSetAttributes.Volume("disk", null, null, null, null)
        );
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes(null, null, null,
                volumes, null, null);
        volumeSetAttributes.setDiscoveryFQDN("fqdn1");
        CloudResource instanceResource = createResource(AZURE_INSTANCE, Map.of());
        List<CloudResource> buildableResource = Lists.newArrayList(
                instanceResource,
                createResource(AZURE_VOLUMESET, Map.of(CloudResource.ATTRIBUTES, volumeSetAttributes))
        );
        CloudInstance cloudInstance =
                new CloudInstance("instance", null, null, null, null, Map.of(FQDN, "nonmatchingfqdn"));
        AuthenticatedContext auth = mock(AuthenticatedContext.class);
        AzureClient azureClient = mock(AzureClient.class);
        VirtualMachine virtualMachine = mock(VirtualMachine.class);
        Disk disk = mock(Disk.class);
        when(virtualMachine.dataDisks()).thenReturn(Map.of());
        when(auth.getCloudContext()).thenReturn(mock(CloudContext.class));
        when(auth.getParameter(eq(AzureClient.class))).thenReturn(azureClient);
        when(azureClient.getVirtualMachineByResourceGroup(any(), any())).thenReturn(virtualMachine);
        when(azureClient.getDiskById(any())).thenReturn(disk);
        when(disk.id()).thenReturn("diskid");
        when(azureResourceGroupMetadataProvider.getResourceGroupName(any(CloudContext.class), any(CloudStack.class))).thenReturn("resourceGroup");
        AzureContext azureContext = mock(AzureContext.class);
        when(azureContext.getComputeResources(1L)).thenReturn(List.of(instanceResource));
        assertThrows(AzureResourceException.class, () ->
                underTest.build(azureContext, cloudInstance, 1L, auth, null, buildableResource, mock(CloudStack.class)),
                "Not possible to attach volume with FQDN nonmatchingfqdn to instance with FQDN fqdn1");
    }

    private CloudResource createResource(ResourceType resourceType, Map<String, Object> parameters) {
        return CloudResource.builder()
                .withType(resourceType)
                .withStatus(CommonStatus.CREATED)
                .withName(resourceType.name())
                .withParameters(parameters)
                .withInstanceId(resourceType.name() + "1")
                .build();
    }
}

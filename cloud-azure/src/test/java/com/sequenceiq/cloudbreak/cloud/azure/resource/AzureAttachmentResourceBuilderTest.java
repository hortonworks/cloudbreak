package com.sequenceiq.cloudbreak.cloud.azure.resource;

import static com.sequenceiq.cloudbreak.cloud.model.CloudInstance.FQDN;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_INSTANCE;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_VOLUMESET;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.resourcemanager.compute.models.Disk;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import com.azure.resourcemanager.compute.models.VirtualMachineDataDisk;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.context.AzureContext;
import com.sequenceiq.cloudbreak.cloud.azure.resource.domain.AzureDiskWithLun;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.constant.AzureConstants;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
public class AzureAttachmentResourceBuilderTest {

    private static final String TAG_NAME = "created-for";

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

    static Object[][] getDataForGetAvailableDisks() {
        return new Object[][] {
                {"fewDisksReturned", List.of(true, true, false)},
                {"NoneOfDisksReturned", List.of(true, true, true)},
                {"AllOfDisksReturned", List.of(false, false, false)}
        };
    }

    @ParameterizedTest(name = "testGetAvailableDisks{0}")
    @MethodSource("getDataForGetAvailableDisks")
    public void testGetAvailableDisks(String testName, List<Boolean> attached) {
        AuthenticatedContext auth = mock(AuthenticatedContext.class);
        AzureClient azureClient = mock(AzureClient.class);
        List<Disk> expectedAvailableDisks = new ArrayList<>();
        List<Disk> disks = new ArrayList<>();
        for (boolean attach: attached) {
            Disk disk = mock(Disk.class);
            when(disk.isAttachedToVirtualMachine()).thenReturn(attach);
            disks.add(disk);
            if (!attach) {
                expectedAvailableDisks.add(disk);
            }
        }
        CloudContext cloudContext = mock(CloudContext.class);
        CloudStack cloudStack = mock(CloudStack.class);
        List<String> tagValues = List.of("fqdn1", "fqdn2");
        when(auth.getCloudContext()).thenReturn(cloudContext);
        when(auth.getParameter(eq(AzureClient.class))).thenReturn(azureClient);
        when(azureClient.listDisksByTag("resourceGroup", TAG_NAME, tagValues)).thenReturn(disks);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, cloudStack)).thenReturn("resourceGroup");

        List<Disk> availableDisks = underTest.getAvailableDisks(auth, cloudStack, TAG_NAME, tagValues);

        assertEquals(true, CollectionUtils.isEqualCollection(expectedAvailableDisks, availableDisks));

    }

    private static Object [] [] getDataAttachDisks() {
        Pair<String, Integer> disk1 = createDiskNameLunPair("disk", 1);
        Pair<String, Integer> disk2 = createDiskNameLunPair("disk", 2);
        Pair<String, Integer> disk3 = createDiskNameLunPair("disk", 3);
        Pair<String, Integer> disk4 = createDiskNameLunPair("disk", 4);
        return new Object [] [] {
                { "FewDisksTobeAttachedOneDisk", List.of(disk1, disk2), List.of(disk1), List.of(disk2) },
                { "FewDisksTobeAttachedMultipleDisk", List.of(disk1, disk2, disk3), List.of(disk1), List.of(disk3, disk2) },
                { "NoDisksTobeAttached", List.of(disk1, disk2), List.of(disk1, disk2), List.of() },
                { "AllDisksTobeAttachedOneDisk", List.of(disk1), List.of(Pair.of("", -1)), List.of(disk1) },
                { "AllDisksTobeAttachedMultipleDisk", List.of(disk1, disk2, disk3), List.of(disk4), List.of(disk1, disk2, disk3) }
        };
    }

    private static Pair<String, Integer> createDiskNameLunPair(String namePrefix, int index) {
        return Pair.of(namePrefix + index, index);
    }

    @ParameterizedTest(name = "testAttachDisk{0}")
    @MethodSource("getDataAttachDisks")
    public void testAttachDisk(String testName, List<Pair<String, Integer>> disksToAttached, List<Pair<String, Integer>> disksIdsAlreadyAttached,
            List<Pair<String, Integer>> expectedDiskIdsToBeAttached) {
        AzureClient azureClient = mock(AzureClient.class);
        AuthenticatedContext auth = mock(AuthenticatedContext.class);
        VirtualMachine virtualMachine = mock(VirtualMachine.class);
        CloudContext cloudContext = mock(CloudContext.class);
        CloudStack cloudStack = mock(CloudStack.class);
        List<VolumeSetAttributes.Volume> volumes = new ArrayList<>();
        List<AzureDiskWithLun> expectedDiskToBeAttached = new ArrayList<>();
        for (Pair<String, Integer> volumeId : disksToAttached) {
            VolumeSetAttributes.Volume volume = new VolumeSetAttributes.Volume(volumeId.getLeft(), AzureConstants.LUN_DEVICE_PATH_PREFIX + volumeId.getRight(),
                    null, null, null);
            volumes.add(volume);
            Disk disk = mock(Disk.class);
            when(disk.id()).thenReturn(volumeId.getLeft());
            when(azureClient.getDiskById(volumeId.getLeft())).thenReturn(disk);
            if (expectedDiskIdsToBeAttached.contains(volumeId)) {
                expectedDiskToBeAttached.add(new AzureDiskWithLun(disk, volumeId.getRight()));
            }
        }
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes(null, null, null,
                volumes, null, null);
        volumeSetAttributes.setDiscoveryFQDN("fqdn1");
        CloudResource cloudResource = createResource(AZURE_VOLUMESET, Map.of(CloudResource.ATTRIBUTES, volumeSetAttributes));
        CloudInstance cloudInstance = new CloudInstance("instance", null, null, null, null,
                Map.of(FQDN, "fqdn1"));
        Map<Integer, VirtualMachineDataDisk> vmDisks = new HashMap<>();
        for (int index = 0; index < disksIdsAlreadyAttached.size(); index++) {
            VirtualMachineDataDisk virtualMachineDataDisk = mock(VirtualMachineDataDisk.class);
            when(virtualMachineDataDisk.id()).thenReturn(disksIdsAlreadyAttached.get(index).getLeft());
            vmDisks.put(index, virtualMachineDataDisk);
        }

        when(virtualMachine.dataDisks()).thenReturn(vmDisks);
        when(auth.getCloudContext()).thenReturn(cloudContext);
        when(auth.getParameter(eq(AzureClient.class))).thenReturn(azureClient);
        when(azureClient.getVirtualMachineByResourceGroup(any(), any())).thenReturn(virtualMachine);

        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, cloudStack)).thenReturn("resourceGroup");
        underTest.attachDisks(cloudInstance, auth, cloudResource, cloudStack);
        if (CollectionUtils.isEmpty(expectedDiskToBeAttached)) {
            verify(azureClient, never()).attachDisksToVmWithLun(any(), any());
        } else {
            ArgumentCaptor<List<AzureDiskWithLun>> captor = ArgumentCaptor.forClass(List.class);
            verify(azureClient).attachDisksToVmWithLun(captor.capture(), eq(virtualMachine));
            assertEquals(true, CollectionUtils.isEqualCollection(expectedDiskToBeAttached, captor.getValue()));
        }
    }

    @Test
    public void testFailAttachDiskWithNonMatchingFqdns() {
        AzureClient azureClient = mock(AzureClient.class);
        AuthenticatedContext auth = mock(AuthenticatedContext.class);
        VirtualMachine virtualMachine = mock(VirtualMachine.class);
        CloudContext cloudContext = mock(CloudContext.class);
        CloudStack cloudStack = mock(CloudStack.class);
        List<VolumeSetAttributes.Volume> volumes = List.of(
                new VolumeSetAttributes.Volume("disk", null, null, null, null)
        );
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes(null, null, null,
                volumes, null, null);
        volumeSetAttributes.setDiscoveryFQDN("fqdn1");
        CloudResource cloudResource = createResource(AZURE_VOLUMESET, Map.of(CloudResource.ATTRIBUTES, volumeSetAttributes));
        CloudInstance cloudInstance = new CloudInstance("instance", null, null, null, null,
                Map.of(FQDN, "nonmatchingfqdn"));
        Map<Integer, VirtualMachineDataDisk> vmDisks = new HashMap<>();
        when(virtualMachine.dataDisks()).thenReturn(vmDisks);
        when(auth.getCloudContext()).thenReturn(cloudContext);
        when(auth.getParameter(eq(AzureClient.class))).thenReturn(azureClient);
        when(azureClient.getVirtualMachineByResourceGroup(any(), any())).thenReturn(virtualMachine);
        Disk disk = mock(Disk.class);
        when(disk.id()).thenReturn("disk");
        when(azureClient.getDiskById("disk")).thenReturn(disk);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, cloudStack)).thenReturn("resourceGroup");

        assertThrows(AzureResourceException.class, () ->
                        underTest.attachDisks(cloudInstance, auth, cloudResource, cloudStack),
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

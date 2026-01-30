package com.sequenceiq.cloudbreak.cloud.azure.resource;

import static com.sequenceiq.cloudbreak.cloud.model.CloudInstance.FQDN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.OngoingStubbing;

import com.azure.resourcemanager.compute.models.Disk;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import com.azure.resourcemanager.compute.models.VirtualMachineDataDisk;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.azure.AzureCloudResourceService;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.azure.AzureVirtualMachineService;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.service.AzureResourceNameService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.RootVolumeFetchDto;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AzureResourceVolumeConnectorTest {

    private static final String GROUP_NAME = "compute";

    private static final String STACK_CRN = "STACK_CRN";

    private static final String STACK_NAME = "DataHub";

    private static final String TAG_NAME = "created-for";

    private static final int VOLUME_SIZE = 100;

    private static final String VOLUME_TYPE = "StandardSSD_LRS";

    private static final String RESOURCE_GROUP_NAME = "resourceGroup";

    @Mock
    private AzureVolumeResourceBuilder azureVolumeResourceBuilder;

    @InjectMocks
    private AzureResourceVolumeConnector underTest;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private AzureAttachmentResourceBuilder azureAttachmentResourceBuilder;

    @Mock
    private AzureResourceNameService resourceNameService;

    @Mock
    private AzureCloudResourceService azureCloudResourceService;

    @Mock
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Mock
    private AzureVirtualMachineService azureVirtualMachineService;

    @Test
    void testUpdateDiskVolumes() {
        List<String> volumeIds = List.of("test-vol-1");
        underTest.updateDiskVolumes(authenticatedContext, volumeIds, "test", 100);
        verify(azureVolumeResourceBuilder).modifyVolumes(authenticatedContext, volumeIds, "test", 100);
    }

    @Test
    void testUpdateDiskVolumesFail() {
        List<String> volumeIds = List.of("test-vol-1");
        doThrow(new RuntimeException("TEST")).when(azureVolumeResourceBuilder).modifyVolumes(authenticatedContext, volumeIds, "test", 100);
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> underTest.updateDiskVolumes(authenticatedContext, volumeIds, "test", 100));
        assertEquals("TEST", exception.getMessage());
        verify(azureVolumeResourceBuilder).modifyVolumes(authenticatedContext, volumeIds, "test", 100);
    }

    @Test
    void testDetachVolumes() throws Exception {
        List<CloudResource> resources = List.of();
        underTest.detachVolumes(authenticatedContext, resources);
        verify(azureVolumeResourceBuilder).detachVolumes(authenticatedContext, resources);
    }

    @Test
    void testDetachVolumesFail() {
        List<CloudResource> resources = List.of();
        doThrow(new RuntimeException("TEST")).when(azureVolumeResourceBuilder).detachVolumes(authenticatedContext, resources);
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> underTest.detachVolumes(authenticatedContext, resources));
        assertEquals("TEST", exception.getMessage());
        verify(azureVolumeResourceBuilder).detachVolumes(authenticatedContext, resources);
    }

    @Test
    void testDeleteVolumes() throws Exception {
        List<CloudResource> resources = List.of();
        underTest.deleteVolumes(authenticatedContext, resources);
        verify(azureVolumeResourceBuilder).deleteVolumes(authenticatedContext, resources);
    }

    @Test
    void testDeleteVolumesFail() {
        List<CloudResource> resources = List.of();
        doThrow(new RuntimeException("TEST")).when(azureVolumeResourceBuilder).deleteVolumes(authenticatedContext, resources);
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> underTest.deleteVolumes(authenticatedContext, resources));
        assertEquals("TEST", exception.getMessage());
        verify(azureVolumeResourceBuilder).deleteVolumes(authenticatedContext, resources);
    }

    static Object[] [] getDataForCreateVolumes() {
        return new Object [] [] {
                // {disksToAdd, List of CloudResource = {{existingDisksCount, availableDisks, availableDisksInDb}}
                // availableDisksInDb should be <= availableDisks and existingDisksCount
                // existingDisksCount = -1 represents that cloud resource does not exist.
                {2, List.of(new int[]{3, 0, 0})},
                {3, List.of(new int[]{3, 3, 0})},
                {3, List.of(new int[]{3, 3, 3})},
                {2, List.of(new int[]{3, 0, 0}, new int[] {3, 0, 0}, new int[]{3, 0, 0})},
                {3, List.of(new int[]{3, 1, 0}, new int[] {3, 0, 0}, new int[]{3, 0, 0})},
                {3, List.of(new int[]{3, 1, 0}, new int[] {3, 1, 1}, new int[]{3, 2, 1})},
                {2, List.of(new int[]{3, 2, 0}, new int[] {3, 2, 0}, new int[]{3, 2, 0})},
                {3, List.of(new int[]{4, 3, 3}, new int[] {3, 3, 3}, new int[]{4, 3, 3})},
                {2, List.of(new int[]{-1, 0, 0})},
                {2, List.of(new int[]{-1, 0, 0}, new int[]{-1, 0, 0})},
                {2, List.of(new int[]{-1, 2, 0}, new int[]{-1, 3, 0})}
        };
    }

    @ParameterizedTest(name = "testCreateVolumes{index}")
    @MethodSource("getDataForCreateVolumes")
    void testCreateVolumes(int numDisksToAdd, List<int []> existingDiskInfo) throws Exception {

        List<Disk> availableDisks = new ArrayList<>();
        List<CloudResource> resources = new ArrayList<>();
        List<CloudInstance> cloudInstances = new ArrayList<>();

        populateExistingDiskInformation(existingDiskInfo, resources, availableDisks, cloudInstances);

        CloudContext cloudContext = mock(CloudContext.class);
        when(cloudContext.getName()).thenReturn(STACK_NAME);
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        CloudStack cloudStack = mock(CloudStack.class);
        when(cloudStack.getParameters()).thenReturn(Map.of(PlatformParametersConsts.RESOURCE_CRN_PARAMETER, STACK_CRN));
        Group group = getGroup(GROUP_NAME);
        when(group.getInstances()).thenReturn(cloudInstances);
        boolean cloudResourceExist = cloudResourceExist(existingDiskInfo);
        if (!cloudResourceExist) {
            CloudResource cloudResource = populateCloudResource(0, 0, 0, existingDiskInfo.get(0)[1], availableDisks);
            OngoingStubbing ongoingStubbing = when(azureVolumeResourceBuilder.createVolumeSet(eq(1L), eq(authenticatedContext),
                    eq(group), any(CloudResource.class), eq(STACK_CRN), eq(false), any())).thenReturn(cloudResource);

            for (int index = 1; index < existingDiskInfo.size(); index++) {
                cloudResource = populateCloudResource(index, 0, 0, existingDiskInfo.get(index)[1], availableDisks);
                ongoingStubbing = ongoingStubbing.thenReturn(cloudResource);
            }
        }

        when(azureAttachmentResourceBuilder.getAvailableDisks(authenticatedContext, cloudStack, TAG_NAME, group.getInstances().stream().
                map(instance -> instance.getParameter(FQDN, String.class)).collect(Collectors.toList()))).thenReturn(availableDisks);

        underTest.createVolumes(authenticatedContext, group,
                new VolumeSetAttributes.Volume(null, null, VOLUME_SIZE, VOLUME_TYPE, null), cloudStack, numDisksToAdd,
                resources);

        Map<Integer, Integer> attachDiskInvocation = new HashMap<>();
        for (int count = 0; count < existingDiskInfo.size(); count++) {
            int availableDisksToAdd = existingDiskInfo.get(count)[1] - existingDiskInfo.get(count)[2];
            int newDisksToAdd = Math.max(0, numDisksToAdd - existingDiskInfo.get(count)[1]);
            for (int index = 0; index < newDisksToAdd; index++) {
                int indexDisk = Math.max(0, existingDiskInfo.get(count)[0]) + availableDisksToAdd + index;
                attachDiskInvocation.put(indexDisk, attachDiskInvocation.getOrDefault(indexDisk, 0) + 1);
            }
            if (cloudResourceExist) {
                assertEquals(existingDiskInfo.get(count)[0] + availableDisksToAdd + newDisksToAdd, getVolumes(resources.get(count)).size());
                if (newDisksToAdd > 0) {
                    verify(azureVolumeResourceBuilder).build(group.getInstances().get(count), authenticatedContext, group, List.of(resources.get(count)),
                            cloudStack, existingDiskInfo.get(count)[0] + 1, Map.of(TAG_NAME, String.format("fqdn%s", count)));
                }
            }
        }
        attachDiskInvocation.entrySet().stream().forEach(entry -> {
            verify(resourceNameService, times(entry.getValue())).attachedDisk(eq(STACK_NAME), eq(GROUP_NAME), eq(1L),
                    eq(entry.getKey()), any());
        });
        if (!cloudResourceExist) {
            verify(azureVolumeResourceBuilder, times(existingDiskInfo.size())).createVolumeSet(eq(1L), eq(authenticatedContext),
                    eq(group), any(CloudResource.class), eq(STACK_CRN),
                    eq(false), any());
        }
    }

    @Test
    void testCreateVolumesInstanceNotFound() {

        List<CloudResource> resources = new ArrayList<>();
        List<CloudInstance> cloudInstances = new ArrayList<>();
        CloudInstance cloudInstance = mock(CloudInstance.class);
        when(cloudInstance.getInstanceId()).thenReturn("instance0");
        when(cloudInstance.getStringParameter(FQDN)).thenReturn("fqdn0");
        cloudInstances.add(cloudInstance);
        CloudResource cloudResource = mock(CloudResource.class);
        when(cloudResource.getInstanceId()).thenReturn("instance1");
        resources.add(cloudResource);
        CloudStack cloudStack = mock(CloudStack.class);
        Group group = getGroup(GROUP_NAME);
        when(group.getInstances()).thenReturn(cloudInstances);


        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.createVolumes(authenticatedContext, group,
                        new VolumeSetAttributes.Volume(null, null, VOLUME_SIZE, VOLUME_TYPE, null), cloudStack, 0,
                        resources));

        assertEquals("Instance :instance1 not found", exception.getMessage());
    }

    @Test
    void testCreateVolumesFailPropagateException() {
        List<CloudInstance> cloudInstances = List.of(getCloudInstance("instance"));
        CloudStack cloudStack = mock(CloudStack.class);
        Group group = getGroup(GROUP_NAME);
        when(cloudStack.getGroups()).thenReturn(List.of(group));
        when(group.getInstances()).thenReturn(cloudInstances);
        doThrow(new RuntimeException("TEST")).when(azureVolumeResourceBuilder).createVolumeSet(eq(1L), any(), any(), any(), any(), eq(false), any());
        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.createVolumes(authenticatedContext, group,
                        new VolumeSetAttributes.Volume(null, null, VOLUME_SIZE, VOLUME_TYPE, null),
                        cloudStack, 0, List.of()));
        assertEquals("TEST", exception.getMessage());
    }

    @ParameterizedTest(name = "testAttachVolumesFor{index}")
    @ValueSource(ints = {0, 1, 5})
    void testAttachVolumes(int numVolumeSets) {
        List<CloudResource> resources = new ArrayList<>();
        List<CloudInstance> cloudInstances = new ArrayList<>();
        createCloudResourcesForAttachDisks(numVolumeSets, resources, cloudInstances);

        CloudStack cloudStack = mock(CloudStack.class);
        Group group = getGroup(GROUP_NAME);
        when(cloudStack.getGroups()).thenReturn(List.of(group));
        when(group.getInstances()).thenReturn(cloudInstances);

        underTest.attachVolumes(authenticatedContext, resources, cloudStack);

        for (int count = 0; count < numVolumeSets; count++) {
            verify(azureAttachmentResourceBuilder).attachDisks(group.getInstances().get(count), authenticatedContext, resources.get(count), cloudStack);
        }

        if (numVolumeSets == 0) {
            verify(azureAttachmentResourceBuilder, never()).attachDisks(any(), any(), any(), any());
        }
    }

    @Test
    void testAttachVolumesGroupNotFound() {
        CloudResource cloudResource = getCloudResource("compute1", null);
        CloudStack cloudStack = mock(CloudStack.class);
        Group group = mock(Group.class);
        when(group.getName()).thenReturn(GROUP_NAME);
        when(cloudStack.getGroups()).thenReturn(List.of(group));

        List<CloudResource> resources = List.of(cloudResource);

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.attachVolumes(authenticatedContext, resources, cloudStack));
        assertEquals("Instance Group :compute1 not found for cluster", exception.getMessage());
    }

    @Test
    void testAttachVolumesInstanceNotFound() {
        CloudResource cloudResource = getCloudResource(GROUP_NAME, "instance0");
        CloudStack cloudStack = mock(CloudStack.class);
        Group group = getGroup(GROUP_NAME);
        CloudInstance cloudInstance = getCloudInstance("instance1");
        when(group.getInstances()).thenReturn(List.of(cloudInstance));
        when(cloudStack.getGroups()).thenReturn(List.of(group));

        List<CloudResource> resources = List.of(cloudResource);

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.attachVolumes(authenticatedContext, resources, cloudStack));

        assertEquals("Instance :instance0 not found for cluster", exception.getMessage());
    }

    @Test
    void testAttachVolumesFail() {
        List<CloudResource> resources = new ArrayList<>();
        List<CloudInstance> cloudInstances = new ArrayList<>();
        createCloudResourcesForAttachDisks(1, resources, cloudInstances);

        CloudStack cloudStack = mock(CloudStack.class);
        Group group = getGroup(GROUP_NAME);
        when(cloudStack.getGroups()).thenReturn(List.of(group));
        when(group.getInstances()).thenReturn(cloudInstances);
        doThrow(new RuntimeException("TEST")).when(azureAttachmentResourceBuilder).attachDisks(cloudInstances.get(0), authenticatedContext,
                resources.get(0), cloudStack);
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> underTest.attachVolumes(authenticatedContext, resources, cloudStack));
        assertEquals("TEST", exception.getMessage());
    }

    @Test
    void testGetRootVolumes() throws Exception {
        Group group = mock(Group.class);
        CloudResource cloudResource = mock(CloudResource.class);
        List<CloudResource> cloudResourceList = List.of(cloudResource);
        AzureClient azureClient = mock(AzureClient.class);
        when(authenticatedContext.getParameter(eq(AzureClient.class))).thenReturn(azureClient);
        String resourceGroupName = "test-resource-group";
        when(azureCloudResourceService.getAttachedOsDiskResources(cloudResourceList, resourceGroupName, azureClient)).thenReturn(cloudResourceList);
        RootVolumeFetchDto rootVolumeFetchDto = new RootVolumeFetchDto(authenticatedContext, group, resourceGroupName, cloudResourceList);

        List<CloudResource> result = underTest.getRootVolumes(rootVolumeFetchDto);
        assertEquals(cloudResourceList, result);
        verify(azureCloudResourceService).getAttachedOsDiskResources(cloudResourceList, resourceGroupName, azureClient);
    }

    @Test
    void testGetAttachedVolumeCountPerInstance() {
        Map<String, Integer> expected = Map.of("instance1", 1, "instance2", 3);
        CloudStack cloudStack = mock(CloudStack.class);
        CloudContext cloudContext = mock(CloudContext.class);
        AzureClient azureClient = mock(AzureClient.class);
        VirtualMachine vm1 = mock(VirtualMachine.class);
        when(vm1.name()).thenReturn("instance1");
        when(vm1.dataDisks())
                .thenReturn(Map.of(0, mock(VirtualMachineDataDisk.class)));
        VirtualMachine vm2 = mock(VirtualMachine.class);
        when(vm2.name()).thenReturn("instance2");
        when(vm2.dataDisks())
                .thenReturn(Map.of(0, mock(VirtualMachineDataDisk.class), 1, mock(VirtualMachineDataDisk.class), 2, mock(VirtualMachineDataDisk.class)));
        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, cloudStack)).thenReturn(RESOURCE_GROUP_NAME);
        when(azureVirtualMachineService.getVirtualMachinesByName(azureClient, RESOURCE_GROUP_NAME, List.of("instance1", "instance2")))
                .thenReturn(Map.of("instance1", vm1, "instance2", vm2));

        Map<String, Integer> result = underTest.getAttachedVolumeCountPerInstance(authenticatedContext, cloudStack, List.of("instance1", "instance2"));

        assertThat(result).containsExactlyInAnyOrderEntriesOf(expected);
    }

    private CloudResource getCloudResource(String groupName, String instanceName) {
        CloudResource cloudResource = mock(CloudResource.class);
        when(cloudResource.getGroup()).thenReturn(groupName);
        when(cloudResource.getInstanceId()).thenReturn(instanceName);
        return cloudResource;
    }

    private Group getGroup(String groupName) {
        Group group = mock(Group.class);
        when(group.getName()).thenReturn(groupName);
        InstanceTemplate instanceTemplate = mock(InstanceTemplate.class);
        when(instanceTemplate.getVolumes()).thenReturn(new ArrayList<>());
        when(instanceTemplate.getTemporaryStorageCount()).thenReturn(1L);
        when(group.getReferenceInstanceTemplate()).thenReturn(instanceTemplate);
        return group;
    }

    private CloudInstance getCloudInstance(String instanceId) {
        CloudInstance cloudInstance = mock(CloudInstance.class);
        InstanceTemplate instanceTemplate = mock(InstanceTemplate.class);
        when(instanceTemplate.getPrivateId()).thenReturn(1L);
        when(cloudInstance.getInstanceId()).thenReturn(instanceId);
        when(cloudInstance.getTemplate()).thenReturn(instanceTemplate);
        return cloudInstance;
    }

    private void createCloudResourcesForAttachDisks(int numVolumeSets, List<CloudResource> resources, List<CloudInstance> cloudInstances) {
        for (int count = 0; count < numVolumeSets; count++) {
            String instanceName = String.format("instance%s", count);
            CloudResource cloudResource = mock(CloudResource.class);
            when(cloudResource.getGroup()).thenReturn("compute");
            when(cloudResource.getInstanceId()).thenReturn(instanceName);
            CloudInstance cloudInstance = mock(CloudInstance.class);
            when(cloudInstance.getInstanceId()).thenReturn(instanceName);
            resources.add(cloudResource);
            cloudInstances.add(cloudInstance);
        }

    }

    private List<VolumeSetAttributes.Volume> getVolumes(CloudResource resource) {
        return resource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class).getVolumes();
    }

    private void populateExistingDiskInformation(List<int []> existingDiskInfo, List<CloudResource> resources, List<Disk> availableDisks,
            List<CloudInstance> cloudInstances) {
        for (int count = 0; count < existingDiskInfo.size(); count++) {
            String instanceName = String.format("instance%s", count);
            String fqdn = String.format("fqdn%s", count);
            CloudInstance cloudInstance = getCloudInstance(instanceName);
            when(cloudInstance.getParameter(FQDN, String.class)).thenReturn(fqdn);
            when(cloudInstance.getStringParameter(FQDN)).thenReturn(fqdn);
            cloudInstances.add(cloudInstance);
        }
        if (cloudResourceExist(existingDiskInfo)) {
            for (int count = 0; count < existingDiskInfo.size(); count++) {
                CloudResource cloudResource = populateCloudResource(count, existingDiskInfo.get(count)[0], existingDiskInfo.get(count)[2],
                        existingDiskInfo.get(count)[1], availableDisks);
                resources.add(cloudResource);
            }
        }
    }

    private CloudResource populateCloudResource(int index, int numVolumes, int availableVolumesInDb, int availableVolumes, List<Disk> availableDisks) {
        List<String> availableVolumeIds = new ArrayList<>();
        String instanceName = String.format("instance%s", index);
        String fqdn = String.format("fqdn%s", index);
        int volumeCount = 0;
        CloudResource cloudResource = mock(CloudResource.class);
        when(cloudResource.getInstanceId()).thenReturn(instanceName);
        ArrayList<VolumeSetAttributes.Volume> volumes = new ArrayList<>();
        while (volumeCount < numVolumes) {
            String volumeId = String.format("volume-%s%s", index, volumeCount);
            if (availableVolumeIds.size() < availableVolumesInDb) {
                availableVolumeIds.add(volumeId);
            }
            volumes.add(new VolumeSetAttributes.Volume(volumeId, null, VOLUME_SIZE, VOLUME_TYPE, null));
            volumeCount++;
        }
        when(cloudResource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class)).thenReturn(new VolumeSetAttributes(null,
                true, null, volumes, null, null));
        while (availableVolumeIds.size() < availableVolumes) {
            availableVolumeIds.add(String.format("volume-%s%s", index, volumeCount++));
        }
        availableDisks.addAll(availableVolumeIds.stream().map(volumeId -> {
            Disk disk = mock(Disk.class);
            when(disk.tags()).thenReturn(Map.of(TAG_NAME, fqdn));
            when(disk.id()).thenReturn(volumeId);
            return disk;
        }).collect(Collectors.toList()));
        return cloudResource;
    }

    private boolean cloudResourceExist(List<int []> existingDiskInfo) {
        return existingDiskInfo.stream().noneMatch(diskInfo -> diskInfo[0] == -1);
    }
}

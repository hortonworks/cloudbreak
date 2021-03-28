package com.sequenceiq.cloudbreak.cloud.azure.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.task.AsyncTaskExecutor;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.compute.Disk;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.azure.AzureDiskType;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.context.AzureContext;
import com.sequenceiq.cloudbreak.cloud.azure.service.AzureResourceNameService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.model.instance.AzureInstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.template.compute.PreserveResourceException;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AzureVolumeResourceBuilderTest {
    private static final long PRIVATE_ID = 1L;

    private static final String STACK_CRN = "crn";

    private static final String REGION = "westus2";

    private static final String VOLUME_ID = "volume-1";

    private static final int VOLUME_SIZE = 100;

    private static final String VOLUME_TYPE = "StandardSSD_LRS";

    private static final String RESOURCE_GROUP = "my-rg";

    private static final String DISK_ID = "disk-1";

    private static final String VOLUME_NAME = "volume";

    private static final String AVAILABILITY_ZONE = "az";

    private static final String FSTAB = "fstab";

    private static final String DEVICE = "device";

    private static final String DEVICE_DEV_SDC = "/dev/sdc";

    private static final String DISK_ENCRYPTION_SET_ID = "diskEncryptionSetId";

    @Mock
    private AzureContext context;

    @Mock
    private AuthenticatedContext auth;

    @Mock
    private Group group;

    @Mock
    private Image image;

    @Mock
    private InstanceTemplate instanceTemplate;

    @Mock
    private Volume volumeTemplate;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private AzureClient azureClient;

    @InjectMocks
    private AzureVolumeResourceBuilder underTest;

    @Mock
    private AsyncTaskExecutor intermediateBuilderExecutor;

    @Mock
    private PersistenceNotifier resourceNotifier;

    @Mock
    private AzureUtils azureUtils;

    @Mock
    private AzureResourceNameService resourceNameService;

    @Mock
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Mock
    private CloudStack cloudStack;

    @Captor
    private ArgumentCaptor<Collection<String>> collectionCaptor;

    @Mock
    private CloudInstance cloudInstance;

    @Mock
    private Future<?> future;

    @Mock
    private Disk disk;

    @BeforeEach
    public void setUp() {
        CloudResource cloudResource1 = mock(CloudResource.class);
        CloudResource cloudResource2 = mock(CloudResource.class);
        when(context.getComputeResources(PRIVATE_ID)).thenReturn(List.of(cloudResource1, cloudResource2));
        when(context.getStringParameter(PlatformParametersConsts.RESOURCE_CRN_PARAMETER)).thenReturn(STACK_CRN);

        when(group.getReferenceInstanceTemplate()).thenReturn(instanceTemplate);
        when(group.getReferenceInstanceConfiguration()).thenReturn(cloudInstance);
        when(cloudInstance.getTemplate()).thenReturn(instanceTemplate);
        when(instanceTemplate.getVolumes()).thenReturn(List.of(volumeTemplate));

        when(auth.getCloudContext()).thenReturn(cloudContext);
        when(auth.getParameter(AzureClient.class)).thenReturn(azureClient);

        when(resourceNameService.resourceName(eq(ResourceType.AZURE_VOLUMESET), any(), any(), eq(PRIVATE_ID), eq(STACK_CRN))).thenReturn("someResourceName");

        Region region = Region.region(REGION);
        Location location = Location.location(region);
        when(cloudContext.getLocation()).thenReturn(location);

        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, cloudStack)).thenReturn(RESOURCE_GROUP);

        when(disk.id()).thenReturn(DISK_ID);
    }

    @Test
    public void testWhenComputeResourceIsNullThenNullReturns() {
        when(context.getComputeResources(anyLong())).thenReturn(null);

        List<CloudResource> result = underTest.create(context, PRIVATE_ID, auth, group, image);

        assertNull(result);
    }

    @Test
    public void testWhenComputeResourceIsEmptyThenNullReturns() {
        when(context.getComputeResources(anyLong())).thenReturn(Collections.emptyList());

        List<CloudResource> result = underTest.create(context, PRIVATE_ID, auth, group, image);

        assertNull(result);
    }

    @Test
    public void testWhenDetachedReattachableVolumeExistsThenItShouldReturn() {
        CloudResource volumeSetResource = CloudResource.builder().type(ResourceType.AZURE_VOLUMESET).status(CommonStatus.DETACHED)
                .name(VOLUME_NAME).params(Map.of()).build();
        CloudResource newInstance = CloudResource.builder().instanceId("instanceid").type(ResourceType.AZURE_INSTANCE).status(CommonStatus.CREATED)
                .name("instance").params(Map.of()).build();
        when(context.getComputeResources(PRIVATE_ID)).thenReturn(List.of(volumeSetResource, newInstance));

        List<CloudResource> result = underTest.create(context, PRIVATE_ID, auth, group, image);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(volumeSetResource, result.get(0));
    }

    @Test
    public void testWhenReattachableVolumeWithInstanceIdExistsThenItShouldReturn() {
        CloudResource volumeSetResource = CloudResource.builder().type(ResourceType.AZURE_VOLUMESET).status(CommonStatus.CREATED).instanceId("instanceid")
                .name(VOLUME_NAME).params(Map.of()).build();
        CloudResource newInstance = CloudResource.builder().instanceId("instanceid").type(ResourceType.AZURE_INSTANCE).status(CommonStatus.CREATED)
                .name("instance").params(Map.of()).build();
        when(context.getComputeResources(PRIVATE_ID)).thenReturn(List.of(volumeSetResource, newInstance));

        List<CloudResource> result = underTest.create(context, PRIVATE_ID, auth, group, image);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(volumeSetResource, result.get(0));
    }

    @Test
    public void testWhenReattachableDoesNotExistsThenNewlyBuildedInstanceShouldBeCreated() {
        CloudResource volumeSetResource = CloudResource.builder().type(ResourceType.AZURE_VOLUMESET).status(CommonStatus.CREATED)
                .name(VOLUME_NAME).params(Map.of()).build();
        CloudResource newInstance = CloudResource.builder().instanceId("instanceid").type(ResourceType.AZURE_INSTANCE).status(CommonStatus.CREATED)
                .name("instance").params(Map.of()).build();
        when(context.getComputeResources(PRIVATE_ID)).thenReturn(List.of(volumeSetResource, newInstance));

        List<CloudResource> result = underTest.create(context, PRIVATE_ID, auth, group, image);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotEquals(volumeSetResource, result.get(0));
    }

    @Test
    public void deleteTestWhenDiskIsDeletedOnAzure() throws PreserveResourceException {
        CloudResource mock = CloudResource.builder().type(ResourceType.AZURE_RESOURCE_GROUP).name("resource-group").build();
        when(context.getNetworkResources()).thenReturn(List.of(mock));
        ArrayList<VolumeSetAttributes.Volume> volumes = new ArrayList<>();
        volumes.add(new VolumeSetAttributes.Volume(VOLUME_ID, DEVICE, VOLUME_SIZE, "ssd", CloudVolumeUsageType.GENERAL));
        CloudResource volumeSetResource = CloudResource.builder().type(ResourceType.AZURE_VOLUMESET).status(CommonStatus.CREATED)
                .params(Map.of(CloudResource.ATTRIBUTES, new VolumeSetAttributes(AVAILABILITY_ZONE, true, "", volumes, VOLUME_SIZE, "ssd")))
                .name(VOLUME_NAME).build();
        PagedList<Disk> pagedList = mock(PagedList.class);
        when(azureClient.listDisksByResourceGroup(eq("resource-group"))).thenReturn(pagedList);

        underTest.delete(context, auth, volumeSetResource);

        verify(azureUtils, times(0)).deleteManagedDisks(any(), any());
    }

    @Test
    public void deleteTestWhenDiskIsOnAzureAndNotAttached() throws PreserveResourceException {
        CloudResource mock = CloudResource.builder().type(ResourceType.AZURE_RESOURCE_GROUP).name("resource-group").build();
        when(context.getNetworkResources()).thenReturn(List.of(mock));
        ArrayList<VolumeSetAttributes.Volume> volumes = new ArrayList<>();
        volumes.add(new VolumeSetAttributes.Volume("vol1", DEVICE, VOLUME_SIZE, "ssd", CloudVolumeUsageType.GENERAL));
        CloudResource volumeSetResource = CloudResource.builder().type(ResourceType.AZURE_VOLUMESET).status(CommonStatus.CREATED)
                .params(Map.of(
                        CloudResource.ATTRIBUTES, new VolumeSetAttributes(AVAILABILITY_ZONE, true, "", volumes, VOLUME_SIZE, "ssd")
                ))
                .name(VOLUME_NAME).build();
        List<Disk> diskList = new ArrayList<>();
        Disk disk1 = mock(Disk.class);
        when(disk1.id()).thenReturn("vol1");
        Disk disk2 = mock(Disk.class);
        when(disk2.id()).thenReturn("vol2");
        Disk disk3 = mock(Disk.class);
        when(disk3.id()).thenReturn("vol3");
        PagedList<Disk> pagedList = mock(PagedList.class);
        diskList.add(disk1);
        diskList.add(disk2);
        diskList.add(disk3);
        Disk disk = mock(Disk.class);
        when(disk.isAttachedToVirtualMachine()).thenReturn(false);
        when(azureClient.getDiskById(any())).thenReturn(disk);
        when(pagedList.stream()).thenAnswer(invocation -> diskList.stream());
        when(azureClient.listDisksByResourceGroup(eq("resource-group"))).thenReturn(pagedList);

        underTest.delete(context, auth, volumeSetResource);

        verify(azureUtils, times(1)).deleteManagedDisks(any(), collectionCaptor.capture());
        verify(azureClient, times(0)).getVirtualMachine(any());
        verify(azureClient, times(0)).detachDiskFromVm(any(), any());
        Collection<String> deletedAzureManagedDisks = collectionCaptor.getValue();
        assertThat(deletedAzureManagedDisks).contains("vol1");
    }

    @Test
    public void deleteTestWhenDiskIsOnAzureAndAttached() throws PreserveResourceException {
        CloudResource mock = CloudResource.builder().type(ResourceType.AZURE_RESOURCE_GROUP).name("resource-group").build();
        when(context.getNetworkResources()).thenReturn(List.of(mock));
        ArrayList<VolumeSetAttributes.Volume> volumes = new ArrayList<>();
        volumes.add(new VolumeSetAttributes.Volume("vol1", DEVICE, VOLUME_SIZE, "ssd", CloudVolumeUsageType.GENERAL));
        CloudResource volumeSetResource = CloudResource.builder().type(ResourceType.AZURE_VOLUMESET).status(CommonStatus.CREATED)
                .params(Map.of(
                        CloudResource.ATTRIBUTES, new VolumeSetAttributes(AVAILABILITY_ZONE, true, "", volumes, VOLUME_SIZE, "ssd")
                ))
                .instanceId("instance1")
                .name(VOLUME_NAME).build();
        List<Disk> diskList = new ArrayList<>();
        Disk disk1 = mock(Disk.class);
        when(disk1.id()).thenReturn("vol1");
        when(disk1.isAttachedToVirtualMachine()).thenReturn(true);
        Disk disk2 = mock(Disk.class);
        when(disk2.id()).thenReturn("vol2");
        when(disk2.isAttachedToVirtualMachine()).thenReturn(true);
        Disk disk3 = mock(Disk.class);
        when(disk3.id()).thenReturn("vol3");
        when(disk3.isAttachedToVirtualMachine()).thenReturn(true);
        PagedList<Disk> pagedList = mock(PagedList.class);
        diskList.add(disk1);
        diskList.add(disk2);
        diskList.add(disk3);
        Disk disk = mock(Disk.class);
        when(disk.isAttachedToVirtualMachine()).thenReturn(true);
        when(disk.virtualMachineId()).thenReturn("instance1");
        when(azureClient.getDiskById(any())).thenReturn(disk);
        when(pagedList.stream()).thenAnswer(invocation -> diskList.stream());
        when(azureClient.listDisksByResourceGroup(eq("resource-group"))).thenReturn(pagedList);
        VirtualMachine virtualMachine = mock(VirtualMachine.class);
        when(azureClient.getVirtualMachine(eq("instance1"))).thenReturn(virtualMachine);

        underTest.delete(context, auth, volumeSetResource);

        verify(azureClient, times(1)).getVirtualMachine(eq("instance1"));
        verify(azureClient, times(1)).detachDiskFromVm(eq("vol1"), eq(virtualMachine));
        verify(azureUtils, times(1)).deleteManagedDisks(any(), collectionCaptor.capture());
        Collection<String> deletedAzureManagedDisks = collectionCaptor.getValue();
        assertThat(deletedAzureManagedDisks).contains("vol1");
    }

    @Test
    void buildTestWhenNoVolumeSet() throws Exception {
        List<CloudResource> result = underTest.build(context, PRIVATE_ID, auth, group, List.of(), cloudStack);

        verify(azureClient, never()).createManagedDisk(anyString(), anyInt(), any(AzureDiskType.class), anyString(), anyString(), anyMap(), anyString());

        assertThat(result).isNotNull();
        assertThat(result).hasSize(0);
    }

    @Test
    void buildTestWhenVolumeSetExistsAndCreated() throws Exception {
        ArrayList<VolumeSetAttributes.Volume> volumes = new ArrayList<>();
        volumes.add(new VolumeSetAttributes.Volume(VOLUME_ID, DEVICE, VOLUME_SIZE, VOLUME_TYPE, CloudVolumeUsageType.GENERAL));
        CloudResource volumeSetResource = CloudResource.builder().type(ResourceType.AZURE_VOLUMESET).status(CommonStatus.CREATED)
                .params(Map.of(CloudResource.ATTRIBUTES, new VolumeSetAttributes(AVAILABILITY_ZONE, true, FSTAB, volumes, VOLUME_SIZE, VOLUME_TYPE)))
                .name(VOLUME_NAME).build();

        List<CloudResource> result = underTest.build(context, PRIVATE_ID, auth, group, List.of(volumeSetResource), cloudStack);

        verify(azureClient, never()).createManagedDisk(anyString(), anyInt(), any(AzureDiskType.class), anyString(), anyString(), anyMap(), anyString());

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);

        verifyVolumeSetResource(result.get(0), VOLUME_ID, DEVICE);
    }

    @Test
    void buildTestWhenVolumeSetExistsAndRequestedAndExistingDisk() throws Exception {
        initAsyncTaskExecutor();

        ArrayList<VolumeSetAttributes.Volume> volumes = new ArrayList<>();
        volumes.add(new VolumeSetAttributes.Volume(VOLUME_ID, DEVICE, VOLUME_SIZE, VOLUME_TYPE, CloudVolumeUsageType.GENERAL));
        CloudResource volumeSetResource = CloudResource.builder().type(ResourceType.AZURE_VOLUMESET).status(CommonStatus.REQUESTED)
                .params(Map.of(CloudResource.ATTRIBUTES, new VolumeSetAttributes(AVAILABILITY_ZONE, true, FSTAB, volumes, VOLUME_SIZE, VOLUME_TYPE)))
                .name(VOLUME_NAME).build();

        when(azureClient.getDiskByName(RESOURCE_GROUP, VOLUME_ID)).thenReturn(disk);

        List<CloudResource> result = underTest.build(context, PRIVATE_ID, auth, group, List.of(volumeSetResource), cloudStack);

        verify(azureClient, never()).createManagedDisk(anyString(), anyInt(), any(AzureDiskType.class), anyString(), anyString(), anyMap(), anyString());

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);

        verifyVolumeSetResource(result.get(0), DISK_ID, DEVICE_DEV_SDC);
    }

    @Test
    void buildTestWhenVolumeSetExistsAndRequestedAndNewDiskAndNoDiskEncryptionSetId() throws Exception {
        initAsyncTaskExecutor();

        ArrayList<VolumeSetAttributes.Volume> volumes = new ArrayList<>();
        volumes.add(new VolumeSetAttributes.Volume(VOLUME_ID, DEVICE, VOLUME_SIZE, VOLUME_TYPE, CloudVolumeUsageType.GENERAL));
        CloudResource volumeSetResource = CloudResource.builder().type(ResourceType.AZURE_VOLUMESET).status(CommonStatus.REQUESTED)
                .params(Map.of(CloudResource.ATTRIBUTES, new VolumeSetAttributes(AVAILABILITY_ZONE, true, FSTAB, volumes, VOLUME_SIZE, VOLUME_TYPE)))
                .name(VOLUME_NAME).build();

        when(azureClient.createManagedDisk(VOLUME_ID, VOLUME_SIZE, AzureDiskType.STANDARD_SSD_LRS, REGION, RESOURCE_GROUP, Map.of(), null)).thenReturn(disk);

        List<CloudResource> result = underTest.build(context, PRIVATE_ID, auth, group, List.of(volumeSetResource), cloudStack);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);

        verifyVolumeSetResource(result.get(0), DISK_ID, DEVICE_DEV_SDC);
    }

    @Test
    void buildTestWhenVolumeSetExistsAndRequestedAndNewDiskAndDiskEncryptionSetIdGivenButIneffective() throws Exception {
        initAsyncTaskExecutor();

        ArrayList<VolumeSetAttributes.Volume> volumes = new ArrayList<>();
        volumes.add(new VolumeSetAttributes.Volume(VOLUME_ID, DEVICE, VOLUME_SIZE, VOLUME_TYPE, CloudVolumeUsageType.GENERAL));
        CloudResource volumeSetResource = CloudResource.builder().type(ResourceType.AZURE_VOLUMESET).status(CommonStatus.REQUESTED)
                .params(Map.of(CloudResource.ATTRIBUTES, new VolumeSetAttributes(AVAILABILITY_ZONE, true, FSTAB, volumes, VOLUME_SIZE, VOLUME_TYPE)))
                .name(VOLUME_NAME).build();

        when(instanceTemplate.getStringParameter(AzureInstanceTemplate.DISK_ENCRYPTION_SET_ID)).thenReturn(DISK_ENCRYPTION_SET_ID);

        when(azureClient.createManagedDisk(VOLUME_ID, VOLUME_SIZE, AzureDiskType.STANDARD_SSD_LRS, REGION, RESOURCE_GROUP, Map.of(), null)).thenReturn(disk);

        List<CloudResource> result = underTest.build(context, PRIVATE_ID, auth, group, List.of(volumeSetResource), cloudStack);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);

        verifyVolumeSetResource(result.get(0), DISK_ID, DEVICE_DEV_SDC);
    }

    @Test
    void buildTestWhenVolumeSetExistsAndRequestedAndNewDiskAndDiskEncryptionSetIdGivenAndEffective() throws Exception {
        initAsyncTaskExecutor();

        ArrayList<VolumeSetAttributes.Volume> volumes = new ArrayList<>();
        volumes.add(new VolumeSetAttributes.Volume(VOLUME_ID, DEVICE, VOLUME_SIZE, VOLUME_TYPE, CloudVolumeUsageType.GENERAL));
        CloudResource volumeSetResource = CloudResource.builder().type(ResourceType.AZURE_VOLUMESET).status(CommonStatus.REQUESTED)
                .params(Map.of(CloudResource.ATTRIBUTES, new VolumeSetAttributes(AVAILABILITY_ZONE, true, FSTAB, volumes, VOLUME_SIZE, VOLUME_TYPE)))
                .name(VOLUME_NAME).build();

        when(instanceTemplate.getParameter(AzureInstanceTemplate.MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED, Object.class)).thenReturn(true);
        when(instanceTemplate.getStringParameter(AzureInstanceTemplate.DISK_ENCRYPTION_SET_ID)).thenReturn(DISK_ENCRYPTION_SET_ID);

        when(azureClient.createManagedDisk(VOLUME_ID, VOLUME_SIZE, AzureDiskType.STANDARD_SSD_LRS, REGION, RESOURCE_GROUP, Map.of(), DISK_ENCRYPTION_SET_ID))
                .thenReturn(disk);

        List<CloudResource> result = underTest.build(context, PRIVATE_ID, auth, group, List.of(volumeSetResource), cloudStack);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);

        verifyVolumeSetResource(result.get(0), DISK_ID, DEVICE_DEV_SDC);
    }

    private void verifyVolumeSetResource(CloudResource resultVolumeSetResource, String diskIdExpected, String deviceExpected) {
        assertThat(resultVolumeSetResource.getType()).isEqualTo(ResourceType.AZURE_VOLUMESET);
        assertThat(resultVolumeSetResource.getStatus()).isEqualTo(CommonStatus.CREATED);
        assertThat(resultVolumeSetResource.getName()).isEqualTo(VOLUME_NAME);

        VolumeSetAttributes resultVolumeSetAttributes = resultVolumeSetResource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
        assertThat(resultVolumeSetAttributes).isNotNull();
        assertThat(resultVolumeSetAttributes.getAvailabilityZone()).isEqualTo(AVAILABILITY_ZONE);
        assertThat(resultVolumeSetAttributes.getDeleteOnTermination()).isEqualTo(true);
        assertThat(resultVolumeSetAttributes.getFstab()).isEqualTo(FSTAB);
        assertThat(resultVolumeSetAttributes.getVolumeSize()).isEqualTo(VOLUME_SIZE);
        assertThat(resultVolumeSetAttributes.getVolumeType()).isEqualTo(VOLUME_TYPE);

        List<VolumeSetAttributes.Volume> resultVolumes = resultVolumeSetAttributes.getVolumes();
        assertThat(resultVolumes).isNotNull();
        assertThat(resultVolumes).hasSize(1);

        VolumeSetAttributes.Volume resultVolume = resultVolumes.get(0);
        assertThat(resultVolume.getId()).isEqualTo(diskIdExpected);
        assertThat(resultVolume.getDevice()).isEqualTo(deviceExpected);
        assertThat(resultVolume.getSize()).isEqualTo(VOLUME_SIZE);
        assertThat(resultVolume.getType()).isEqualTo(VOLUME_TYPE);
        assertThat(resultVolume.getCloudVolumeUsageType()).isEqualTo(CloudVolumeUsageType.GENERAL);
    }

    private void initAsyncTaskExecutor() {
        when(intermediateBuilderExecutor.submit(any(Runnable.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return future;
        });
    }
}

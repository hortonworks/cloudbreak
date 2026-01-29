package com.sequenceiq.cloudbreak.cloud.azure.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.task.AsyncTaskExecutor;

import com.azure.resourcemanager.compute.models.Disk;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.azure.AzureDisk;
import com.sequenceiq.cloudbreak.cloud.azure.AzureDiskType;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureListResult;
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
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
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

    private static final String DISK_ID_ON_AZURE = "/subscriptions/subid/resourceGroups/rg/providers/Microsoft.Compute/disks/disk-1";

    private static final String VOLUME_NAME = "volume";

    private static final String AVAILABILITY_ZONE = "az";

    private static final String FSTAB = "fstab";

    private static final String DEVICE = "device";

    private static final String DEVICE_DEV_SDC = "/dev/disk/azure/scsi[1-9]/lun0";

    private static final String DISK_ENCRYPTION_SET_ID = "diskEncryptionSetId";

    private static final String INSTANCE_ID = "instance1";

    private static final String MICROSOFT_DISK_PREFIX = "/subscriptions/sub/resourceGroups/rg/providers/Microsoft.Compute/disks/";

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

    @Spy
    private AzureInstanceFinder azureInstanceFinder;

    @Mock
    private ResourceRetriever resourceRetriever;

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
        when(cloudInstance.getParameter(AzureInstanceTemplate.RESOURCE_DISK_ATTACHED, Boolean.class)).thenReturn(true);
        when(instanceTemplate.getVolumes()).thenReturn(List.of(volumeTemplate));

        when(auth.getCloudContext()).thenReturn(cloudContext);
        when(auth.getParameter(AzureClient.class)).thenReturn(azureClient);

        when(resourceNameService.volumeSet(any(), any(), eq(PRIVATE_ID), any())).thenReturn("someResourceName");

        Region region = Region.region(REGION);
        Location location = Location.location(region);
        when(cloudContext.getLocation()).thenReturn(location);

        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, cloudStack)).thenReturn(RESOURCE_GROUP);

        when(disk.id()).thenReturn(DISK_ID_ON_AZURE);
    }

    @Test
    public void testWhenComputeResourceIsNullThenNullReturns() {
        when(context.getComputeResources(anyLong())).thenReturn(null);

        List<CloudResource> result = underTest.create(context, cloudInstance, PRIVATE_ID, auth, group, image);

        assertNull(result);
    }

    @Test
    public void testWhenComputeResourceIsEmptyThenNullReturns() {
        when(context.getComputeResources(anyLong())).thenReturn(Collections.emptyList());

        List<CloudResource> result = underTest.create(context, cloudInstance, PRIVATE_ID, auth, group, image);

        assertNull(result);
    }

    @Test
    public void testWhenDetachedReattachableVolumeExistsThenItShouldReturn() {
        CloudResource volumeSetResource = CloudResource.builder().withType(ResourceType.AZURE_VOLUMESET).withStatus(CommonStatus.DETACHED)
                .withName(VOLUME_NAME).withParameters(Map.of()).build();
        CloudResource newInstance = CloudResource.builder().withInstanceId("instanceid").withType(ResourceType.AZURE_INSTANCE).withStatus(CommonStatus.CREATED)
                .withName("instance").withParameters(Map.of()).build();
        when(context.getComputeResources(PRIVATE_ID)).thenReturn(List.of(volumeSetResource, newInstance));

        List<CloudResource> result = underTest.create(context, cloudInstance, PRIVATE_ID, auth, group, image);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(volumeSetResource, result.get(0));
    }

    @Test
    public void testWhenReattachableVolumeWithInstanceIdExistsThenItShouldReturn() {
        CloudResource volumeSetResource = CloudResource.builder().withType(ResourceType.AZURE_VOLUMESET).withStatus(CommonStatus.CREATED)
                .withInstanceId("instanceid").withName(VOLUME_NAME).withParameters(Map.of()).build();
        CloudResource newInstance = CloudResource.builder().withInstanceId("instanceid").withType(ResourceType.AZURE_INSTANCE).withStatus(CommonStatus.CREATED)
                .withName("instance").withParameters(Map.of()).build();
        when(context.getComputeResources(PRIVATE_ID)).thenReturn(List.of(volumeSetResource, newInstance));

        List<CloudResource> result = underTest.create(context, cloudInstance, PRIVATE_ID, auth, group, image);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(volumeSetResource, result.get(0));
    }

    @Test
    public void testWhenReattachableDoesNotExistsThenNewlyBuildedInstanceShouldBeCreated() {
        CloudResource volumeSetResource = CloudResource.builder().withType(ResourceType.AZURE_VOLUMESET).withStatus(CommonStatus.CREATED)
                .withName(VOLUME_NAME).withParameters(Map.of()).build();
        CloudResource newInstance = CloudResource.builder().withInstanceId("instanceid").withType(ResourceType.AZURE_INSTANCE).withStatus(CommonStatus.CREATED)
                .withName("instance").withParameters(Map.of()).build();
        when(context.getComputeResources(PRIVATE_ID)).thenReturn(List.of(volumeSetResource, newInstance));
        CloudResource resourceGroupResource = mock(CloudResource.class);
        when(resourceGroupResource.getName()).thenReturn("resourcegroup");
        when(resourceRetriever.findAllByStatusAndTypeAndStack(eq(CommonStatus.CREATED), eq(ResourceType.AZURE_RESOURCE_GROUP), anyLong()))
                .thenReturn(List.of(resourceGroupResource));

        List<CloudResource> result = underTest.create(context, cloudInstance, PRIVATE_ID, auth, group, image);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotEquals(volumeSetResource, result.get(0));
    }

    @Test
    public void deleteTestWhenDiskIsDeletedOnAzure() throws PreserveResourceException {
        CloudResource mock = CloudResource.builder().withType(ResourceType.AZURE_RESOURCE_GROUP).withName("resource-group").build();
        when(context.getNetworkResources()).thenReturn(List.of(mock));
        ArrayList<VolumeSetAttributes.Volume> volumes = new ArrayList<>();
        volumes.add(new VolumeSetAttributes.Volume(VOLUME_ID, DEVICE, VOLUME_SIZE, "ssd", CloudVolumeUsageType.GENERAL));
        CloudResource volumeSetResource = CloudResource.builder().withType(ResourceType.AZURE_VOLUMESET).withStatus(CommonStatus.CREATED)
                .withParameters(Map.of(CloudResource.ATTRIBUTES, new VolumeSetAttributes(AVAILABILITY_ZONE, true, "", volumes, VOLUME_SIZE, "ssd")))
                .withName(VOLUME_NAME).build();
        AzureListResult<Disk> azureListResult = mock(AzureListResult.class);
        when(azureListResult.getAll()).thenReturn(List.of());
        when(azureClient.listDisksByResourceGroup(eq("resource-group"))).thenReturn(azureListResult);

        underTest.delete(context, auth, volumeSetResource);

        verify(azureUtils, times(0)).deleteManagedDisks(any(), any());
    }

    @Test
    public void deleteTestWhenDiskIsOnAzureAndNotAttached() throws PreserveResourceException {
        CloudResource mock = CloudResource.builder().withType(ResourceType.AZURE_RESOURCE_GROUP).withName("resource-group").build();
        when(context.getNetworkResources()).thenReturn(List.of(mock));
        ArrayList<VolumeSetAttributes.Volume> volumes = new ArrayList<>();
        volumes.add(new VolumeSetAttributes.Volume("vol1", DEVICE, VOLUME_SIZE, "ssd", CloudVolumeUsageType.GENERAL));
        CloudResource volumeSetResource = CloudResource.builder().withType(ResourceType.AZURE_VOLUMESET).withStatus(CommonStatus.CREATED)
                .withParameters(Map.of(
                        CloudResource.ATTRIBUTES, new VolumeSetAttributes(AVAILABILITY_ZONE, true, "", volumes, VOLUME_SIZE, "ssd")
                ))
                .withName(VOLUME_NAME).build();
        List<Disk> diskList = new ArrayList<>();
        Disk disk1 = mock(Disk.class);
        when(disk1.id()).thenReturn("vol1");
        Disk disk2 = mock(Disk.class);
        when(disk2.id()).thenReturn("vol2");
        Disk disk3 = mock(Disk.class);
        when(disk3.id()).thenReturn("vol3");
        diskList.add(disk1);
        diskList.add(disk2);
        diskList.add(disk3);
        Disk disk = mock(Disk.class);
        AzureListResult<Disk> azureListResult = mock(AzureListResult.class);
        when(disk.isAttachedToVirtualMachine()).thenReturn(false);
        when(azureClient.getDiskById(any())).thenReturn(disk);
        when(azureListResult.getAll()).thenReturn(diskList);
        when(azureClient.listDisksByResourceGroup(eq("resource-group"))).thenReturn(azureListResult);

        underTest.delete(context, auth, volumeSetResource);

        verify(azureUtils, times(1)).deleteManagedDisks(any(), collectionCaptor.capture());
        verify(azureClient, times(0)).getVirtualMachine(any());
        verify(azureClient, times(0)).detachDiskFromVm(any(), any());
        Collection<String> deletedAzureManagedDisks = collectionCaptor.getValue();
        assertThat(deletedAzureManagedDisks).contains("vol1");
    }

    @Test
    public void deleteTestWhenDiskIsOnAzureAndAttached() throws PreserveResourceException {
        CloudResource mock = CloudResource.builder().withType(ResourceType.AZURE_RESOURCE_GROUP).withName("resource-group").build();
        when(context.getNetworkResources()).thenReturn(List.of(mock));
        ArrayList<VolumeSetAttributes.Volume> volumes = new ArrayList<>();
        volumes.add(new VolumeSetAttributes.Volume("vol1", DEVICE, VOLUME_SIZE, "ssd", CloudVolumeUsageType.GENERAL));
        CloudResource volumeSetResource = CloudResource.builder().withType(ResourceType.AZURE_VOLUMESET).withStatus(CommonStatus.CREATED)
                .withParameters(Map.of(
                        CloudResource.ATTRIBUTES, new VolumeSetAttributes(AVAILABILITY_ZONE, true, "", volumes, VOLUME_SIZE, "ssd")
                ))
                .withInstanceId("instance1")
                .withName(VOLUME_NAME).build();
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
        diskList.add(disk1);
        diskList.add(disk2);
        diskList.add(disk3);
        Disk disk = mock(Disk.class);
        when(disk.isAttachedToVirtualMachine()).thenReturn(true);
        when(disk.virtualMachineId()).thenReturn("instance1");
        when(azureClient.getDiskById(any())).thenReturn(disk);
        AzureListResult<Disk> azureListResult = mock(AzureListResult.class);
        when(azureListResult.getAll()).thenReturn(diskList);
        when(azureClient.listDisksByResourceGroup(eq("resource-group"))).thenReturn(azureListResult);
        VirtualMachine virtualMachine = mock(VirtualMachine.class);
        when(azureClient.getVirtualMachine(eq("instance1"))).thenReturn(virtualMachine);
        when(virtualMachine.name()).thenReturn("instance1");
        underTest.delete(context, auth, volumeSetResource);

        verify(azureClient, times(1)).getVirtualMachine(eq("instance1"));
        verify(azureClient, times(1)).detachDisksFromVm(collectionCaptor.capture(), eq(virtualMachine));
        Collection<String> detachedDisks = collectionCaptor.getValue();
        assertThat(detachedDisks).containsOnly("vol1");
        verify(azureUtils, times(1)).deleteManagedDisks(any(), collectionCaptor.capture());
        Collection<String> deletedAzureManagedDisks = collectionCaptor.getValue();
        assertThat(deletedAzureManagedDisks).contains("vol1");
    }

    @Test
    void buildTestWhenNoVolumeSet() throws Exception {
        List<CloudResource> result = underTest.build(context, cloudInstance, PRIVATE_ID, auth, group, List.of(), cloudStack);

        verify(azureClient, never()).createManagedDisk(any());

        assertThat(result).isNotNull();
        assertThat(result).hasSize(0);
    }

    @Test
    void buildTestWhenVolumeSetExistsAndCreated() throws Exception {
        ArrayList<VolumeSetAttributes.Volume> volumes = new ArrayList<>();
        volumes.add(new VolumeSetAttributes.Volume(VOLUME_ID, DEVICE, VOLUME_SIZE, VOLUME_TYPE, CloudVolumeUsageType.GENERAL));
        CloudResource volumeSetResource = CloudResource.builder().withType(ResourceType.AZURE_VOLUMESET).withStatus(CommonStatus.CREATED)
                .withParameters(Map.of(CloudResource.ATTRIBUTES, new VolumeSetAttributes(AVAILABILITY_ZONE, true, FSTAB, volumes, VOLUME_SIZE, VOLUME_TYPE)))
                .withName(VOLUME_NAME).withAvailabilityZone(AVAILABILITY_ZONE).build();

        List<CloudResource> result = underTest.build(context, cloudInstance, PRIVATE_ID, auth, group, List.of(volumeSetResource), cloudStack);

        verify(azureClient, never()).createManagedDisk(any());

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);

        verifyVolumeSetResource(result.get(0), VOLUME_ID, DEVICE);
    }

    @Test
    void buildTestWhenVolumeSetExistsAndRequestedAndExistingDisk() throws Exception {
        initAsyncTaskExecutor();

        ArrayList<VolumeSetAttributes.Volume> volumes = new ArrayList<>();
        volumes.add(new VolumeSetAttributes.Volume(DISK_ID_ON_AZURE, DEVICE, VOLUME_SIZE, VOLUME_TYPE, CloudVolumeUsageType.GENERAL));
        CloudResource volumeSetResource = CloudResource.builder().withType(ResourceType.AZURE_VOLUMESET).withStatus(CommonStatus.REQUESTED)
                .withParameters(Map.of(CloudResource.ATTRIBUTES, new VolumeSetAttributes(AVAILABILITY_ZONE, true, FSTAB, volumes, VOLUME_SIZE, VOLUME_TYPE)))
                .withName(VOLUME_NAME).withAvailabilityZone(AVAILABILITY_ZONE).build();

        when(azureClient.getDiskByName(RESOURCE_GROUP, DISK_ID)).thenReturn(disk);

        List<CloudResource> result = underTest.build(context, cloudInstance, PRIVATE_ID, auth, group, List.of(volumeSetResource), cloudStack);

        verify(azureClient, never()).createManagedDisk(any());

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);

        verifyVolumeSetResource(result.get(0), DISK_ID_ON_AZURE, DEVICE);
    }

    @Test
    void buildTestWhenVolumeSetExistsAndRequestedAndNewDiskAndNoDiskEncryptionSetId() throws Exception {
        initAsyncTaskExecutor();

        ArrayList<VolumeSetAttributes.Volume> volumes = new ArrayList<>();
        volumes.add(new VolumeSetAttributes.Volume(VOLUME_ID, null, VOLUME_SIZE, VOLUME_TYPE, CloudVolumeUsageType.GENERAL));
        CloudResource volumeSetResource = CloudResource.builder().withType(ResourceType.AZURE_VOLUMESET).withStatus(CommonStatus.REQUESTED)
                .withParameters(Map.of(CloudResource.ATTRIBUTES, new VolumeSetAttributes(AVAILABILITY_ZONE, true, FSTAB, volumes, VOLUME_SIZE, VOLUME_TYPE)))
                .withName(VOLUME_NAME).withAvailabilityZone(AVAILABILITY_ZONE).build();

        when(azureClient.createManagedDisk(new AzureDisk(VOLUME_ID, VOLUME_SIZE, AzureDiskType.STANDARD_SSD_LRS, REGION, RESOURCE_GROUP,
                Map.of(), null, null))).thenReturn(disk);

        List<CloudResource> result = underTest.build(context, cloudInstance, PRIVATE_ID, auth, group, List.of(volumeSetResource), cloudStack);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);

        verifyVolumeSetResource(result.get(0), DISK_ID_ON_AZURE, DEVICE_DEV_SDC);
    }

    @Test
    void buildTestWhenVolumeSetExistsAndRequestedAndOffSetAndAdditionalTagsGiven() throws Exception {
        initAsyncTaskExecutor();

        ArrayList<VolumeSetAttributes.Volume> volumes = new ArrayList<>();
        volumes.add(new VolumeSetAttributes.Volume(VOLUME_ID, null, VOLUME_SIZE, VOLUME_TYPE, CloudVolumeUsageType.GENERAL));
        CloudResource volumeSetResource = CloudResource.builder().withType(ResourceType.AZURE_VOLUMESET).withStatus(CommonStatus.REQUESTED)
                .withParameters(Map.of(CloudResource.ATTRIBUTES, new VolumeSetAttributes(AVAILABILITY_ZONE, true, FSTAB, volumes, VOLUME_SIZE, VOLUME_TYPE)))
                .withName(VOLUME_NAME).withAvailabilityZone(AVAILABILITY_ZONE).build();

        when(azureClient.createManagedDisk(new AzureDisk(VOLUME_ID, VOLUME_SIZE, AzureDiskType.STANDARD_SSD_LRS, REGION, RESOURCE_GROUP,
                Map.of(), null, null))).thenReturn(disk);
        when(cloudStack.getTags()).thenReturn(Map.of("existingTag", "existingTagValue"));

        List<CloudResource> result = underTest.build(auth, group, List.of(volumeSetResource), cloudStack, 5, Map.of("newTag", "newTagValue"));

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);

        ArgumentCaptor<AzureDisk> captor = ArgumentCaptor.forClass(AzureDisk.class);
        verify(azureClient).createManagedDisk(captor.capture());
        AzureDisk azureDisk = captor.getValue();
        assertEquals(2, azureDisk.getTags().size());
        assertTrue("existingTagValue".equals(azureDisk.getTags().get("existingTag")));
        assertTrue("newTagValue".equals(azureDisk.getTags().get("newTag")));


        verifyVolumeSetResource(result.get(0), DISK_ID_ON_AZURE, "/dev/disk/azure/scsi[1-9]/lun5");
    }

    @Test
    void buildTestWhenVolumeSetExistsAndRequestedAndNewDiskAndDiskEncryptionSetIdGivenButIneffective() throws Exception {
        initAsyncTaskExecutor();

        ArrayList<VolumeSetAttributes.Volume> volumes = new ArrayList<>();
        volumes.add(new VolumeSetAttributes.Volume(VOLUME_ID, null, VOLUME_SIZE, VOLUME_TYPE, CloudVolumeUsageType.GENERAL));
        CloudResource volumeSetResource = CloudResource.builder().withType(ResourceType.AZURE_VOLUMESET).withStatus(CommonStatus.REQUESTED)
                .withParameters(Map.of(CloudResource.ATTRIBUTES, new VolumeSetAttributes(AVAILABILITY_ZONE, true, FSTAB, volumes, VOLUME_SIZE, VOLUME_TYPE)))
                .withName(VOLUME_NAME).withAvailabilityZone(AVAILABILITY_ZONE).build();

        when(instanceTemplate.getStringParameter(AzureInstanceTemplate.DISK_ENCRYPTION_SET_ID)).thenReturn(DISK_ENCRYPTION_SET_ID);

        when(azureClient.createManagedDisk(new AzureDisk(VOLUME_ID, VOLUME_SIZE, AzureDiskType.STANDARD_SSD_LRS, REGION, RESOURCE_GROUP,
                        Map.of(), null, null))).thenReturn(disk);

        List<CloudResource> result = underTest.build(context, cloudInstance, PRIVATE_ID, auth, group, List.of(volumeSetResource), cloudStack);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);

        verifyVolumeSetResource(result.get(0), DISK_ID_ON_AZURE, DEVICE_DEV_SDC);
    }

    @Test
    void buildTestWhenVolumeSetExistsAndRequestedAndNewDiskAndDiskEncryptionSetIdGivenAndEffective() throws Exception {
        initAsyncTaskExecutor();

        ArrayList<VolumeSetAttributes.Volume> volumes = new ArrayList<>();
        volumes.add(new VolumeSetAttributes.Volume(VOLUME_ID, DEVICE, VOLUME_SIZE, VOLUME_TYPE, CloudVolumeUsageType.GENERAL));
        CloudResource volumeSetResource = CloudResource.builder().withType(ResourceType.AZURE_VOLUMESET).withStatus(CommonStatus.REQUESTED)
                .withParameters(Map.of(CloudResource.ATTRIBUTES, new VolumeSetAttributes(AVAILABILITY_ZONE, true, FSTAB, volumes, VOLUME_SIZE, VOLUME_TYPE)))
                .withName(VOLUME_NAME).withAvailabilityZone(AVAILABILITY_ZONE).build();

        when(instanceTemplate.getParameter(AzureInstanceTemplate.MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED, Object.class)).thenReturn(true);
        when(instanceTemplate.getStringParameter(AzureInstanceTemplate.DISK_ENCRYPTION_SET_ID)).thenReturn(DISK_ENCRYPTION_SET_ID);

        when(azureClient.createManagedDisk(new AzureDisk(VOLUME_ID, VOLUME_SIZE, AzureDiskType.STANDARD_SSD_LRS, REGION, RESOURCE_GROUP, Map.of(),
                DISK_ENCRYPTION_SET_ID, null))).thenReturn(disk);

        List<CloudResource> result = underTest.build(context, cloudInstance, PRIVATE_ID, auth, group, List.of(volumeSetResource), cloudStack);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);

        verifyVolumeSetResource(result.get(0), DISK_ID_ON_AZURE, DEVICE);
    }

    private void verifyVolumeSetResource(CloudResource resultVolumeSetResource, String diskIdExpected, String deviceExpected) {
        assertThat(resultVolumeSetResource.getType()).isEqualTo(ResourceType.AZURE_VOLUMESET);
        assertThat(resultVolumeSetResource.getStatus()).isEqualTo(CommonStatus.CREATED);
        assertThat(resultVolumeSetResource.getName()).isEqualTo(VOLUME_NAME);
        assertThat(resultVolumeSetResource.getAvailabilityZone()).isEqualTo(AVAILABILITY_ZONE);


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

    @Test
    void testModifyVolumes() {
        when(auth.getParameter(AzureClient.class)).thenReturn(azureClient);
        List<String> volumeIds = List.of("/subscriptions/test-subscription/resourceGroups/test-res-group/providers/Microsoft.Compute/disks/test-vol");
        underTest.modifyVolumes(auth, volumeIds, "test-disk", 100);
        verify(azureClient).modifyDisk("test-vol", "test-res-group", 100, "test-disk");
    }

    @Test
    void testModifyVolumesEmptyVolumeIds() {
        when(auth.getParameter(AzureClient.class)).thenReturn(azureClient);
        List<String> volumeIds = List.of();
        underTest.modifyVolumes(auth, volumeIds, "test-disk", 100);
        verify(azureClient, times(0)).modifyDisk(any(), any(), eq(100), eq("test-disk"));
    }

    static Object[][] detachVolumes() {
        return new Object[][]{
                {
                        List.of()
                },
                {
                        List.of(List.of())
                },
                {
                        List.of(List.of(), List.of())
                },
                {
                        List.of(List.of(), List.of(true))
                },
                {
                        List.of(List.of(false), List.of())
                },
                {
                        List.of(List.of(true))
                },
                {
                        List.of(List.of(false))
                },
                {
                        List.of(List.of(true, true, true))
                },
                {
                        List.of(List.of(false, false, false))
                },
                {
                        List.of(List.of(true, true, true), List.of(true, true, true))
                },
                {
                        List.of(List.of(false, false, false), List.of(false, false, false))
                },
                {
                        List.of(List.of(true, false, true), List.of(false, true, false))
                }
        };
    }

    @ParameterizedTest()
    @MethodSource("detachVolumes")
    void testDetachVolumes(List<List<Boolean>> volumeSets) {
        Map<String, List<String>> attachedVolumes = new HashMap<>();
        when(auth.getParameter(AzureClient.class)).thenReturn(azureClient);
        List<CloudResource> volumeSetResources = new ArrayList<>();
        for (int volumeSetCounter = 0; volumeSetCounter < volumeSets.size(); volumeSetCounter++) {
            String volumeSetName = String.format("%s%d", "VolumeSet", volumeSetCounter);
            ArrayList<VolumeSetAttributes.Volume> volumes = new ArrayList<>();
            for (int volumeCounter = 0; volumeCounter < volumeSets.get(volumeSetCounter).size(); volumeCounter++) {
                String volumeId = String.format("%s%s%d", volumeSetName, "Volume", volumeCounter);
                Disk disk = mock(Disk.class);
                boolean attached = volumeSets.get(volumeSetCounter).get(volumeCounter);
                when(disk.isAttachedToVirtualMachine()).thenReturn(attached);
                when(disk.virtualMachineId()).thenReturn(INSTANCE_ID);
                when(azureClient.getDiskById(volumeId)).thenReturn(disk);
                volumes.add(new VolumeSetAttributes.Volume(volumeId, DEVICE, VOLUME_SIZE, "ssd", CloudVolumeUsageType.GENERAL));
                if (attached) {
                    attachedVolumes.putIfAbsent(volumeSetName, new ArrayList<>());
                    attachedVolumes.get(volumeSetName).add(volumeId);
                }
            }
            CloudResource volumeSetResource = CloudResource.builder().withType(ResourceType.AZURE_VOLUMESET).withStatus(CommonStatus.CREATED)
                    .withParameters(Map.of(
                            CloudResource.ATTRIBUTES, new VolumeSetAttributes(AVAILABILITY_ZONE, true, "", volumes, VOLUME_SIZE, "ssd")
                    ))
                    .withInstanceId(INSTANCE_ID)
                    .withName(volumeSetName).build();
            volumeSetResources.add(volumeSetResource);
        }

        VirtualMachine virtualMachine = mock(VirtualMachine.class);
        when(azureClient.getVirtualMachine(eq(INSTANCE_ID))).thenReturn(virtualMachine);
        when(virtualMachine.name()).thenReturn(INSTANCE_ID);

        underTest.detachVolumes(auth, volumeSetResources);

        verify(azureClient, times(attachedVolumes.size())).getVirtualMachine(eq(INSTANCE_ID));
        verify(azureClient, times(attachedVolumes.size())).detachDisksFromVm(collectionCaptor.capture(), eq(virtualMachine));
        List<Collection<String>> allDetachedVolumes = collectionCaptor.getAllValues();
        attachedVolumes.values().stream().forEach(attachedVols -> assertEquals(true,
                allDetachedVolumes.stream().anyMatch(detachedVolumes -> CollectionUtils.isEqualCollection(detachedVolumes, attachedVols))));
    }

    static Object[][] deleteVolumes() {
        return new Object[][]{
                {
                        List.of()
                },
                {
                        List.of(0)
                },
                {
                        List.of(0, 0)
                },
                {
                        List.of(0, 2)
                },
                {
                        List.of(1)
                },
                {
                        List.of(1, 2)
                },
                {
                        List.of(3, 4)
                }
        };
    }

    @ParameterizedTest()
    @MethodSource("deleteVolumes")
    void testDeleteVolumes(List<Integer> volumeSets) {
        Map<String, List<String>> deletedVolumes = new HashMap<>();
        when(auth.getParameter(AzureClient.class)).thenReturn(azureClient);
        List<CloudResource> volumeSetResources = new ArrayList<>();
        for (int volumeSetCounter = 0; volumeSetCounter < volumeSets.size(); volumeSetCounter++) {
            String volumeSetName = String.format("%s%d", "VolumeSet", volumeSetCounter);
            ArrayList<VolumeSetAttributes.Volume> volumes = new ArrayList<>();
            deletedVolumes.putIfAbsent(volumeSetName, new ArrayList<>());
            for (int volumeCounter = 0; volumeCounter < volumeSets.get(volumeSetCounter); volumeCounter++) {
                String volumeId = String.format("%s%s%d", volumeSetName, "Volume", volumeCounter);
                volumes.add(new VolumeSetAttributes.Volume(volumeId, DEVICE, VOLUME_SIZE, "ssd", CloudVolumeUsageType.GENERAL));
                deletedVolumes.get(volumeSetName).add(volumeId);
            }
            CloudResource volumeSetResource = CloudResource.builder().withType(ResourceType.AZURE_VOLUMESET).withStatus(CommonStatus.CREATED)
                    .withParameters(Map.of(
                            CloudResource.ATTRIBUTES, new VolumeSetAttributes(AVAILABILITY_ZONE, true, "", volumes, VOLUME_SIZE, "ssd")
                    ))
                    .withInstanceId(INSTANCE_ID)
                    .withName(volumeSetName).build();
            volumeSetResources.add(volumeSetResource);
        }

        underTest.deleteVolumes(auth, volumeSetResources);

        verify(azureUtils, times(deletedVolumes.size())).deleteManagedDisks(eq(azureClient), collectionCaptor.capture());
        List<Collection<String>> allDeletedVolumes = collectionCaptor.getAllValues();
        deletedVolumes.values().stream().forEach(deletedVols -> assertEquals(true,
                allDeletedVolumes.stream().anyMatch(deletedVolumesAtAzure -> CollectionUtils.isEqualCollection(deletedVols, deletedVolumesAtAzure))));
    }

    static Object[][] volumeOrderingTestData() {
        return new Object[][]{
                {List.of("disk-1", "disk-2", "disk-3"), List.of(100, 200, 300), 3},
                {List.of("disk-a", "disk-b", "disk-c", "disk-d"), List.of(50, 100, 150, 200), 4},
                {List.of("vol-1", "vol-2"), List.of(500, 1000), 2}
        };
    }

    @ParameterizedTest
    @MethodSource("volumeOrderingTestData")
    void testConsistentVolumeOrderingWithMultipleRunsAndVariousConfigurations(List<String> diskIds, List<Integer> sizes, int volumeCount) throws Exception {
        initAsyncTaskExecutor();

        List<VolumeSetAttributes.Volume> volumes = new ArrayList<>();
        List<Disk> mockDisks = new ArrayList<>();

        for (int i = 0; i < volumeCount; i++) {
            volumes.add(new VolumeSetAttributes.Volume(diskIds.get(i), null, sizes.get(i), "StandardSSD_LRS", CloudVolumeUsageType.GENERAL));
            Disk disk = mock(Disk.class);
            when(disk.id()).thenReturn(String.format(MICROSOFT_DISK_PREFIX + "%s", diskIds.get(i)));
            mockDisks.add(disk);
            when(azureClient.getDiskByName(eq(RESOURCE_GROUP), eq(diskIds.get(i)))).thenReturn(null);
        }

        when(azureClient.createManagedDisk(any(AzureDisk.class))).thenAnswer(invocation -> {
            AzureDisk azureDisk = invocation.getArgument(0);
            String diskId = azureDisk.getDiskName();
            int index = diskIds.indexOf(diskId);
            return mockDisks.get(Math.max(index, 0));
        });

        List<List<String>> deviceNamesFromMultipleRuns = new ArrayList<>();
        List<List<String>> volumeIdsFromMultipleRuns = new ArrayList<>();

        for (int run = 0; run < 100; run++) {
            CloudResource volumeSetResource = CloudResource.builder()
                    .withType(ResourceType.AZURE_VOLUMESET)
                    .withStatus(CommonStatus.REQUESTED)
                    .withName("volumeset-1")
                    .withAvailabilityZone(AVAILABILITY_ZONE)
                    .withParameters(Map.of(CloudResource.ATTRIBUTES,
                            new VolumeSetAttributes(AVAILABILITY_ZONE, true, FSTAB, volumes, 100, "StandardSSD_LRS")))
                    .build();

            List<CloudResource> result = underTest.build(auth, group, List.of(volumeSetResource), cloudStack, null, null);

            assertThat(result).hasSize(1);
            VolumeSetAttributes resultAttributes = result.getFirst().getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
            List<VolumeSetAttributes.Volume> resultVolumes = resultAttributes.getVolumes();

            assertThat(resultVolumes).hasSize(volumeCount);

            List<String> deviceNames = resultVolumes.stream()
                    .map(VolumeSetAttributes.Volume::getDevice)
                    .collect(java.util.stream.Collectors.toList());
            List<String> volumeIds = resultVolumes.stream()
                    .map(v -> v.getId().substring(v.getId().lastIndexOf('/') + 1))
                    .collect(java.util.stream.Collectors.toList());

            deviceNamesFromMultipleRuns.add(deviceNames);
            volumeIdsFromMultipleRuns.add(volumeIds);

            assertThat(deviceNames).doesNotContainNull().doesNotHaveDuplicates();
            assertThat(volumeIds).isEqualTo(diskIds);

            for (int i = 0; i < volumeCount; i++) {
                assertThat(resultVolumes.get(i).getSize()).isEqualTo(sizes.get(i));
            }
        }

        List<String> firstRunDeviceNames = deviceNamesFromMultipleRuns.getFirst();
        for (int i = 1; i < deviceNamesFromMultipleRuns.size(); i++) {
            assertThat(deviceNamesFromMultipleRuns.get(i)).as("Run %d device names should match run 0", i).isEqualTo(firstRunDeviceNames);
            assertThat(volumeIdsFromMultipleRuns.get(i)).as("Run %d volume IDs should match original order", i).isEqualTo(diskIds);
        }
    }

    @Test
    void testConsistentOrderingWithPreassignedDeviceNames() throws Exception {
        initAsyncTaskExecutor();

        List<VolumeSetAttributes.Volume> volumes = new ArrayList<>();
        volumes.add(new VolumeSetAttributes.Volume("disk-1", "/dev/sdc", 100, "StandardSSD_LRS", CloudVolumeUsageType.GENERAL));
        volumes.add(new VolumeSetAttributes.Volume("disk-2", null, 200, "StandardSSD_LRS", CloudVolumeUsageType.GENERAL));
        volumes.add(new VolumeSetAttributes.Volume("disk-3", "/dev/sdf", 300, "StandardSSD_LRS", CloudVolumeUsageType.GENERAL));

        CloudResource volumeSetResource = CloudResource.builder()
                .withType(ResourceType.AZURE_VOLUMESET)
                .withStatus(CommonStatus.REQUESTED)
                .withName("volumeset-1")
                .withAvailabilityZone(AVAILABILITY_ZONE)
                .withParameters(Map.of(CloudResource.ATTRIBUTES,
                        new VolumeSetAttributes(AVAILABILITY_ZONE, true, FSTAB, volumes, 100, "StandardSSD_LRS")))
                .build();

        Disk disk1 = mock(Disk.class);
        Disk disk2 = mock(Disk.class);
        Disk disk3 = mock(Disk.class);
        when(disk1.id()).thenReturn(MICROSOFT_DISK_PREFIX + "disk-1");
        when(disk2.id()).thenReturn(MICROSOFT_DISK_PREFIX + "disk-2");
        when(disk3.id()).thenReturn(MICROSOFT_DISK_PREFIX + "disk-3");

        when(azureClient.getDiskByName(eq(RESOURCE_GROUP), eq("disk-1"))).thenReturn(null);
        when(azureClient.getDiskByName(eq(RESOURCE_GROUP), eq("disk-2"))).thenReturn(null);
        when(azureClient.getDiskByName(eq(RESOURCE_GROUP), eq("disk-3"))).thenReturn(null);

        when(azureClient.createManagedDisk(any(AzureDisk.class)))
                .thenReturn(disk1)
                .thenReturn(disk2)
                .thenReturn(disk3);

        List<CloudResource> result = underTest.build(auth, group, List.of(volumeSetResource), cloudStack, null, null);

        assertThat(result).hasSize(1);
        VolumeSetAttributes resultAttributes = result.getFirst().getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
        List<VolumeSetAttributes.Volume> resultVolumes = resultAttributes.getVolumes();

        assertThat(resultVolumes).hasSize(3);
        assertThat(resultVolumes.get(0).getDevice()).as("First volume should keep preassigned device name").isEqualTo("/dev/sdc");
        assertThat(resultVolumes.get(1).getDevice()).as("Second volume should have generated device name").isNotNull();
        assertThat(resultVolumes.get(2).getDevice()).as("Third volume should keep preassigned device name").isEqualTo("/dev/sdf");
        assertThat(resultVolumes.get(0).getId()).contains("disk-1");
        assertThat(resultVolumes.get(1).getId()).contains("disk-2");
        assertThat(resultVolumes.get(2).getId()).contains("disk-3");
    }
}

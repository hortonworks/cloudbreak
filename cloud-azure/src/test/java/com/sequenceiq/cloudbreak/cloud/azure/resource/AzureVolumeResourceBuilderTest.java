package com.sequenceiq.cloudbreak.cloud.azure.resource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.task.AsyncTaskExecutor;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.compute.Disk;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.context.AzureContext;
import com.sequenceiq.cloudbreak.cloud.azure.service.AzureResourceNameService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@RunWith(MockitoJUnitRunner.class)
public class AzureVolumeResourceBuilderTest {
    private static final long PRIVATE_ID = 1L;

    private static final String STACK_CRN = "crn";

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

    @Before
    public void setUp() {
        CloudResource cloudResource1 = mock(CloudResource.class);
        CloudResource cloudResource2 = mock(CloudResource.class);
        when(context.getComputeResources(PRIVATE_ID)).thenReturn(List.of(cloudResource1, cloudResource2));
        when(context.getStringParameter(PlatformParametersConsts.RESOURCE_CRN_PARAMETER)).thenReturn(STACK_CRN);
        when(group.getReferenceInstanceTemplate()).thenReturn(instanceTemplate);
        when(instanceTemplate.getVolumes()).thenReturn(List.of(volumeTemplate));
        when(auth.getCloudContext()).thenReturn(cloudContext);
        when(auth.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(resourceNameService.resourceName(eq(ResourceType.AZURE_VOLUMESET), any(), any(), eq(PRIVATE_ID), eq(STACK_CRN))).thenReturn("someResourceName");
    }

    @Test
    public void testWhenComputeResourceIsNullThenNullReturns() {
        when(context.getComputeResources(anyLong())).thenReturn(null);

        List<CloudResource> result = underTest.create(context, PRIVATE_ID, auth, group, image);

        Assert.assertNull(result);
    }

    @Test
    public void testWhenComputeResourceIsEmptyThenNullReturns() {
        when(context.getComputeResources(anyLong())).thenReturn(Collections.emptyList());

        List<CloudResource> result = underTest.create(context, PRIVATE_ID, auth, group, image);

        Assert.assertNull(result);
    }

    @Test
    public void testWhenDetachedReattachableVolumeExistsThenItShouldReturn() {
        CloudResource volumeSetResource = CloudResource.builder().type(ResourceType.AZURE_VOLUMESET).status(CommonStatus.DETACHED)
                .name("volume").params(Map.of()).build();
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
                .name("volume").params(Map.of()).build();
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
                .name("volume").params(Map.of()).build();
        CloudResource newInstance = CloudResource.builder().instanceId("instanceid").type(ResourceType.AZURE_INSTANCE).status(CommonStatus.CREATED)
                .name("instance").params(Map.of()).build();
        when(context.getComputeResources(PRIVATE_ID)).thenReturn(List.of(volumeSetResource, newInstance));

        List<CloudResource> result = underTest.create(context, PRIVATE_ID, auth, group, image);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotEquals(volumeSetResource, result.get(0));
    }

    @Test
    public void deleteTestWhenDiskIsDeletedOnAzure() throws InterruptedException {
        CloudResource mock = CloudResource.builder().type(ResourceType.AZURE_RESOURCE_GROUP).name("resource-group").build();
        when(context.getNetworkResources()).thenReturn(List.of(mock));
        ArrayList<VolumeSetAttributes.Volume> volumes = new ArrayList<>();
        volumes.add(new VolumeSetAttributes.Volume("volume-1", "device", 100, "ssd"));
        CloudResource volumeSetResource = CloudResource.builder().type(ResourceType.AZURE_VOLUMESET).status(CommonStatus.CREATED)
                .params(Map.of(CloudResource.ATTRIBUTES, new VolumeSetAttributes("az", true, "", volumes, 100, "ssd")))
                .name("volume").build();
        PagedList<Disk> pagedList = mock(PagedList.class);
        when(azureClient.listDisksByResourceGroup(eq("resource-group"))).thenReturn(pagedList);
        underTest.delete(context, auth, volumeSetResource);
        verify(azureUtils, times(0)).deleteManagedDisks(any(), any());
    }

    @Test
    public void deleteTestWhenDiskIsOnAzureAndNotAttached() throws InterruptedException {
        CloudResource mock = CloudResource.builder().type(ResourceType.AZURE_RESOURCE_GROUP).name("resource-group").build();
        when(context.getNetworkResources()).thenReturn(List.of(mock));
        ArrayList<VolumeSetAttributes.Volume> volumes = new ArrayList<>();
        volumes.add(new VolumeSetAttributes.Volume("vol1", "device", 100, "ssd"));
        CloudResource volumeSetResource = CloudResource.builder().type(ResourceType.AZURE_VOLUMESET).status(CommonStatus.CREATED)
                .params(Map.of(
                        CloudResource.ATTRIBUTES, new VolumeSetAttributes("az", true, "", volumes, 100, "ssd")
                ))
                .name("volume").build();
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
        when(pagedList.stream()).thenAnswer(invocation -> diskList.stream());
        when(azureClient.listDisksByResourceGroup(eq("resource-group"))).thenReturn(pagedList);
        ArgumentCaptor<Collection<String>> captor = ArgumentCaptor.forClass(Collection.class);
        underTest.delete(context, auth, volumeSetResource);
        verify(azureUtils, times(1)).deleteManagedDisks(any(), captor.capture());
        verify(azureClient, times(0)).getVirtualMachine(any(), any());
        verify(azureClient, times(0)).detachDiskFromVm(any(), any());
        Collection<String> deletedAzureManagedDisks = captor.getValue();
        assertThat(deletedAzureManagedDisks, containsInAnyOrder("vol1"));
    }

    @Test
    public void deleteTestWhenDiskIsOnAzureAndAttached() throws InterruptedException {
        CloudResource mock = CloudResource.builder().type(ResourceType.AZURE_RESOURCE_GROUP).name("resource-group").build();
        when(context.getNetworkResources()).thenReturn(List.of(mock));
        ArrayList<VolumeSetAttributes.Volume> volumes = new ArrayList<>();
        volumes.add(new VolumeSetAttributes.Volume("vol1", "device", 100, "ssd"));
        CloudResource volumeSetResource = CloudResource.builder().type(ResourceType.AZURE_VOLUMESET).status(CommonStatus.CREATED)
                .params(Map.of(
                        CloudResource.ATTRIBUTES, new VolumeSetAttributes("az", true, "", volumes, 100, "ssd")
                ))
                .instanceId("instance1")
                .name("volume").build();
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
        when(pagedList.stream()).thenAnswer(invocation -> diskList.stream());
        when(azureClient.listDisksByResourceGroup(eq("resource-group"))).thenReturn(pagedList);
        VirtualMachine virtualMachine = mock(VirtualMachine.class);
        when(azureClient.getVirtualMachine(any(), eq("instance1"))).thenReturn(virtualMachine);
        ArgumentCaptor<Collection<String>> captor = ArgumentCaptor.forClass(Collection.class);
        underTest.delete(context, auth, volumeSetResource);

        verify(azureUtils, times(1)).deleteManagedDisks(any(), captor.capture());
        verify(azureClient, times(1)).getVirtualMachine(eq("resource-group"), eq("instance1"));
        verify(azureClient, times(1)).detachDiskFromVm(eq("vol1"), eq(virtualMachine));
        Collection<String> deletedAzureManagedDisks = captor.getValue();
        assertThat(deletedAzureManagedDisks, containsInAnyOrder("vol1"));
    }
}

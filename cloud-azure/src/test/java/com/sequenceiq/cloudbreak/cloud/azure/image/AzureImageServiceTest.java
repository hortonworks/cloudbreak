package com.sequenceiq.cloudbreak.cloud.azure.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.compute.VirtualMachineCustomImage;
import com.microsoft.azure.management.resources.Subscription;
import com.sequenceiq.cloudbreak.cloud.azure.AzureImage;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.resource.AzureResourceIdProviderService;
import com.sequenceiq.cloudbreak.cloud.azure.task.image.AzureManagedImageCreationCheckerContext;
import com.sequenceiq.cloudbreak.cloud.azure.task.image.AzureManagedImageCreationPoller;
import com.sequenceiq.cloudbreak.cloud.azure.util.CustomVMImageNameProvider;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceRetriever;
import com.sequenceiq.common.api.type.CommonStatus;

@RunWith(MockitoJUnitRunner.class)
public class AzureImageServiceTest {

    private static final String RESOURCE_GROUP_NAME = "resourceGroupName";

    private static final String FROM_VHD_URI = "fromVhdUri";

    private static final String CUSTOM_IMAGE_NAME = "customImageName";

    private static final String CUSTOM_IMAGE_ID = "customImageId";

    private static final String REGION_NAME = "regionName";

    private static final String SUBSCRIPTION_ID = "subscriptionId";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private PersistenceRetriever resourcePersistenceRetriever;

    @Mock
    private PersistenceNotifier persistenceNotifier;

    @Mock
    private AzureResourceIdProviderService azureResourceIdProviderService;

    @Mock
    private AzureManagedImageCreationPoller azureManagedImageCreationPoller;

    @Mock
    private AzureManagedImageService azureManagedImageService;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private AzureClient azureClient;

    @Mock
    private VirtualMachineCustomImage virtualMachineCustomImage;

    @Mock
    private CustomVMImageNameProvider customVMImageNameProvider;

    @InjectMocks
    private AzureImageService underTest;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        setupAuthenticatedContext();
        setupAzureClient();
        when(customVMImageNameProvider.get(anyString(), anyString())).thenReturn(CUSTOM_IMAGE_NAME);
        when(azureResourceIdProviderService.generateImageId(anyString(), anyString(), anyString())).thenReturn(CUSTOM_IMAGE_ID);
    }

    @Test
    public void testGetCustomImageIdWhenImageExistsOnAzure() {
        when(azureManagedImageService.findVirtualMachineCustomImage(anyString(), anyString(), any())).thenReturn(Optional.of(virtualMachineCustomImage));

        AzureImage azureImage = underTest.getCustomImageId(RESOURCE_GROUP_NAME, FROM_VHD_URI, authenticatedContext, false, azureClient);

        assertEquals(CUSTOM_IMAGE_ID, azureImage.getId());
        assertEquals(CUSTOM_IMAGE_NAME, azureImage.getName());
        verifyPollingStarted();
    }

    @Test
    public void testGetCustomImageIdWhenImageIsRequested() {
        when(azureManagedImageService.findVirtualMachineCustomImage(anyString(), anyString(), any())).thenReturn(Optional.empty());
        when(resourcePersistenceRetriever.notifyRetrieve(anyString(), any(), any())).thenReturn(Optional.of(mock(CloudResource.class)));

        AzureImage azureImage = underTest.getCustomImageId(RESOURCE_GROUP_NAME, FROM_VHD_URI, authenticatedContext, false, azureClient);

        assertEquals(CUSTOM_IMAGE_ID, azureImage.getId());
        assertEquals(CUSTOM_IMAGE_NAME, azureImage.getName());
        verifyPollingStarted();
    }

    @Test
    public void testGetCustomImageIdWhenImageWasNotRequestedAndDoNotCreateIfNotFound() {
        when(azureManagedImageService.findVirtualMachineCustomImage(anyString(), anyString(), any())).thenReturn(Optional.empty());

        AzureImage azureImage = underTest.getCustomImageId(RESOURCE_GROUP_NAME, FROM_VHD_URI, authenticatedContext, false, azureClient);

        assertNull(azureImage);
        verify(azureManagedImageCreationPoller, never()).startPolling(any(), any());
        verify(azureClient, never()).createCustomImage(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void testGetCustomImageIdWhenImageWasNotRequestedAndCreateIfNotFoundAndSuccess() {
        when(azureManagedImageService.findVirtualMachineCustomImage(anyString(), anyString(), any())).thenReturn(Optional.empty());
        when(azureClient.createCustomImage(anyString(), anyString(), anyString(), anyString())).thenReturn(virtualMachineCustomImage);
        when(virtualMachineCustomImage.name()).thenReturn(CUSTOM_IMAGE_NAME);
        when(virtualMachineCustomImage.id()).thenReturn(CUSTOM_IMAGE_ID);

        AzureImage azureImage = underTest.getCustomImageId(RESOURCE_GROUP_NAME, FROM_VHD_URI, authenticatedContext, true, azureClient);

        assertEquals(CUSTOM_IMAGE_ID, azureImage.getId());
        assertEquals(CUSTOM_IMAGE_NAME, azureImage.getName());
        verifyPersistenceNotification(cr -> verify(persistenceNotifier).notifyAllocation(cr.capture(), any()), CommonStatus.REQUESTED);
        verifyPersistenceNotification(cr -> verify(persistenceNotifier).notifyUpdate(cr.capture(), any()), CommonStatus.CREATED);
        verify(azureClient).createCustomImage(CUSTOM_IMAGE_NAME, RESOURCE_GROUP_NAME, FROM_VHD_URI, REGION_NAME);
    }

    @Test
    public void testGetCustomImageIdWhenImageWasNotRequestedAndCreateIfNotFoundAndError() {
        when(azureManagedImageService.findVirtualMachineCustomImage(anyString(), anyString(), any())).thenReturn(Optional.empty());
        when(azureClient.createCustomImage(anyString(), anyString(), anyString(), anyString())).thenThrow(new CloudException("", null));
        thrown.expect(CloudConnectorException.class);

        try {
            underTest.getCustomImageId(RESOURCE_GROUP_NAME, FROM_VHD_URI, authenticatedContext, true, azureClient);

        } catch (CloudConnectorException e) {
            verifyPersistenceNotification(cr -> verify(persistenceNotifier).notifyAllocation(cr.capture(), any()), CommonStatus.REQUESTED);
            verifyPersistenceNotification(cr -> verify(persistenceNotifier).notifyUpdate(cr.capture(), any()), CommonStatus.FAILED);
            verify(azureClient).createCustomImage(CUSTOM_IMAGE_NAME, RESOURCE_GROUP_NAME, FROM_VHD_URI, REGION_NAME);
            throw e;
        }
    }

    @Test
    public void testGetCustomImageIdWhenImageWasNotRequestedAndCreateIfNotFoundAndErrorButImagePresent() {
        when(azureManagedImageService.findVirtualMachineCustomImage(anyString(), anyString(), any()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(virtualMachineCustomImage));
        when(azureClient.createCustomImage(anyString(), anyString(), anyString(), anyString())).thenThrow(new CloudException("", null));
        when(virtualMachineCustomImage.name()).thenReturn(CUSTOM_IMAGE_NAME);
        when(virtualMachineCustomImage.id()).thenReturn(CUSTOM_IMAGE_ID);

        AzureImage azureImage = underTest.getCustomImageId(RESOURCE_GROUP_NAME, FROM_VHD_URI, authenticatedContext, true, azureClient);

        assertEquals(CUSTOM_IMAGE_ID, azureImage.getId());
        assertEquals(CUSTOM_IMAGE_NAME, azureImage.getName());
        verifyPersistenceNotification(cr -> verify(persistenceNotifier).notifyAllocation(cr.capture(), any()), CommonStatus.REQUESTED);
        verifyPersistenceNotification(cr -> verify(persistenceNotifier).notifyUpdate(cr.capture(), any()), CommonStatus.CREATED);
        verify(azureClient).createCustomImage(CUSTOM_IMAGE_NAME, RESOURCE_GROUP_NAME, FROM_VHD_URI, REGION_NAME);
    }

    private void setupAzureClient() {
        Subscription subscription = mock(Subscription.class);
        when(subscription.subscriptionId()).thenReturn(SUBSCRIPTION_ID);
        when(azureClient.getCurrentSubscription()).thenReturn(subscription);
    }

    private void setupAuthenticatedContext() {
        Region region = mock(Region.class);
        Location location = mock(Location.class);
        CloudContext cloudContext = mock(CloudContext.class);
        when(region.getRegionName()).thenReturn(REGION_NAME);
        when(location.getRegion()).thenReturn(region);
        when(cloudContext.getLocation()).thenReturn(location);
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
    }

    private void verifyPersistenceNotification(Consumer<ArgumentCaptor<CloudResource>> argumentCaptorConsumer, CommonStatus imageStatus) {
        ArgumentCaptor<CloudResource> argumentCaptor = ArgumentCaptor.forClass(CloudResource.class);
        argumentCaptorConsumer.accept(argumentCaptor);
        assertEquals(CUSTOM_IMAGE_NAME, argumentCaptor.getValue().getName());
        assertEquals(CUSTOM_IMAGE_ID, argumentCaptor.getValue().getReference());
        assertEquals(imageStatus, argumentCaptor.getValue().getStatus());

    }

    private void verifyPollingStarted() {
        ArgumentCaptor<AzureManagedImageCreationCheckerContext> captor = ArgumentCaptor.forClass(AzureManagedImageCreationCheckerContext.class);
        verify(azureManagedImageCreationPoller).startPolling(any(), captor.capture());
        assertEquals(CUSTOM_IMAGE_NAME, captor.getValue().getImageName());
        assertEquals(RESOURCE_GROUP_NAME, captor.getValue().getResourceGroupName());
    }
}

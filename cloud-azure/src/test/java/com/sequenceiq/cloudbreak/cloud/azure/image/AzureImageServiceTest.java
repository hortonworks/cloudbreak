package com.sequenceiq.cloudbreak.cloud.azure.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.sequenceiq.cloudbreak.cloud.azure.AzureImage;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.task.image.AzureManagedImageCreationCheckerContext;
import com.sequenceiq.cloudbreak.cloud.azure.task.image.AzureManagedImageCreationPoller;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceRetriever;
import com.sequenceiq.common.api.type.CommonStatus;

@RunWith(MockitoJUnitRunner.class)
public class AzureImageServiceTest {

    private static final String RESOURCE_GROUP_NAME = "resourceGroupName";

    private static final String FROM_VHD_URI = "fromVhdUri";

    private static final String CUSTOM_IMAGE_NAME_WITH_REGION = "customImageNameWithRegion";

    private static final String CUSTOM_IMAGE_NAME = "customImageName";

    private static final String CUSTOM_IMAGE_ID = "customImageId";

    private static final String REGION_NAME = "regionName";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private PersistenceRetriever resourcePersistenceRetriever;

    @Mock
    private PersistenceNotifier persistenceNotifier;

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

    @InjectMocks
    private AzureImageService underTest;

    private final AzureImageInfo azureImageInfo =
            new AzureImageInfo(CUSTOM_IMAGE_NAME_WITH_REGION, CUSTOM_IMAGE_NAME, CUSTOM_IMAGE_ID, REGION_NAME, RESOURCE_GROUP_NAME);

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        setupAuthenticatedContext();
    }

    @Test
    public void testFindCustomImageWhenImageExistsOnAzure() {
        imagePresent(true);

        Optional<AzureImage> azureImageOptional = underTest.findImage(azureImageInfo, azureClient, authenticatedContext);

        assertImageFound(azureImageOptional);
        verifyPollingStarted();
    }

    @Test
    public void testFindCustomImageWhenImageIsRequested() {
        imagePresent(false);
        imageRequested(true);

        Optional<AzureImage> azureImageOptional = underTest.findImage(azureImageInfo, azureClient, authenticatedContext);

        assertImageFound(azureImageOptional);
        verifyPollingStarted();
    }

    @Test
    public void testFindCustomImageWhenImageWasNotRequested() {
        imagePresent(false);
        imageRequested(false);

        Optional<AzureImage> azureImageOptional = underTest.findImage(azureImageInfo, azureClient, authenticatedContext);

        assertTrue(azureImageOptional.isEmpty());
        verify(azureManagedImageCreationPoller, never()).startPolling(any(), any());
        verify(azureClient, never()).createImage(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void testCreateCustomImageIfNotFoundAndSuccess() {
        when(azureClient.createImage(anyString(), anyString(), anyString(), anyString())).thenReturn(virtualMachineCustomImage);
        when(virtualMachineCustomImage.name()).thenReturn(CUSTOM_IMAGE_NAME_WITH_REGION);
        when(virtualMachineCustomImage.id()).thenReturn(CUSTOM_IMAGE_ID);

        AzureImage azureImage = underTest.createImage(azureImageInfo, FROM_VHD_URI, azureClient, authenticatedContext);

        assertImageProperties(azureImage);
        verifyPersistenceNotification(cr -> verify(persistenceNotifier).notifyAllocation(cr.capture(), any()), CommonStatus.REQUESTED);
        verifyPersistenceNotification(cr -> verify(persistenceNotifier).notifyUpdate(cr.capture(), any()), CommonStatus.CREATED);
        verify(azureClient).createImage(CUSTOM_IMAGE_NAME_WITH_REGION, RESOURCE_GROUP_NAME, FROM_VHD_URI, REGION_NAME);
    }

    @Test
    public void testCreateCustomImageWhenErrorAndImageNotPresent() {
        imagePresent(false);
        when(azureClient.createImage(anyString(), anyString(), anyString(), anyString())).thenThrow(new CloudException("", null));
        thrown.expect(CloudConnectorException.class);

        try {
            underTest.createImage(azureImageInfo, FROM_VHD_URI, azureClient, authenticatedContext);

        } catch (CloudConnectorException e) {
            verifyPersistenceNotification(cr -> verify(persistenceNotifier).notifyAllocation(cr.capture(), any()), CommonStatus.REQUESTED);
            verifyPersistenceNotification(cr -> verify(persistenceNotifier).notifyUpdate(cr.capture(), any()), CommonStatus.FAILED);
            verify(azureClient).createImage(CUSTOM_IMAGE_NAME_WITH_REGION, RESOURCE_GROUP_NAME, FROM_VHD_URI, REGION_NAME);
            throw e;
        }
    }

    @Test
    public void testCreateCustomImageWhenErrorButImagePresent() {
        imagePresent(true);
        when(azureClient.createImage(anyString(), anyString(), anyString(), anyString())).thenThrow(new CloudException("", null));
        when(virtualMachineCustomImage.name()).thenReturn(CUSTOM_IMAGE_NAME_WITH_REGION);
        when(virtualMachineCustomImage.id()).thenReturn(CUSTOM_IMAGE_ID);

        AzureImage azureImage = underTest.createImage(azureImageInfo, FROM_VHD_URI, azureClient, authenticatedContext);

        assertImageProperties(azureImage);
        verifyPersistenceNotification(cr -> verify(persistenceNotifier).notifyAllocation(cr.capture(), any()), CommonStatus.REQUESTED);
        verifyPersistenceNotification(cr -> verify(persistenceNotifier).notifyUpdate(cr.capture(), any()), CommonStatus.CREATED);
        verify(azureClient).createImage(CUSTOM_IMAGE_NAME_WITH_REGION, RESOURCE_GROUP_NAME, FROM_VHD_URI, REGION_NAME);
    }

    private void setupAuthenticatedContext() {
        CloudContext cloudContext = mock(CloudContext.class);
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
    }

    private void verifyPersistenceNotification(Consumer<ArgumentCaptor<CloudResource>> argumentCaptorConsumer, CommonStatus imageStatus) {
        ArgumentCaptor<CloudResource> argumentCaptor = ArgumentCaptor.forClass(CloudResource.class);
        argumentCaptorConsumer.accept(argumentCaptor);
        assertEquals(CUSTOM_IMAGE_NAME_WITH_REGION, argumentCaptor.getValue().getName());
        assertEquals(CUSTOM_IMAGE_ID, argumentCaptor.getValue().getReference());
        assertEquals(imageStatus, argumentCaptor.getValue().getStatus());

    }

    private void verifyPollingStarted() {
        ArgumentCaptor<AzureManagedImageCreationCheckerContext> captor = ArgumentCaptor.forClass(AzureManagedImageCreationCheckerContext.class);
        verify(azureManagedImageCreationPoller).startPolling(any(), captor.capture());
        assertEquals(CUSTOM_IMAGE_NAME_WITH_REGION, captor.getValue().getAzureImageInfo().getImageNameWithRegion());
        assertEquals(RESOURCE_GROUP_NAME, captor.getValue().getAzureImageInfo().getResourceGroup());
    }

    private void assertImageFound(Optional<AzureImage> azureImageOptional) {
        assertTrue(azureImageOptional.isPresent());
        assertImageProperties(azureImageOptional.get());
    }

    private void assertImageProperties(AzureImage azureImage) {
        assertEquals(CUSTOM_IMAGE_ID, azureImage.getId());
        assertEquals(CUSTOM_IMAGE_NAME_WITH_REGION, azureImage.getName());
    }

    private void imagePresent(boolean present) {
        when(azureManagedImageService.findVirtualMachineCustomImage(azureImageInfo, azureClient))
                .thenReturn(present
                        ? Optional.of(virtualMachineCustomImage)
                        : Optional.empty()
                );
    }

    private void imageRequested(boolean requested) {
        when(resourcePersistenceRetriever.notifyRetrieve(anyString(), any(), any()))
                .thenReturn(requested
                        ? Optional.of(mock(CloudResource.class))
                        : Optional.empty());
    }
}

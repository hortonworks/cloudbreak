package com.sequenceiq.cloudbreak.cloud.azure.image;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.microsoft.azure.management.resources.Subscription;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.resource.AzureResourceIdProviderService;
import com.sequenceiq.cloudbreak.cloud.azure.util.CustomVMImageNameProvider;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;

public class AzureImageInfoServiceTest {

    private static final String RESOURCE_GROUP_NAME = "resourceGroupName";

    private static final String FROM_VHD_URI = "https://somewhere.in.the.windows.net/images/freeipa-cdh--2010061458.vhd";

    private static final String SUBSCRIPTION_ID = "subscriptionId";

    private static final String REGION_NAME = "regionName";

    private static final String CUSTOM_IMAGE_NAME = "customImageName";

    private static final String CUSTOM_IMAGE_ID = "customImageId";

    @Mock
    private AzureResourceIdProviderService azureResourceIdProviderService;

    @Mock
    private CustomVMImageNameProvider customVMImageNameProvider;

    @InjectMocks
    private AzureImageInfoService underTest;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private AzureClient azureClient;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetImageDetails() {
        setupAuthenticatedContext();
        setupAzureClient();
        when(azureResourceIdProviderService.generateImageId(SUBSCRIPTION_ID, RESOURCE_GROUP_NAME, CUSTOM_IMAGE_NAME)).thenReturn(CUSTOM_IMAGE_ID);
        when(customVMImageNameProvider.getImageNameWithRegion(REGION_NAME, FROM_VHD_URI)).thenReturn(CUSTOM_IMAGE_NAME);

        AzureImageInfo azureImageInfo = underTest.getImageInfo(RESOURCE_GROUP_NAME, FROM_VHD_URI, authenticatedContext, azureClient);

        assertEquals(REGION_NAME, azureImageInfo.getRegion());
        assertEquals(CUSTOM_IMAGE_ID, azureImageInfo.getImageId());
        assertEquals(CUSTOM_IMAGE_NAME, azureImageInfo.getImageNameWithRegion());
        assertEquals(RESOURCE_GROUP_NAME, azureImageInfo.getResourceGroup());
        verify(azureResourceIdProviderService).generateImageId(SUBSCRIPTION_ID, RESOURCE_GROUP_NAME, CUSTOM_IMAGE_NAME);
        verify(customVMImageNameProvider).getImageNameWithRegion(REGION_NAME, FROM_VHD_URI);
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
}
package com.sequenceiq.cloudbreak.cloud.azure.validator;

import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureMarketplaceImageProviderService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;

@RunWith(MockitoJUnitRunner.class)
public class AzureImageValidatorTest {

    @Mock
    private AzureMarketplaceImageProviderService azureMarketplaceImageProviderService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @InjectMocks
    private AzureImageFormatValidator underTest;

    private CloudStack cloudStack;

    @Before
    public void setup() {
        cloudStack = new CloudStack(List.of(), null, null, Map.of(), Map.of(), null, null, null, null, null);
    }

    @Test
    public void testImageHasValidVhdFormat() {
        Image image = new Image("https://cldrwestus2.blob.core.windows.net/images/cb-cdh-726-210326090153.vhd", new HashMap<>(), "centos7", "redhat7", "", "default",
                "default-id", new HashMap<>());
        cloudStack = new CloudStack(List.of(), null, image, Map.of(), Map.of(), null, null, null, null, null);
        when(underTest.isVhdImageFormat(image)).thenReturn(true);

        underTest.validate(authenticatedContext, cloudStack);
    }

    @Test
    public void testImageHasValidMarketplaceFormat() {
        Image image = new Image("https://cldrwestus2.blob.core.windows.net/images/cb-cdh-726-210326090153.vhd", new HashMap<>(), "centos7", "redhat7", "", "default",
                "default-id", new HashMap<>());
        cloudStack = new CloudStack(List.of(), null, image, Map.of(), Map.of(), null, null, null, null, null);
        when(underTest.isVhdImageFormat(image)).thenReturn(true);

        underTest.validate(authenticatedContext, cloudStack);
    }

    @Test
    public void testImageHasInvalidFormat() {
        Image image = new Image("https://cldrwestus2.blob.core.windows.net/images/cb-cdh-726-210326090153.vhd", new HashMap<>(), "centos7", "redhat7", "", "default",
                "default-id", new HashMap<>());
        cloudStack = new CloudStack(List.of(), null, image, Map.of(), Map.of(), null, null, null, null, null);
        when(underTest.isVhdImageFormat(image)).thenReturn(true);

//        Assertions.assertThrows()underTest.validate(authenticatedContext, cloudStack);
    }
}
package com.sequenceiq.cloudbreak.cloud.azure.validator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureMarketplaceImageProviderService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;

@ExtendWith(MockitoExtension.class)
public class AzureImageFormatValidatorTest {

    private static final String TEST_ACCOUNT_ID = "accid";

    private static final String TEST_USER_CRN = String.format("crn:cdp:iam:us-west-1:%s:user:mockuser@cloudera.com", TEST_ACCOUNT_ID);

    private static final String VALID_IMAGE_NAME = "https://cldrwestus2.blob.core.windows.net/images/cb-cdh-726-210326090153.vhd";

    private static final String INVALID_IMAGE_NAME = "cldrwestus2.blob.core.windows.net/images/cb-cdh-726-210326090153.vhd\"";

    private static final String MARKETPLACE_IMAGE_NAME = "cloudera:cdp-7_2:freeipa:1.0.2103081333";

    @Mock
    private AzureMarketplaceImageProviderService azureMarketplaceImageProviderService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @InjectMocks
    private AzureImageFormatValidator underTest;

    private CloudStack cloudStack;

    @BeforeAll
    static void setupAll() {
        ThreadBasedUserCrnProvider.setUserCrn(TEST_USER_CRN);
    }

    @Test
    void testImageHasValidVhdFormat() {
        Image image = new Image(VALID_IMAGE_NAME, new HashMap<>(), "centos7", "redhat7", "", "default", "default-id", new HashMap<>());
        cloudStack = new CloudStack(List.of(), null, image, Map.of(), Map.of(), null, null, null, null, null);

        underTest.validate(authenticatedContext, cloudStack);

        verify(entitlementService, times(0)).azureMarketplaceImagesEnabled(any());
    }

    @Test
    void testImageHasValidMarketplaceFormatNoEntitlement() {
        Image image = new Image(MARKETPLACE_IMAGE_NAME, new HashMap<>(), "centos7", "redhat7", "", "default",
                "default-id", new HashMap<>());
        cloudStack = new CloudStack(List.of(), null, image, Map.of(), Map.of(), null, null, null, null, null);

        when(entitlementService.azureMarketplaceImagesEnabled(TEST_ACCOUNT_ID)).thenReturn(false);

        Assertions.assertThrows(CloudConnectorException.class, () -> underTest.validate(authenticatedContext, cloudStack));
    }

    @Test
    void testImageHasValidMarketplaceFormatEntitlement() {
        Image image = new Image(MARKETPLACE_IMAGE_NAME, new HashMap<>(), "centos7", "redhat7", "", "default",
                "default-id", new HashMap<>());
        cloudStack = new CloudStack(List.of(), null, image, Map.of(), Map.of(), null, null, null, null, null);

        when(entitlementService.azureMarketplaceImagesEnabled(TEST_ACCOUNT_ID)).thenReturn(true);

        underTest.validate(authenticatedContext, cloudStack);

        verify(entitlementService, times(1)).azureMarketplaceImagesEnabled(any());
    }

    @Test
    void testImageHasInvalidFormat() {
        Image image = new Image(INVALID_IMAGE_NAME, new HashMap<>(), "centos7", "redhat7", "", "default",
                "default-id", new HashMap<>());
        cloudStack = new CloudStack(List.of(), null, image, Map.of(), Map.of(), null, null, null, null, null);

        Assertions.assertThrows(CloudConnectorException.class, () -> underTest.validate(authenticatedContext, cloudStack));

    }
}
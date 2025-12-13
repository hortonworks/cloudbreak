package com.sequenceiq.cloudbreak.cloud.azure.image;

import static com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureMarketplaceImage.MARKETPLACE_REGION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureMarketplaceImageProviderService;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureImageFormatValidator;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackRepoDetails;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;

@ExtendWith(MockitoExtension.class)
class AzureImageFilterTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:cloudera:user:bob@cloudera.com";

    private static final String ACCOUNT_ID = Crn.fromString(USER_CRN).getAccountId();

    private static final String VALID_MARKETPLACE_IMAGE_NAME = "cloudera:cdp-7_2:runtime-7_2:1.0.2103081333";

    private static final String INVALID_MARKETPLACE_IMAGE_NAME = "cloudera:cdp-7_2:runtime-7_2:1.0.2103081333:latest";

    private static final String AN_AZURE_REGION = "West US";

    private static final String ANOTHER_AZURE_REGION = "West US 2";

    private static final String THIRD_AZURE_REGION = "West US 3";

    private static final String AN_AZURE_IMAGE_NAME = "https://cldrwestus2.blob.core.windows.net/images/cb-cdh-7215-100000000.vhd";

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private AzureImageFormatValidator azureImageFormatValidator;

    @InjectMocks
    private AzureMarketplaceImageProviderService azureMarketplaceImageProviderService = spy(new AzureMarketplaceImageProviderService());

    @InjectMocks
    private AzureImageFilter underTest;

    private List<Image> imageList;

    @BeforeEach
    public void setup() {
        imageList = List.of(
                getMarketplaceImage(),
                getImageWithMarketplaceWithInvalidFormat(),
                getImageWithBothAzureAndMarketplace(),
                getImageWithAzureProviderNoMarketplace(),
                getImageWithMixedPlatformBothAzureAndMarketplace());
        lenient().when(azureMarketplaceImageProviderService.hasMarketplaceFormat(VALID_MARKETPLACE_IMAGE_NAME)).thenReturn(true);
        lenient().when(azureMarketplaceImageProviderService.hasMarketplaceFormat(INVALID_MARKETPLACE_IMAGE_NAME)).thenReturn(false);
    }

    @Test
    public void testMarketplaceOnlyEntitlementGrantedShouldReturnOnlyImagesWithMarketplaceRegionPresent() {

        when(entitlementService.azureMarketplaceImagesEnabled(ACCOUNT_ID)).thenReturn(true);
        when(entitlementService.azureOnlyMarketplaceImagesEnabled(ACCOUNT_ID)).thenReturn(true);

        List<Image> filteredImages = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.filterImages(imageList));
        Set<String> filteredImageUuids = filteredImages.stream().map(Image::getUuid).collect(Collectors.toSet());
        Set<String> regionSet = getRegionSet(filteredImages);
        assertEquals(1, regionSet.size());
        assertEquals("default", regionSet.stream().findFirst().get());

        assertTrue(filteredImageUuids.contains("1"));
        assertTrue(filteredImageUuids.contains("2"));
        assertTrue(filteredImageUuids.contains("3"));
        assertFalse(filteredImageUuids.contains("4"));
        assertFalse(filteredImageUuids.contains("5"));
    }

    @Test
    public void testMarketplaceEntitlementNotGrantedShouldReturnOnlyVhdImages() {

        when(entitlementService.azureMarketplaceImagesEnabled(ACCOUNT_ID)).thenReturn(false);

        List<Image> filteredImages = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.filterImages(imageList));
        Set<String> filteredImageUuids = filteredImages.stream().map(Image::getUuid).collect(Collectors.toSet());
        Set<String> regionSet = getRegionSet(filteredImages);

        assertEquals(3, regionSet.size());
        assertTrue(regionSet.containsAll(List.of(AN_AZURE_REGION, ANOTHER_AZURE_REGION, THIRD_AZURE_REGION)));
        assertTrue(filteredImageUuids.contains("1"));
        assertTrue(filteredImageUuids.contains("2"));
        assertTrue(filteredImageUuids.contains("3"));
        assertTrue(filteredImageUuids.contains("4"));
        assertTrue(filteredImageUuids.contains("5"));
    }

    @Test
    public void testMarketplaceOnlyEntitlementNotGrantedShouldReturnAllImages() {

        when(entitlementService.azureMarketplaceImagesEnabled(ACCOUNT_ID)).thenReturn(true);
        when(entitlementService.azureOnlyMarketplaceImagesEnabled(ACCOUNT_ID)).thenReturn(false);

        List<Image> filteredImages = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.filterImages(imageList));
        Set<String> filteredImageUuids = filteredImages.stream().map(Image::getUuid).collect(Collectors.toSet());
        Set<String> regionSet = getRegionSet(filteredImages);

        assertEquals(4, regionSet.size());
        assertTrue(regionSet.containsAll(List.of(AN_AZURE_REGION, ANOTHER_AZURE_REGION, THIRD_AZURE_REGION, MARKETPLACE_REGION)));
        assertTrue(filteredImageUuids.contains("1"));
        assertTrue(filteredImageUuids.contains("2"));
        assertTrue(filteredImageUuids.contains("3"));
        assertTrue(filteredImageUuids.contains("4"));
        assertTrue(filteredImageUuids.contains("5"));
    }

    private Set<String> getRegionSet(List<Image> filteredImages) {
        return filteredImages.stream().map(Image::getImageSetsByProvider)
                .map(i -> i.get(CloudConstants.AZURE.toLowerCase(Locale.ROOT)))
                .filter(Objects::nonNull)
                .map(Map::keySet)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    private Image getMarketplaceImage() {
        Map<String, Map<String, String>> imageSetsByProvider = new HashMap<>();
        Map<String, String> regionImageIdMap = new HashMap<>();
        regionImageIdMap.put(MARKETPLACE_REGION, VALID_MARKETPLACE_IMAGE_NAME);
        imageSetsByProvider.put(CloudConstants.AZURE.toLowerCase(Locale.ROOT), regionImageIdMap);

        return createImage(imageSetsByProvider, "1");
    }

    private Image getImageWithMixedPlatformBothAzureAndMarketplace() {
        Map<String, Map<String, String>> imageSetsByProvider = new HashMap<>();

        Map<String, String> regionImageIdMap = new HashMap<>();
        regionImageIdMap.put(MARKETPLACE_REGION, VALID_MARKETPLACE_IMAGE_NAME);
        regionImageIdMap.put(AN_AZURE_REGION, AN_AZURE_REGION);
        regionImageIdMap.put(ANOTHER_AZURE_REGION, ANOTHER_AZURE_REGION);
        regionImageIdMap.put(THIRD_AZURE_REGION, THIRD_AZURE_REGION);
        imageSetsByProvider.put(CloudConstants.AZURE.toLowerCase(Locale.ROOT), regionImageIdMap);

        regionImageIdMap = new HashMap<>();
        regionImageIdMap.put("us-west-1", "ami-0a0986bb98dbabcde");
        imageSetsByProvider.put(CloudConstants.AWS.toLowerCase(Locale.ROOT), regionImageIdMap);

        return createImage(imageSetsByProvider, "2");
    }

    private Image getImageWithBothAzureAndMarketplace() {
        Map<String, Map<String, String>> imageSetsByProvider = new HashMap<>();

        Map<String, String> regionImageIdMap = new HashMap<>();
        regionImageIdMap.put(MARKETPLACE_REGION, VALID_MARKETPLACE_IMAGE_NAME);
        regionImageIdMap.put(AN_AZURE_REGION, AN_AZURE_IMAGE_NAME);
        imageSetsByProvider.put(CloudConstants.AZURE.toLowerCase(Locale.ROOT), regionImageIdMap);

        return createImage(imageSetsByProvider, "3");
    }

    private Image getImageWithMarketplaceWithInvalidFormat() {
        Map<String, Map<String, String>> imageSetsByProvider = new HashMap<>();

        Map<String, String> regionImageIdMap = new HashMap<>();
        regionImageIdMap.put(MARKETPLACE_REGION, INVALID_MARKETPLACE_IMAGE_NAME);
        imageSetsByProvider.put(CloudConstants.AZURE.toLowerCase(Locale.ROOT), regionImageIdMap);

        return createImage(imageSetsByProvider, "4");
    }

    private Image getImageWithAzureProviderNoMarketplace() {
        Map<String, Map<String, String>> imageSetsByProvider = new HashMap<>();

        Map<String, String> regionImageIdMap = new HashMap<>();
        regionImageIdMap.put(AN_AZURE_REGION, AN_AZURE_IMAGE_NAME);
        imageSetsByProvider.put(CloudConstants.AZURE.toLowerCase(Locale.ROOT), regionImageIdMap);

        return createImage(imageSetsByProvider, "5");
    }

    private Image createImage(Map<String, Map<String, String>> imageSetsByProvider, String uuid) {
        StackRepoDetails repoDetails = new StackRepoDetails(Collections.emptyMap(), Collections.emptyMap());
        ImageStackDetails stackDetails = new ImageStackDetails("7.2.15", repoDetails, "1");

        return Image.builder()
                .withUuid(uuid)
                .withCreated(System.currentTimeMillis())
                .withPublished(System.currentTimeMillis())
                .withOs("redhat7")
                .withOsType("redhat7")
                .withImageSetsByProvider(imageSetsByProvider)
                .withStackDetails(stackDetails)
                .withCmBuildNumber("1")
                .withAdvertised(true)
                .build();
    }
}
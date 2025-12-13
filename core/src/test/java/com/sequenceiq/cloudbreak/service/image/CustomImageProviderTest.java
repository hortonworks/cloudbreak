package com.sequenceiq.cloudbreak.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.CustomImage;
import com.sequenceiq.cloudbreak.domain.VmImage;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.type.ImageType;

class CustomImageProviderTest {

    private static final String CUSTOM_IMAGE_CATALOG_URL = "http://localhost/custom-imagecatalog-url";

    private static final String V3_CB_CATALOG_FILE = "com/sequenceiq/cloudbreak/service/image/cb-image-catalog-v3.json";

    private static final String V3_FREEIPA_CATALOG_FILE = "com/sequenceiq/cloudbreak/service/image/freeipa-image-catalog-v3.json";

    private static final String CUSTOM_CATALOG_NAME = "custom-catalog";

    private static final String CUSTOM_IMAGE_ID = "customImageId";

    private static final String CUSTOM_IMAGE_DESCRIPTION = "Test description";

    private static final String CUSTOM_BASE_PARCEL_URL = "https://myarchive.test.com";

    private CustomImageProvider underTest = new CustomImageProvider();

    @Test
    void testMergeSourceImageAndCustomImagePropertiesInCaseOfRuntimeImage() throws Exception {

        StatedImage statedImage = getStatedImageFromCatalog(V3_CB_CATALOG_FILE, "949bffa3-17d4-4076-9d5a-bf3d23c1086b");
        CustomImage customImage = getCustomImage(ImageType.RUNTIME, CUSTOM_IMAGE_ID, CUSTOM_BASE_PARCEL_URL);

        StatedImage image = underTest.mergeSourceImageAndCustomImageProperties(statedImage, customImage, CUSTOM_IMAGE_CATALOG_URL, CUSTOM_CATALOG_NAME);

        validateCommonParameters(statedImage, customImage, image);
        assertFalse(image.getImage().getPreWarmParcels().contains(CUSTOM_BASE_PARCEL_URL));
        assertFalse(image.getImage().getPreWarmCsd().contains(CUSTOM_BASE_PARCEL_URL));
        assertEquals("https://myarchive.test.com/cm-public/7.2.2/redhat7/yum/", image.getImage().getRepo().get("redhat7"));
        assertTrue(image.getImage().getImageSetsByProvider().containsKey("aws"));
        assertEquals(16, image.getImage().getImageSetsByProvider().get("aws").size());
        assertEquals("https://myarchive.test.com/cdp-public/7.2.2.0/parcels/", image.getImage().getStackDetails().getRepo().getStack().get("redhat7"));
    }

    @Test
    void testMergeSourceImageAndCustomImagePropertiesInCaseOfFreeipaImage() throws Exception {

        StatedImage statedImage = getStatedImageFromCatalog(V3_FREEIPA_CATALOG_FILE, "3b6ae396-df40-4e2b-7c2b-54b15822614c");
        CustomImage customImage = getCustomImage(ImageType.FREEIPA, CUSTOM_IMAGE_ID, CUSTOM_BASE_PARCEL_URL);
        VmImage vmImage1 = new VmImage();
        vmImage1.setRegion("europe-west1");
        vmImage1.setImageReference("cloudera-freeipa-images/freeipa-cdh--2103081304.tar.gz");
        customImage.setVmImage(Set.of(vmImage1));

        StatedImage image = underTest.mergeSourceImageAndCustomImageProperties(statedImage, customImage, CUSTOM_IMAGE_CATALOG_URL, CUSTOM_CATALOG_NAME);

        validateCommonParameters(statedImage, customImage, image);
        assertTrue(image.getImage().getRepo().isEmpty());
        assertTrue(image.getImage().getPreWarmParcels().isEmpty());
        assertTrue(image.getImage().getPreWarmCsd().isEmpty());

        Map<String, Map<String, String>> imageSetsByProvider = image.getImage().getImageSetsByProvider();
        assertTrue(imageSetsByProvider.containsKey("aws"));
        assertEquals(1, imageSetsByProvider.get("aws").size());
        assertTrue(imageSetsByProvider.get("aws").containsKey("europe-west1"));
        assertEquals("cloudera-freeipa-images/freeipa-cdh--2103081304.tar.gz", image.getImage().getImageSetsByProvider().get("aws").get("europe-west1"));
    }

    private void validateCommonParameters(StatedImage statedImage, CustomImage customImage, StatedImage result) {
        assertEquals(statedImage.getImage().getDate(), result.getImage().getDate());
        assertEquals(customImage.getCreated(), result.getImage().getCreated());
        assertEquals(customImage.getCreated(), result.getImage().getPublished());
        assertEquals(customImage.getDescription(), result.getImage().getDescription());
        assertEquals(statedImage.getImage().getOs(), result.getImage().getOs());
        assertEquals(customImage.getName(), result.getImage().getUuid());
        assertEquals(statedImage.getImage().getVersion(), result.getImage().getVersion());
        assertEquals(statedImage.getImage().getOsType(), result.getImage().getOsType());
        assertEquals(statedImage.getImage().getPackageVersions(), result.getImage().getPackageVersions());
        assertEquals(statedImage.getImage().getCmBuildNumber(), result.getImage().getCmBuildNumber());
        assertEquals(statedImage.getImage().isAdvertised(), result.getImage().isAdvertised());
        assertEquals(customImage.getBaseParcelUrl(), result.getImage().getBaseParcelUrl());
        assertEquals(customImage.getCustomizedImageId(), result.getImage().getSourceImageId());
        assertEquals(statedImage.getImage().getArchitecture(), result.getImage().getArchitecture());
        assertEquals(statedImage.getImage().getTags(), result.getImage().getTags());
    }

    private Image getImageFromCatalog(String catalogFile, String imageId) throws IOException, CloudbreakImageNotFoundException {
        String catalogJson = FileReaderUtils.readFileFromClasspath(catalogFile);
        CloudbreakImageCatalogV3 catalog = JsonUtil.readValue(catalogJson, CloudbreakImageCatalogV3.class);
        Images images = catalog.getImages();
        List<Image> imagesAll = List.of(images.getBaseImages(), images.getCdhImages(), images.getFreeIpaImages())
                .stream().flatMap(List::stream).collect(Collectors.toList());
        Optional<? extends Image> image = findImageByImageId(imageId, imagesAll);
        if (image.isEmpty()) {
            throw new CloudbreakImageNotFoundException(String.format("No image was found with id %s", imageId));
        }
        return image.get();
    }

    private StatedImage getStatedImageFromCatalog(String catalogFile, String imageId) throws IOException, CloudbreakImageNotFoundException {
        Image image = getImageFromCatalog(catalogFile, imageId);
        return StatedImage.statedImage(image, CUSTOM_IMAGE_CATALOG_URL, CUSTOM_CATALOG_NAME);
    }

    private CustomImage getCustomImage(ImageType imageType, String customizedImageId, String baseParcelUrl) {
        CustomImage customImage = new CustomImage();
        customImage.setId(0L);
        customImage.setName(CUSTOM_IMAGE_ID);
        customImage.setDescription(CUSTOM_IMAGE_DESCRIPTION);
        customImage.setImageType(imageType);
        customImage.setCustomizedImageId(customizedImageId);
        customImage.setBaseParcelUrl(baseParcelUrl);
        return customImage;
    }

    private Optional<? extends Image> findImageByImageId(String imageId, Collection<? extends Image> images) {
        return images.stream().filter(img -> img.getUuid().equals(imageId)).findFirst();
    }
}
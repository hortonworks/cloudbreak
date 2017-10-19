package com.sequenceiq.cloudbreak.service.image;

import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV2;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakVersion;

@RunWith(MockitoJUnitRunner.class)
public class ImageCatalogProviderTest {

    private static final String CB_IMAGE_CATALOG_V2_JSON = "cb-image-catalog-v2.json";

    private static final String CB_VERSION = "1.16.4";

    @InjectMocks
    private CachedImageCatalogProvider underTest;

    @Before
    public void before() {
        try {
            String path = TestUtil.getFilePath(getClass(), CB_IMAGE_CATALOG_V2_JSON).getParent().toString();
            underTest.setEtcConfigDir(path);
        } catch (Exception e) {
            underTest.setEtcConfigDir("");
        }
    }

    @Test
    public void testReadImageCatalogFromFile() throws Exception {
        CloudbreakImageCatalogV2 catalog = underTest.getImageCatalogV2(CB_IMAGE_CATALOG_V2_JSON);

        Assert.assertNotNull("Check that the parsed ImageCatalog not null.", catalog);
        Optional<CloudbreakVersion> ver = catalog.getVersions().getCloudbreakVersions().stream().filter(v -> v.getVersions().contains(CB_VERSION)).findFirst();
        Assert.assertTrue("Check that the parsed ImageCatalog contains the desired version of Cloudbreak.", ver.isPresent());
        List<String> imageIds = ver.get().getImageIds();
        Assert.assertNotNull("Check that the parsed ImageCatalog contains the desired version of Cloudbreak with image id(s).", imageIds);
        Optional<String> imageIdOptional = imageIds.stream().findFirst();
        Assert.assertTrue("Check that the parsed ImageCatalog contains Ambari image reference for the Cloudbreak version.", imageIdOptional.isPresent());
        String imageId = imageIdOptional.get();
        boolean baseImageFound = false;
        boolean hdpImageFound = false;
        boolean hdfImageFoiund = false;
        if (catalog.getImages().getBaseImages() != null) {
            baseImageFound = catalog.getImages().getBaseImages().stream().anyMatch(i -> i.getUuid().equals(imageId));
        }
        if (catalog.getImages().getHdpImages() != null) {
            hdpImageFound = catalog.getImages().getHdpImages().stream().anyMatch(i -> i.getUuid().equals(imageId));
        }
        if (catalog.getImages().getHdfImages() != null) {
            hdfImageFoiund = catalog.getImages().getHdfImages().stream().anyMatch(i -> i.getUuid().equals(imageId));
        }
        boolean anyImageFoundForVersion = baseImageFound || hdpImageFound || hdfImageFoiund;
        Assert.assertTrue("Check that the parsed ImageCatalog contains Ambari image for the Cloudbreak version.", anyImageFoundForVersion);
    }

//    @Test
//    public void testReadImageCatalogFromHTTP() {
//        CloudbreakImageCatalogV2 catalog = underTest.getImageCatalogV2();
//
//        Assert.assertNotNull("Check that the parsed ImageCatalog not null.", catalog);
//        Optional<CloudbreakVersion> ver = catalog.getVersions().getCloudbreakVersions().stream().filter(v -> v.getVersions().contains(CB_VERSION)).findFirst();
//        Assert.assertTrue("Check that the parsed ImageCatalog contains the desired version of Cloudbreak.", ver.isPresent());
//        List<String> imageIds = ver.get().getImageIds();
//        Assert.assertNotNull("Check that the parsed ImageCatalog contains the desired version of Cloudbreak with image id(s).", imageIds);
//        Optional<String> imageIdOptional = ver.get().getImageIds().stream().findFirst();
//        Assert.assertTrue("Check that the parsed ImageCatalog contains Ambari image reference for the Cloudbreak version.", imageIdOptional.isPresent());
//        String imageId = imageIdOptional.get();
//        boolean baseImageFound = false;
//        boolean hdpImageFound = false;
//        boolean hdfImageFoiund = false;
//        if (catalog.getImages().getBaseImages() != null) {
//            baseImageFound = catalog.getImages().getBaseImages().stream().anyMatch(i -> i.getUuid().equals(imageId));
//        }
//        if (catalog.getImages().getHdpImages() != null) {
//            hdpImageFound = catalog.getImages().getHdpImages().stream().anyMatch(i -> i.getUuid().equals(imageId));
//        }
//        if (catalog.getImages().getHdfImages() != null) {
//            hdfImageFoiund = catalog.getImages().getHdfImages().stream().anyMatch(i -> i.getUuid().equals(imageId));
//        }
//        boolean anyImageFoundForVersion = baseImageFound || hdpImageFound || hdfImageFoiund;
//        Assert.assertTrue("Check that the parsed ImageCatalog contains Ambari image for the Cloudbreak version.", anyImageFoundForVersion);
//    }
}
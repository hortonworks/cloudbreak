package com.sequenceiq.it.cloudbreak.util;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class TestUpgradeCandidateProviderTest {

    private static final String RUNTIME_731 = "7.3.1";

    private TestUpgradeCandidateProvider underTest;

    @BeforeMethod
    public void setUp() {
        underTest = new TestUpgradeCandidateProvider();
    }

    @Test
    public void testHasDifferentBuildNumberSameCmTargetHasHigherCdhBuild() {
        ImageV4Response source = createImage("02e1ac9c", "7.13.1.706", "76410574", "76397095", 1779357927L);
        ImageV4Response target = createImage("cdc4ace6", "7.13.1.706", "76410574", "78150083", 1778051478L);

        assertTrue(underTest.hasDifferentBuildNumber(source, target));
    }

    @Test
    public void testHasDifferentBuildNumberSameCmTargetHasLowerCdhBuild() {
        ImageV4Response source = createImage("cdc4ace6", "7.13.1.706", "76410574", "78150083", 1778051478L);
        ImageV4Response target = createImage("02e1ac9c", "7.13.1.706", "76410574", "76397095", 1779357927L);

        assertFalse(underTest.hasDifferentBuildNumber(source, target));
    }

    @Test
    public void testHasDifferentBuildNumberSameCmSameCdhBuild() {
        ImageV4Response source = createImage("image-1", "7.13.1.706", "76410574", "76397095", 1779357927L);
        ImageV4Response target = createImage("image-2", "7.13.1.706", "76410574", "76397095", 1779357928L);

        assertFalse(underTest.hasDifferentBuildNumber(source, target));
    }

    @Test
    public void testHasDifferentBuildNumberNewerCmHigherCdhBuild() {
        ImageV4Response source = createImage("image-1", "7.13.1.706", "76410574", "76397095", 1779357927L);
        ImageV4Response target = createImage("image-2", "7.13.2.0", "77000000", "78150083", 1779400000L);

        assertTrue(underTest.hasDifferentBuildNumber(source, target));
    }

    @Test
    public void testHasDifferentBuildNumberOlderCmHigherCdhBuild() {
        ImageV4Response source = createImage("image-1", "7.13.1.706", "76410574", "76397095", 1779357927L);
        ImageV4Response target = createImage("image-2", "7.13.0.0", "75000000", "78150083", 1779400000L);

        assertFalse(underTest.hasDifferentBuildNumber(source, target));
    }

    @Test
    public void testFindUpgradePairRealCatalogScenario() {
        ImageV4Response osUpdateRelease = createImage("02e1ac9c", "7.13.1.706", "76410574", "76397095", 1779357927L);
        ImageV4Response emergencyRelease = createImage("cdc4ace6", "7.13.1.706", "76410574", "78150083", 1778051478L);

        List<ImageV4Response> sourceImages = List.of(osUpdateRelease, emergencyRelease);
        List<ImageV4Response> targetImages = List.of(osUpdateRelease, emergencyRelease);

        Pair<String, String> result = underTest.findUpgradePair(sourceImages, targetImages, underTest::hasDifferentBuildNumber, RUNTIME_731);

        assertEquals(result.getLeft(), "02e1ac9c");
        assertEquals(result.getRight(), "cdc4ace6");
    }

    @Test(expectedExceptions = TestFailException.class)
    public void testFindUpgradePairNoValidPairAllSameCdhBuild() {
        ImageV4Response image1 = createImage("image-1", "7.13.1.706", "76410574", "76397095", 1779357927L);
        ImageV4Response image2 = createImage("image-2", "7.13.1.706", "76410574", "76397095", 1779357928L);

        List<ImageV4Response> sourceImages = List.of(image1, image2);
        List<ImageV4Response> targetImages = List.of(image1, image2);

        underTest.findUpgradePair(sourceImages, targetImages, underTest::hasDifferentBuildNumber, RUNTIME_731);
    }

    @Test(expectedExceptions = TestFailException.class)
    public void testFindUpgradePairSingleImage() {
        ImageV4Response image = createImage("02e1ac9c", "7.13.1.706", "76410574", "76397095", 1779357927L);

        List<ImageV4Response> sourceImages = List.of(image);
        List<ImageV4Response> targetImages = List.of(image);

        underTest.findUpgradePair(sourceImages, targetImages, underTest::hasDifferentBuildNumber, RUNTIME_731);
    }

    @Test(expectedExceptions = TestFailException.class)
    public void testFindUpgradePairEmptyLists() {
        underTest.findUpgradePair(List.of(), List.of(), underTest::hasDifferentBuildNumber, RUNTIME_731);
    }

    @Test
    public void testFindUpgradePairMultipleCandidatesReturnsFirstMatch() {
        ImageV4Response oldImage = createImage("old-image", "7.13.1.0", "60343691", "60371244", 1733695711L);
        ImageV4Response midImage = createImage("mid-image", "7.13.1.706", "76410574", "76397095", 1779357927L);
        ImageV4Response newImage = createImage("new-image", "7.13.1.706", "76410574", "78150083", 1778051478L);

        List<ImageV4Response> sourceImages = List.of(midImage, oldImage);
        List<ImageV4Response> targetImages = List.of(newImage, midImage);

        Pair<String, String> result = underTest.findUpgradePair(sourceImages, targetImages, underTest::hasDifferentBuildNumber, RUNTIME_731);

        assertEquals(result.getLeft(), "mid-image");
        assertEquals(result.getRight(), "new-image");
    }

    private ImageV4Response createImage(String uuid, String cmVersion, String cmBuildNumber, String cdhBuildNumber, Long created) {
        ImageV4Response image = new ImageV4Response();
        image.setUuid(uuid);
        image.setCreated(created);
        image.setPackageVersions(Map.of(
                ImagePackageVersion.CM.getKey(), cmVersion,
                ImagePackageVersion.CM_BUILD_NUMBER.getKey(), cmBuildNumber,
                ImagePackageVersion.CDH_BUILD_NUMBER.getKey(), cdhBuildNumber
        ));
        return image;
    }
}

package com.sequenceiq.it.cloudbreak.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.BaseStackDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.BaseStackRepoDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

class TestUpgradeCandidateProviderTest {

    private static final String RUNTIME_731 = "7.3.1";

    private TestUpgradeCandidateProvider underTest;

    @BeforeEach
    void setUp() {
        underTest = new TestUpgradeCandidateProvider();
        CommonClusterManagerProperties properties = new CommonClusterManagerProperties();
        properties.getUpgrade().setRuntimeImageRepositoryVersionExclusions(Map.of("7.3.1", List.of(".p900.")));
        ReflectionTestUtils.setField(underTest, "commonClusterManagerProperties", properties);
    }

    @Test
    void testHasDifferentBuildNumberSameCmTargetHasHigherCdhBuild() {
        ImageV4Response source = createImage("02e1ac9c", "7.13.1.706", "76410574", "76397095", 1779357927L);
        ImageV4Response target = createImage("cdc4ace6", "7.13.1.706", "76410574", "78150083", 1778051478L);

        assertTrue(underTest.hasDifferentBuildNumber(source, target));
    }

    @Test
    void testHasDifferentBuildNumberSameCmTargetHasLowerCdhBuild() {
        ImageV4Response source = createImage("cdc4ace6", "7.13.1.706", "76410574", "78150083", 1778051478L);
        ImageV4Response target = createImage("02e1ac9c", "7.13.1.706", "76410574", "76397095", 1779357927L);

        assertFalse(underTest.hasDifferentBuildNumber(source, target));
    }

    @Test
    void testHasDifferentBuildNumberSameCmSameCdhBuild() {
        ImageV4Response source = createImage("image-1", "7.13.1.706", "76410574", "76397095", 1779357927L);
        ImageV4Response target = createImage("image-2", "7.13.1.706", "76410574", "76397095", 1779357928L);

        assertFalse(underTest.hasDifferentBuildNumber(source, target));
    }

    @Test
    void testHasDifferentBuildNumberNewerCmHigherCdhBuild() {
        ImageV4Response source = createImage("image-1", "7.13.1.706", "76410574", "76397095", 1779357927L);
        ImageV4Response target = createImage("image-2", "7.13.2.0", "77000000", "78150083", 1779400000L);

        assertTrue(underTest.hasDifferentBuildNumber(source, target));
    }

    @Test
    void testHasDifferentBuildNumberOlderCmHigherCdhBuild() {
        ImageV4Response source = createImage("image-1", "7.13.1.706", "76410574", "76397095", 1779357927L);
        ImageV4Response target = createImage("image-2", "7.13.0.0", "75000000", "78150083", 1779400000L);

        assertFalse(underTest.hasDifferentBuildNumber(source, target));
    }

    @Test
    void testFindUpgradePairRealCatalogScenario() {
        ImageV4Response osUpdateRelease = createImage("02e1ac9c", "7.13.1.706", "76410574", "76397095", 1779357927L);
        ImageV4Response emergencyRelease = createImage("cdc4ace6", "7.13.1.706", "76410574", "78150083", 1778051478L);

        List<ImageV4Response> sourceImages = List.of(osUpdateRelease, emergencyRelease);
        List<ImageV4Response> targetImages = List.of(osUpdateRelease, emergencyRelease);

        Pair<String, String> result = underTest.findUpgradePair(sourceImages, targetImages, underTest::hasDifferentBuildNumber, RUNTIME_731);

        assertEquals("02e1ac9c", result.getLeft());
        assertEquals("cdc4ace6", result.getRight());
    }

    @Test
    void testFindUpgradePairNoValidPairFallsBackToNewerCdhBuild() {
        ImageV4Response osUpdate = createImage("os-update", "7.13.1.706", "76410574", "76397095", 1779357979L);
        ImageV4Response emergency = createImage("emergency", "7.13.1.706", "76410574", "78150083", 1778051561L);

        List<ImageV4Response> sourceImages = List.of(osUpdate, emergency);
        List<ImageV4Response> targetImages = List.of(osUpdate, emergency);

        Pair<String, String> result = underTest.findUpgradePair(sourceImages, targetImages, underTest::hasSameBuildNumber, RUNTIME_731);

        assertEquals("os-update", result.getLeft());
        assertEquals("emergency", result.getRight());
    }

    @Test
    void testFindUpgradePairNoValidPairSameCdhBuildFallbackThrows() {
        ImageV4Response image1 = createImage("image-1", "7.13.1.706", "76410574", "76397095", 1779357927L);
        ImageV4Response image2 = createImage("image-2", "7.13.1.706", "76410574", "76397095", 1779357928L);

        List<ImageV4Response> sourceImages = List.of(image1, image2);
        List<ImageV4Response> targetImages = List.of(image1, image2);

        assertThrows(TestFailException.class, () -> underTest.findUpgradePair(sourceImages, targetImages, underTest::hasDifferentBuildNumber, RUNTIME_731));
    }

    @Test
    void testFindUpgradePairSingleImage() {
        ImageV4Response image = createImage("02e1ac9c", "7.13.1.706", "76410574", "76397095", 1779357927L);

        List<ImageV4Response> sourceImages = List.of(image);
        List<ImageV4Response> targetImages = List.of(image);

        assertThrows(TestFailException.class, () -> underTest.findUpgradePair(sourceImages, targetImages, underTest::hasDifferentBuildNumber, RUNTIME_731));
    }

    @Test
    void testFindUpgradePairEmptyLists() {
        assertThrows(TestFailException.class, () -> underTest.findUpgradePair(List.of(), List.of(), underTest::hasDifferentBuildNumber, RUNTIME_731));
    }

    @Test
    void testFindUpgradePairMultipleCandidatesReturnsFirstMatch() {
        ImageV4Response oldImage = createImage("old-image", "7.13.1.0", "60343691", "60371244", 1733695711L);
        ImageV4Response midImage = createImage("mid-image", "7.13.1.706", "76410574", "76397095", 1779357927L);
        ImageV4Response newImage = createImage("new-image", "7.13.1.706", "76410574", "78150083", 1778051478L);

        List<ImageV4Response> sourceImages = List.of(midImage, oldImage);
        List<ImageV4Response> targetImages = List.of(newImage, midImage);

        Pair<String, String> result = underTest.findUpgradePair(sourceImages, targetImages, underTest::hasDifferentBuildNumber, RUNTIME_731);

        assertEquals("mid-image", result.getLeft());
        assertEquals("new-image", result.getRight());
    }

    @Test
    void testExcludedByRuntimeRuleWhenRuntimeRuleMatchesRepositoryVersion() {
        ImageV4Response image = createImage("service-pack", "7.13.1.706", "76410574", "80138085", 1779357927L);
        image.setStackDetails(createStackDetailsWithRepositoryVersion("7.3.1-1.cdh7.3.1.p900.80138085"));

        assertTrue(underTest.excludedByRuntimeRule("7.3.1", image));
    }

    @Test
    void testExcludedByRuntimeRuleWhenRuntimeHasNoRules() {
        ImageV4Response image = createImage("service-pack", "7.13.1.706", "76410574", "80138085", 1779357927L);
        image.setStackDetails(createStackDetailsWithRepositoryVersion("7.3.1-1.cdh7.3.1.p900.80138085"));

        assertFalse(underTest.excludedByRuntimeRule("7.3.2", image));
    }

    @Test
    void testExcludedByRuntimeRuleWhenRepositoryVersionMissing() {
        ImageV4Response image = createImage("service-pack", "7.13.1.706", "76410574", "80138085", 1779357927L);

        assertFalse(underTest.excludedByRuntimeRule("7.3.1", image));
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

    private BaseStackDetailsV4Response createStackDetailsWithRepositoryVersion(String repositoryVersion) {
        BaseStackRepoDetailsV4Response stackRepo = new BaseStackRepoDetailsV4Response();
        stackRepo.setStack(Map.of("repository-version", repositoryVersion));

        BaseStackDetailsV4Response stackDetails = new BaseStackDetailsV4Response();
        stackDetails.setRepository(stackRepo);
        return stackDetails;
    }
}

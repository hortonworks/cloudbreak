package com.sequenceiq.cloudbreak.service.upgrade.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;
import com.sequenceiq.cloudbreak.service.image.ImageTestBuilder;

class PythonVersionBasedRuntimeVersionValidatorTest {

    private final PythonVersionBasedRuntimeVersionValidator underTest = new PythonVersionBasedRuntimeVersionValidator();

    @Test
    void testShouldPermitUpgradeWhenTheTargetVersionIsEqualsWithTheMinimum() {
        Image currentImage = createImage("7.2.15", true);
        Image targetImage = createImage("7.2.16", true);

        assertTrue(underTest.isUpgradePermittedForRuntime(currentImage, targetImage));
    }

    @Test
    void testShouldPermitUpgradeWhenTheTargetVersionIsHigherWithTheMinimum() {
        Image currentImage = createImage("7.2.15", true);
        Image targetImage = createImage("7.2.17", true);

        assertTrue(underTest.isUpgradePermittedForRuntime(currentImage, targetImage));
    }

    @Test
    void testShouldPermitUpgradeWhenTheTargetVersionIsLowerThenTheMinimumAndNotPythonRequired() {
        Image currentImage = createImage("7.2.15", true);
        Image targetImage = createImage("7.2.15", false);

        assertTrue(underTest.isUpgradePermittedForRuntime(currentImage, targetImage));
    }

    @Test
    void testShouldPermitUpgradeWhenTheTargetVersionIsLowerThenTheMinimumAndPythonIsPresentOnTheImage() {
        Image currentImage = createImage("7.2.15", true);
        Image targetImage = createImage("7.2.15", true);

        assertTrue(underTest.isUpgradePermittedForRuntime(currentImage, targetImage));
    }

    @Test
    void testShouldPermitUpgradeWhenTheCurrentImageDoesNotContainsPythonAndTheTargetDoesNotRequires() {
        Image currentImage = createImage("7.2.15", false);
        Image targetImage = createImage("7.2.16", false);

        assertTrue(underTest.isUpgradePermittedForRuntime(currentImage, targetImage));
    }

    @Test
    void testShouldNotPermitUpgradeWhenTheCurrentImageDoesNotContainsPythonAndTheTargetRequiresIt() {
        Image currentImage = createImage("7.2.15", false);
        Image targetImage = createImage("7.2.16", true);

        assertFalse(underTest.isUpgradePermittedForRuntime(currentImage, targetImage));
    }

    @Test
    void testShouldNotPermitUpgradeWhenTheCurrentImageDoesNotContainsPythonAndTheTargetRequiresItAndTheVersionsAreEqualsWithTheMinimum() {
        Image currentImage = createImage("7.2.16", false);
        Image targetImage = createImage("7.2.16", true);

        assertFalse(underTest.isUpgradePermittedForRuntime(currentImage, targetImage));
    }

    @Test
    void testShouldNotPermitUpgradeWhenTheCurrentImageDoesNotContainsPythonAndTheTargetRequiresItAndTheTargetVersionIsHigherThanTheMinimum() {
        Image currentImage = createImage("7.2.16", false);
        Image targetImage = createImage("7.2.17", true);

        assertFalse(underTest.isUpgradePermittedForRuntime(currentImage, targetImage));
    }

    private Image createImage(String runtimeVersion, boolean containsPython38) {
        return ImageTestBuilder.builder()
                .withStackDetails(new ImageStackDetails(runtimeVersion, null, null))
                .withPackageVersions(containsPython38 ? Map.of(ImagePackageVersion.PYTHON38.getKey(), "3.8") : Collections.emptyMap())
                .build();
    }

}
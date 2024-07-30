package com.sequenceiq.it.util.imagevalidation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.testng.ITestResult;
import org.testng.util.Strings;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImagesV4Response;
import com.sequenceiq.cloudbreak.cloud.model.Architecture;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.listener.PassedTestsReporter;
import com.sequenceiq.it.cloudbreak.testcase.e2e.freeipa.FreeIpaUpgradeTests;
import com.sequenceiq.it.cloudbreak.testcase.e2e.imagevalidation.BaseImageValidatorE2ETest;
import com.sequenceiq.it.cloudbreak.testcase.e2e.imagevalidation.PrewarmImageValidatorE2ETest;
import com.sequenceiq.it.cloudbreak.testcase.e2e.imagevalidation.YarnImageValidatorE2ETest;
import com.sequenceiq.it.cloudbreak.testcase.e2e.java.ForceJavaVersionE2ETest;
import com.sequenceiq.it.cloudbreak.testcase.e2e.l0promotion.BasicEnvironmentVirtualGroupTest;
import com.sequenceiq.it.cloudbreak.testcase.e2e.l0promotion.DatalakeCcmUpgradeAndRotationTest;
import com.sequenceiq.it.cloudbreak.testcase.e2e.l0promotion.MonitoringTests;
import com.sequenceiq.it.cloudbreak.testcase.e2e.sdx.SdxUpgradeTests;
import com.sequenceiq.it.util.TestNGUtil;

@Component
public class ImageValidatorE2ETestUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageValidatorE2ETestUtil.class);

    private static final int IMAGE_WAIT_SLEEP_TIME_IN_SECONDS = 10;

    @Inject
    private CommonCloudProperties commonCloudProperties;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Inject
    private ImageCatalogTestClient imageCatalogTestClient;

    @Inject
    private TestNGUtil testNGUtil;

    @Value("${integrationtest.imageValidation.imageWait.timeoutInMinutes:15}")
    private int imageWaitTimeoutInMinutes;

    @Value("${integrationtest.imageValidation.type:}")
    private ImageValidationType imageValidationType;

    @Value("${integrationtest.imageValidation.runAdditionalTests:false}")
    private boolean runAdditionalTests;

    public void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createSourceCatalogIfNotExistsAndValidateDefaultImage(testContext);
    }

    private void createDefaultUser(TestContext testContext) {
        testContext.as();
    }

    public void validateImageIdAndWriteToFile(TestContext testContext, ImageValidatorE2ETest e2ETest) {
        String imageId = isFreeIpaImageValidation() ? e2ETest.getFreeIpaImageId(testContext) : e2ETest.getCbImageId(testContext);
        if (Strings.isNotNullAndNotEmpty(imageId)) {
            if (testContext.getCurrentTestResult().getStatus() == ITestResult.SUCCESS) {
                String expectedImageUuid = getImageUuid();
                if (Strings.isNotNullAndNotEmpty(expectedImageUuid) && !expectedImageUuid.equalsIgnoreCase(imageId)) {
                    throw new RuntimeException("The test was successful but the image is not the expected one. Actual: " + imageId
                            + " Expected: " + expectedImageUuid);
                }
                LOGGER.info("The test was successful with this image: {}", imageId);
            }
            writeImageIdToFile(e2ETest, imageId);
        }
    }

    private void createSourceCatalogIfNotExistsAndValidateDefaultImage(TestContext testContext) {
        String imageUuid = getImageUuid();
        if (Strings.isNotNullAndNotEmpty(imageUuid) && !isFreeIpaImageValidation()) {
            createImageValidationSourceCatalog(testContext,
                    commonCloudProperties.getImageValidation().getSourceCatalogUrl(),
                    getImageCatalogName());

            try {
                Polling.waitPeriodly(IMAGE_WAIT_SLEEP_TIME_IN_SECONDS, TimeUnit.SECONDS)
                        .stopAfterDelay(imageWaitTimeoutInMinutes, TimeUnit.MINUTES)
                        .stopIfException(true)
                        .run(() -> containsImageAttempt(testContext));
            } catch (PollerStoppedException e) {
                String message = String.format("%s image is missing from the '%s' catalog.", imageUuid, testContext.get(ImageCatalogTestDto.class).getName());
                throw new TestFailException(message, e);
            }
        }
    }

    public boolean isImageValidation() {
        return imageValidationType != null;
    }

    private boolean isFreeIpaImageValidation() {
        return imageValidationType == ImageValidationType.FREEIPA;
    }

    public Optional<ImageV4Response> getImage(TestContext testContext) {
        ImagesV4Response imagesV4Response = getImages(testContext);
        List<ImageV4Response> images = new LinkedList<>();
        images.addAll(imagesV4Response.getBaseImages());
        images.addAll(imagesV4Response.getCdhImages());
        return images.stream()
                .filter(img -> img.getUuid().equalsIgnoreCase(getImageUuid()))
                .findFirst();
    }

    private AttemptResult<ImageV4Response> containsImageAttempt(TestContext testContext) {
        Optional<ImageV4Response> image = getImage(testContext);
        return image.isPresent()
                ? AttemptResults.finishWith(image.get())
                : AttemptResults.justContinue();
    }

    private ImagesV4Response getImages(TestContext testContext) {
        testContext
                .given(ImageCatalogTestDto.class)
                    .withName(getImageCatalogName())
                .when(imageCatalogTestClient.getV4WithAllImages())
                .validate();
        return testContext.get(ImageCatalogTestDto.class).getResponse().getImages();
    }

    public String getImageCatalogName() {
        return isFreeIpaImageValidation()
                ? commonCloudProperties.getImageValidation().getFreeIpaImageCatalog()
                : commonCloudProperties.getImageValidation().getSourceCatalogName();
    }

    public String getImageUuid() {
        return isFreeIpaImageValidation()
                ? commonCloudProperties.getImageValidation().getFreeIpaImageUuid()
                : commonCloudProperties.getImageValidation().getImageUuid();
    }

    private void createImageValidationSourceCatalog(TestContext testContext, String url, String name) {
        testContext
                .given(ImageCatalogTestDto.class)
                    .withUrl(url)
                    .withName(name)
                    .withoutCleanup()
                .when(imageCatalogTestClient.createIfNotExistV4())
                .validate();
    }

    private void writeImageIdToFile(ImageValidatorE2ETest e2ETest, String imageId) {
        try {
            File file = new File("ImageId-" + e2ETest.getClass().getSimpleName());
            FileUtils.writeStringToFile(file, "IMAGE_ID=" + imageId, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Writing image id to file failed: ", e);
        }
    }

    public ImageV4Response getUpgradeSourceImage(TestContext testContext) {
        List<ImageV4Response> images = getImages(testContext).getCdhImages();
        ImageV4Response imageUnderValidation = images.stream()
                .filter(img -> img.getUuid().equalsIgnoreCase(getImageUuid()))
                .findFirst()
                .orElseThrow();
        String runtimeVersion = shouldValidateWithSameRuntime(imageUnderValidation)
                ? imageUnderValidation.getVersion()
                : commonClusterManagerProperties.getUpgrade().getMatrix().get(imageUnderValidation.getVersion());
        if (runtimeVersion == null) {
            throw new TestFailException("Upgrade matrix entry is not defined for image version " + imageUnderValidation.getVersion());
        }
        Architecture architecture = Architecture.fromString(imageUnderValidation.getArchitecture());
        int cmBuildNumber = Integer.parseInt(imageUnderValidation.getCmBuildNumber());
        int stackBuildNumber = Integer.parseInt(imageUnderValidation.getStackDetails().getStackBuildNumber());
        return images.stream()
                .filter(img -> img.getCreated() < imageUnderValidation.getCreated())
                .filter(img -> Objects.equals(imageUnderValidation.getImageSetsByProvider().keySet(), img.getImageSetsByProvider().keySet()))
                .filter(img -> Objects.equals(imageUnderValidation.getOs(), img.getOs()))
                .filter(img -> Objects.equals(architecture, Architecture.fromString(img.getArchitecture())))
                .filter(img -> Objects.equals(runtimeVersion, img.getVersion()))
                .filter(img -> cmBuildNumber > Integer.parseInt(img.getCmBuildNumber()))
                .filter(img -> stackBuildNumber > Integer.parseInt(img.getStackDetails().getStackBuildNumber()))
                .max(Comparator.comparing(ImageV4Response::getCreated))
                .orElseThrow(() -> new TestFailException("No upgrade source image found for " + imageUnderValidation.getUuid()));
    }

    private boolean shouldValidateWithSameRuntime(ImageV4Response imageUnderValidation) {
        return isRhel8InitialVersion(imageUnderValidation);
    }

    private boolean isRhel8InitialVersion(ImageV4Response imageUnderValidation) {
        return OsType.RHEL8.getOs().equalsIgnoreCase(imageUnderValidation.getOs())
                && "7.2.17".equals(imageUnderValidation.getVersion());
    }

    public List<XmlSuite> getSuites() {
        XmlSuite suite = testNGUtil.createSuite("Image validation test suite");
        XmlTest basicTests = testNGUtil.createTest(suite, "Basic tests", true);

        if (imageValidationType == null) {
            throw new IllegalStateException("integrationtest.imageValidation.type must not be null");
        }
        switch (imageValidationType) {
            case BASE -> {
                if (CloudPlatform.YARN.equalsIgnoreCase(commonCloudProperties.getCloudProvider())) {
                    testNGUtil.addTestCase(basicTests, YarnImageValidatorE2ETest.class, "testHybridSDXWithBaseImage");
                } else {
                    testNGUtil.addTestCase(basicTests, BaseImageValidatorE2ETest.class, "testSDXWithBaseImage");
                }
            }
            case FREEIPA -> {
                addTestCaseIfNotAlreadyPassed(basicTests, PrewarmImageValidatorE2ETest.class, "testCreateInternalSdxAndDistrox");

                if (runAdditionalTests) {
                    XmlTest additionalTests = testNGUtil.createTest(suite, "Additional FreeIPA tests", false);
                    addTestCaseIfNotAlreadyPassed(additionalTests, DatalakeCcmUpgradeAndRotationTest.class, "testCcmV1Upgrade");
                    addTestCaseIfNotAlreadyPassed(additionalTests, BasicEnvironmentVirtualGroupTest.class, "testAddUsersToEnvironment");
                    addTestCaseIfNotAlreadyPassed(additionalTests, BasicEnvironmentVirtualGroupTest.class, "testAddGroupsToEnvironment");
                    addTestCaseIfNotAlreadyPassed(additionalTests, FreeIpaUpgradeTests.class, "testHAFreeIpaInstanceUpgrade");
                    addTestCaseIfNotAlreadyPassed(additionalTests, MonitoringTests.class, "testMonitoringOnEnvironment");
                }
            }
            case PREWARM -> {
                addTestCaseIfNotAlreadyPassed(basicTests, PrewarmImageValidatorE2ETest.class, "testCreateInternalSdxAndDistrox");

                if (runAdditionalTests) {
                    XmlTest additionalTests = testNGUtil.createTest(suite, "Additional runtime tests", false);
                    addTestCaseIfNotAlreadyPassed(additionalTests, DatalakeCcmUpgradeAndRotationTest.class, "testCcmV1Upgrade");
                    addTestCaseIfNotAlreadyPassed(additionalTests, ForceJavaVersionE2ETest.class, "testClusterProvisionWithForcedJavaVersion");
                    addTestCaseIfNotAlreadyPassed(additionalTests, SdxUpgradeTests.class, "testSDXUpgrade");
                    addTestCaseIfNotAlreadyPassed(additionalTests, MonitoringTests.class, "testMonitoringOnFreeIpaSdxDistrox");
                }
            }
            default -> throw new IllegalStateException("Unknown image validation type: " + imageValidationType);
        }
        return List.of(suite);
    }

    private void addTestCaseIfNotAlreadyPassed(XmlTest xmlTest, Class<?> testClass, String methodName) {
        if (!PassedTestsReporter.isAlreadyPassedTest(testClass, methodName)) {
            testNGUtil.addTestCase(xmlTest, testClass, methodName);
        }
    }
}

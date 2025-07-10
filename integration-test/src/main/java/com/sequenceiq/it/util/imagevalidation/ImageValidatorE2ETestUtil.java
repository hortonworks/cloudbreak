package com.sequenceiq.it.util.imagevalidation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.testng.ITestResult;
import org.testng.util.Strings;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImagesV4Response;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.it.cloudbreak.assertion.image.ImageAssertions;
import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.listener.PassedTestsReporter;
import com.sequenceiq.it.cloudbreak.testcase.e2e.freeipa.FreeIpaUpgradeTests;
import com.sequenceiq.it.cloudbreak.testcase.e2e.hybrid.BasicHybridCloudE2ETest;
import com.sequenceiq.it.cloudbreak.testcase.e2e.imagevalidation.BaseImageValidatorE2ETest;
import com.sequenceiq.it.cloudbreak.testcase.e2e.imagevalidation.PrewarmImageValidatorE2ETest;
import com.sequenceiq.it.cloudbreak.testcase.e2e.java.ForceJavaVersionE2ETest;
import com.sequenceiq.it.cloudbreak.testcase.e2e.l0promotion.BasicEnvironmentVirtualGroupTest;
import com.sequenceiq.it.cloudbreak.testcase.e2e.l0promotion.DatalakeCcmUpgradeAndRotationTest;
import com.sequenceiq.it.cloudbreak.testcase.e2e.l0promotion.MonitoringTests;
import com.sequenceiq.it.cloudbreak.testcase.e2e.sdx.SdxUpgradeTests;
import com.sequenceiq.it.util.TestNGUtil;

@Component
public class ImageValidatorE2ETestUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageValidatorE2ETestUtil.class);

    private static final Map<String, Class<?>> ALL_TESTS_FOR_FREEIPA = Map.of(
            "testCcmV1Upgrade", DatalakeCcmUpgradeAndRotationTest.class,
            "testAddUsersToEnvironment", BasicEnvironmentVirtualGroupTest.class,
            "testAddGroupsToEnvironment", BasicEnvironmentVirtualGroupTest.class,
            "testHAFreeIpaInstanceUpgrade", FreeIpaUpgradeTests.class,
            "testMonitoringOnEnvironment", MonitoringTests.class
    );

    private static final Map<String, Class<?>> ALL_TESTS_FOR_RUNTIME = Map.of(
            "testCcmV1Upgrade", DatalakeCcmUpgradeAndRotationTest.class,
            "testClusterProvisionWithForcedJavaVersion", ForceJavaVersionE2ETest.class,
            "testSDXUpgrade", SdxUpgradeTests.class,
            "testMonitoringOnFreeIpaSdxDistrox", MonitoringTests.class
    );

    @Inject
    private CommonCloudProperties commonCloudProperties;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Inject
    private ImageCatalogTestClient imageCatalogTestClient;

    @Inject
    private TestNGUtil testNGUtil;

    @Inject
    private ImageAssertions imageAssertions;

    @Value("${integrationtest.imageValidation.type:}")
    private ImageValidationType imageValidationType;

    @Value("${integrationtest.imageValidation.runAdditionalTests:false}")
    private String runAdditionalTests;

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
        }
    }

    public boolean isImageValidation() {
        return imageValidationType != null;
    }

    public boolean isFreeIpaImageValidation() {
        return imageValidationType == ImageValidationType.FREEIPA;
    }

    public Optional<ImageV4Response> getImageUnderValidation(TestContext testContext) {

        final String imageUuid = getImageUuid();
        ImagesV4Response imagesV4Response = null;
        if (isFreeIpaImageValidation()) {

            try {
                RestTemplate restTemplate = new RestTemplate();
                //Sadly for FreeIPA we have a URL, not a catalog name!
                String url = getImageCatalogName();
                String json = restTemplate.getForObject(url, String.class);

                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(json);
                JsonNode images = root.path("images").path("freeipa-images");
                for (JsonNode image : images) {
                    if (imageUuid.equals(image.path("uuid").asText())) {
                        ImageV4Response response = new ImageV4Response();
                        response.setDefaultImage(true);
                        response.setCreated(image.path("created").asLong());
                        response.setPublished(image.path("published").asLong());
                        response.setDate(image.path("date").asText());
                        response.setDescription(image.path("description").asText());
                        response.setOs(image.path("os").asText());
                        response.setOsType(image.path("os_type").asText());
                        response.setArchitecture(image.path("architecture").asText());
                        response.setUuid(image.path("uuid").asText());
                        return Optional.of(response);
                    }
                }
                return Optional.empty();
            } catch (Exception e) {
                throw new TestFailException("Failed to query FreeIPA image for validation!");
            }

        } else {
            imagesV4Response = getImages(testContext);
            List<ImageV4Response> images = new LinkedList<>();
            images.addAll(imagesV4Response.getBaseImages());
            images.addAll(imagesV4Response.getCdhImages());
            return images.stream()
                    .filter(img -> img.getUuid().equalsIgnoreCase(imageUuid))
                    .findFirst();
        }
    }

    private ImagesV4Response getImages(TestContext testContext) {
        return getImages(testContext, getImageCatalogName());
    }

    private ImagesV4Response getImages(TestContext testContext, String catalogName) {
        testContext
                .given(ImageCatalogTestDto.class)
                    .withName(catalogName)
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

    public Architecture getArchitecture(TestContext testContext) {
        Optional<ImageV4Response> imageUnderValidation = getImageUnderValidation(testContext);
        String architecture = imageUnderValidation.isPresent() ? imageUnderValidation.get().getArchitecture() : null;
        return Architecture.fromStringWithFallback(architecture);
    }

    private void createImageValidationSourceCatalog(TestContext testContext, String url, String name) {
        testContext
                .given(ImageCatalogTestDto.class)
                    .withUrl(url)
                    .withName(name)
                    .withoutCleanup()
                .when(imageCatalogTestClient.createIfNotExistV4())
                .then(imageAssertions.validateContainsImage(getImageUuid()))
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
        ImageV4Response imageUnderValidation = getImageUnderValidation(testContext).orElseThrow();
        String runtimeVersion = shouldValidateWithSameRuntime(imageUnderValidation)
                ? imageUnderValidation.getVersion()
                : commonClusterManagerProperties.getUpgrade().getMatrix().get(imageUnderValidation.getVersion());
        if (runtimeVersion == null) {
            throw new TestFailException("Upgrade matrix entry is not defined for image version " + imageUnderValidation.getVersion());
        }
        Architecture architecture = Architecture.fromStringWithFallback(imageUnderValidation.getArchitecture());
        int cmBuildNumber = Integer.parseInt(imageUnderValidation.getCmBuildNumber());
        int stackBuildNumber = Integer.parseInt(imageUnderValidation.getStackDetails().getStackBuildNumber());
        return getImages(testContext, "cdp-default").getCdhImages().stream()
                .filter(img -> img.getCreated() < imageUnderValidation.getCreated())
                .filter(img -> Objects.equals(imageUnderValidation.getImageSetsByProvider().keySet(), img.getImageSetsByProvider().keySet()))
                .filter(img -> Objects.equals(imageUnderValidation.getOs(), img.getOs()))
                .filter(img -> Objects.equals(architecture, Architecture.fromStringWithFallback(img.getArchitecture())))
                .filter(img -> Objects.equals(runtimeVersion, img.getVersion()))
                .filter(img -> cmBuildNumber > Integer.parseInt(img.getCmBuildNumber()))
                .filter(img -> stackBuildNumber > Integer.parseInt(img.getStackDetails().getStackBuildNumber()))
                .max(Comparator.comparing(ImageV4Response::getCreated))
                .orElseThrow(() -> new TestFailException("No upgrade source image found for " + imageUnderValidation.getUuid()));
    }

    private boolean shouldValidateWithSameRuntime(ImageV4Response imageUnderValidation) {
        return isRhel8InitialVersion(imageUnderValidation) || isArm64InitialVersion(imageUnderValidation);
    }

    private boolean isRhel8InitialVersion(ImageV4Response imageUnderValidation) {
        return OsType.RHEL8.getOs().equalsIgnoreCase(imageUnderValidation.getOs())
                && "7.2.17".equals(imageUnderValidation.getVersion());
    }

    private boolean isArm64InitialVersion(ImageV4Response imageUnderValidation) {
        return Architecture.ARM64.equals(Architecture.fromStringWithFallback(imageUnderValidation.getArchitecture()))
                && "7.3.1".equals(imageUnderValidation.getVersion());
    }

    public List<XmlSuite> getSuites() {
        XmlSuite suite = testNGUtil.createSuite("Image validation test suite");
        XmlTest basicTests = testNGUtil.createTest(suite, "Basic tests", true);

        if (imageValidationType == null) {
            throw new IllegalStateException("integrationtest.imageValidation.type must not be null");
        }
        switch (imageValidationType) {
            case BASE -> {
                if (commonCloudProperties.isYcloudTest()) {
                    testNGUtil.addTestCase(basicTests, BasicHybridCloudE2ETest.class, "testHybridSdx");
                } else {
                    testNGUtil.addTestCase(basicTests, BaseImageValidatorE2ETest.class, "testProvisioningWithBaseImage");
                }
            }
            case FREEIPA -> {
                addTestCase(basicTests);
                if (!"none".equalsIgnoreCase(runAdditionalTests)) {
                    XmlTest additionalTests = testNGUtil.createTest(suite, "Additional FreeIPA tests", false);
                    ALL_TESTS_FOR_FREEIPA.forEach((k, v) -> {
                        addTestCaseIfNotPassedOrSkipped(additionalTests, v, k, runAdditionalTests);
                    });
                }
            }
            case PREWARM -> {
                addTestCase(basicTests);
                if (!"none".equalsIgnoreCase(runAdditionalTests)) {
                    XmlTest additionalTests = testNGUtil.createTest(suite, "Additional Runtime tests", false);
                    ALL_TESTS_FOR_RUNTIME.forEach((k, v) -> {
                        addTestCaseIfNotPassedOrSkipped(additionalTests, v, k, runAdditionalTests);
                    });
                }
            }
            default -> throw new IllegalStateException("Unknown image validation type: " + imageValidationType);
        }
        return List.of(suite);
    }

    private void addTestCase(XmlTest xmlTest) {
        addTestCaseIfNotPassedOrSkipped(xmlTest, PrewarmImageValidatorE2ETest.class, "testCreateInternalSdxAndDistrox", null);
    }

    private void addTestCaseIfNotPassedOrSkipped(XmlTest xmlTest, Class<?> testClass, String methodName, String runAdditionalTests) {
        if (runAdditionalTests != null && (runAdditionalTests.contains("-" + methodName) || "none".equalsIgnoreCase(runAdditionalTests))) {
            LOGGER.debug("Skipping ignored test case {}::{}", testClass.getSimpleName(), methodName);
            return;
        }
        if (!PassedTestsReporter.isAlreadyPassedTest(testClass, methodName)) {
            LOGGER.info("Adding test case {}::{}", testClass.getSimpleName(), methodName);
            testNGUtil.addTestCase(xmlTest, testClass, methodName);
        } else {
            LOGGER.debug("Skipping already passed test case {}::{}", testClass.getSimpleName(), methodName);
        }
    }
}

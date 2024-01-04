package com.sequenceiq.it.util.imagevalidation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.testng.ITestResult;
import org.testng.util.Strings;

import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;

@Component
public class ImageValidatorE2ETestUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageValidatorE2ETestUtil.class);

    @Inject
    private CommonCloudProperties commonCloudProperties;

    @Inject
    private ImageCatalogTestClient imageCatalogTestClient;

    public void setupTest(TestContext testContext, ImageValidatorE2ETest e2ETest) {
        createDefaultUser(testContext);
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createSourceCatalogIfNotExistsAndValidateDefaultImage(testContext, e2ETest);
    }

    private void createDefaultUser(TestContext testContext) {
        testContext.as();
    }

    public void validateImageIdAndWriteToFile(TestContext testContext, ImageValidatorE2ETest e2ETest) {
        String imageId = e2ETest.getImageId(testContext);
        if (testContext.getCurrentTestResult().getStatus() == ITestResult.SUCCESS) {
            String expectedImageUuid = getImageUuid();
            if (Strings.isNotNullAndNotEmpty(expectedImageUuid) && !expectedImageUuid.equalsIgnoreCase(imageId)) {
                throw new RuntimeException("The test was successful but the image is not the expected one. Actual: " + imageId
                        + " Expected: " + expectedImageUuid);
            }
            LOGGER.info("The test was successful with this image:  " + imageId);
        }
        writeImageIdToFile(e2ETest, imageId);
    }

    public void createSourceCatalogIfNotExistsAndValidateDefaultImage(TestContext testContext, ImageValidatorE2ETest e2ETest) {
        createImageValidationSourceCatalog(testContext,
                commonCloudProperties.getImageValidation().getSourceCatalogUrl(),
                getImageCatalogName());

        String imageUuid = getImageUuid();
        if (Strings.isNotNullAndNotEmpty(imageUuid)) {
            if (e2ETest.isPrewarmedImageTest()) {
                validatePrewarmedImage(testContext, imageUuid);
            } else {
                validateBaseImage(testContext, imageUuid);
            }
        }
    }

    public String getImageCatalogName() {
        return commonCloudProperties.getImageValidation().getSourceCatalogName();
    }

    public String getImageUuid() {
        return commonCloudProperties.getImageValidation().getImageUuid();
    }

    private void validatePrewarmedImage(TestContext testContext, String imageUuid) {
        testContext.given(ImageCatalogTestDto.class)
                .when(imageCatalogTestClient.getV4(true))
                .validate();
        ImageCatalogTestDto dto = testContext.get(ImageCatalogTestDto.class);
        dto.getResponse().getImages().getCdhImages().stream()
                .filter(img -> img.getUuid().equalsIgnoreCase(imageUuid))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(imageUuid + " prewarmed image is missing from the '" + dto.getName() + "' catalog."));
    }

    private void validateBaseImage(TestContext testContext, String imageUuid) {
        testContext.given(ImageCatalogTestDto.class)
                .when(imageCatalogTestClient.getV4(true))
                .validate();
        ImageCatalogTestDto dto = testContext.get(ImageCatalogTestDto.class);
        dto.getResponse().getImages().getBaseImages().stream()
                .filter(img -> img.getUuid().equalsIgnoreCase(imageUuid))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(imageUuid + " base image is missing from the '" + dto.getName() + "' catalog."));
    }

    private void createImageValidationSourceCatalog(TestContext testContext, String url, String name) {
        testContext.given(ImageCatalogTestDto.class)
                .withUrl(url)
                .withName(name)
                .withoutCleanup()
                .when(imageCatalogTestClient.createIfNotExistV4());
    }

    private void writeImageIdToFile(ImageValidatorE2ETest e2ETest, String imageId) {
        try {
            File file = new File("ImageId-" + e2ETest.getClass().getSimpleName());
            FileUtils.writeStringToFile(file, "IMAGE_ID=" + imageId, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Writing image id to file failed: ", e);
        }
    }
}

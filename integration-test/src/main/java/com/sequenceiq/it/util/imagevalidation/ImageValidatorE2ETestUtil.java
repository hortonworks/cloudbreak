package com.sequenceiq.it.util.imagevalidation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.testng.ITestResult;
import org.testng.util.Strings;

import com.dyngr.Polling;
import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImagesV4Response;
import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

@Component
public class ImageValidatorE2ETestUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageValidatorE2ETestUtil.class);

    private static final int IMAGE_WAIT_SLEEP_TIME_IN_SECONDS = 10;

    @Inject
    private CommonCloudProperties commonCloudProperties;

    @Inject
    private ImageCatalogTestClient imageCatalogTestClient;

    @Value("${integrationtest.imageValidation.imageWait.timeoutInMinutes:15}")
    private int imageWaitTimeoutInMinutes;

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
            try {
                Polling.waitPeriodly(IMAGE_WAIT_SLEEP_TIME_IN_SECONDS, TimeUnit.SECONDS)
                        .stopAfterDelay(imageWaitTimeoutInMinutes, TimeUnit.MINUTES)
                        .stopIfException(true)
                        .run(getImageValidateAttemptMaker(testContext, e2ETest, imageUuid));
            } catch (PollerStoppedException e) {
                String message = String.format("%s %s image is missing from the '%s' catalog.",
                        imageUuid, e2ETest.isPrewarmedImageTest() ? "prewarmed" : "base", testContext.get(ImageCatalogTestDto.class).getName());
                throw new TestFailException(message, e);
            }
        }
    }

    private AttemptMaker<Void> getImageValidateAttemptMaker(TestContext testContext, ImageValidatorE2ETest e2ETest, String imageUuid) {
        return e2ETest.isPrewarmedImageTest()
                ? () -> containsImageAttempt(getImages(testContext).getCdhImages(), imageUuid)
                : () -> containsImageAttempt(getImages(testContext).getBaseImages(), imageUuid);
    }

    private ImagesV4Response getImages(TestContext testContext) {
        testContext.given(ImageCatalogTestDto.class)
                .when(imageCatalogTestClient.getV4(true))
                .validate();
        return testContext.get(ImageCatalogTestDto.class).getResponse().getImages();
    }

    private <T extends ImageV4Response> AttemptResult<Void> containsImageAttempt(List<T> images, String imageUuid) {
        return images.stream().anyMatch(img -> img.getUuid().equalsIgnoreCase(imageUuid))
                ? AttemptResults.justFinish()
                : AttemptResults.justContinue();
    }

    public String getImageCatalogName() {
        return commonCloudProperties.getImageValidation().getSourceCatalogName();
    }

    public String getImageUuid() {
        return commonCloudProperties.getImageValidation().getImageUuid();
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

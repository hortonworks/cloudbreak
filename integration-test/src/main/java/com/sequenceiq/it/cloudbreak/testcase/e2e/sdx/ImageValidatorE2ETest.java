package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.util.Strings;

import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;

public abstract class ImageValidatorE2ETest extends AbstractE2ETest {

    private static final Logger LOG = LoggerFactory.getLogger(ImageValidatorE2ETest.class);

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        createSourceCatalogIfNotExistsAndValidateDefaultImage(testContext);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
        createEnvironmentWithNetworkAndFreeIpa(testContext);
    }

    @AfterMethod
    public void validateImageIdAndWriteToFile(Object[] data, ITestResult result) {
        TestContext testContext = (TestContext) data[0];
        if (result.getStatus() == ITestResult.SUCCESS) {
            String expectedImageUuid = commonCloudProperties().getImageValidation().getExpectedDefaultImageUuid();
            String actualImageUuid = getImageId(testContext);
            if (Strings.isNotNullAndNotEmpty(expectedImageUuid) && !expectedImageUuid.equalsIgnoreCase(actualImageUuid)) {
                throw new RuntimeException("The test was successful but the image is not the expected one. Actual: " + actualImageUuid
                        + " Expected: " + expectedImageUuid);
            }
            LOG.info("The test was successful with this image:  " + actualImageUuid);
            writeImageIdToFile(testContext);
        }
    }

    protected abstract String getImageId(TestContext testContext);

    private void createSourceCatalogIfNotExistsAndValidateDefaultImage(TestContext testContext) {
        createImageValidationSourceCatalog(testContext,
                commonCloudProperties().getImageValidation().getSourceCatalogUrl(),
                commonCloudProperties().getImageValidation().getSourceCatalogName());

        String imageUuid = commonCloudProperties().getImageValidation().getExpectedDefaultImageUuid();
        if (Strings.isNotNullAndNotEmpty(imageUuid)) {
            validateDefaultImage(testContext, imageUuid);
        }
    }

    private void writeImageIdToFile(TestContext testContext) {
        try {
            File file = new File("ImageId-" + getClass().getSimpleName());
            FileUtils.writeStringToFile(file, "IMAGE_ID=" + getImageId(testContext), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.error("Writing image id to file failed: ", e);
        }
    }

}

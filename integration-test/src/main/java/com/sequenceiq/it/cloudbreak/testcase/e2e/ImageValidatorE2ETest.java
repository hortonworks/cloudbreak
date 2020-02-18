package com.sequenceiq.it.cloudbreak.testcase.e2e;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import com.sequenceiq.it.cloudbreak.context.TestContext;

public abstract class ImageValidatorE2ETest extends AbstractE2ETest {

    private static final Logger LOG = LoggerFactory.getLogger(ImageValidatorE2ETest.class);

    @Value("${integrationtest.imageValidation.sdxTemplateName}")
    protected String sdxTemplateName;

    @Value("${integrationtest.imageValidation.distroxTemplateName}")
    protected String distroxTemplateName;

    @Value("${integrationtest.imageValidation.runtimeVersion}")
    protected String runtimeVersion;

    @Value("${integrationtest.imageValidation.sourceCatalogName}")
    private String sourceImageCatalogName;

    @Value("${integrationtest.imageValidation.sourceCatalogUrl}")
    private String sourceImageCatalogUrl;

    @BeforeMethod
    public void createSourceCatalogIfNotExists(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        createImageValidationSourceCatalog(testContext, sourceImageCatalogUrl, sourceImageCatalogName);
    }

    @AfterMethod
    public void setImageId(Object[] data, ITestResult result) {
        TestContext testContext = (TestContext) data[0];
        if (result.getStatus() == ITestResult.SUCCESS) {
            LOG.info("The test was successful with this image:  " + getImageId(testContext));
            writeImageIdToFile(testContext);
        }
    }

    protected abstract String getImageId(TestContext testContext);

    private void writeImageIdToFile(TestContext testContext) {
        try {
            File file = new File("ImageId-" + getClass().getSimpleName());
            FileUtils.writeStringToFile(file, "IMAGE_ID=" + getImageId(testContext), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.error("Writing image id to file failed: ", e);
        }
    }

}

package com.sequenceiq.it;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.util.CollectionUtils;
import org.testng.ITestContext;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import com.sequenceiq.cloudbreak.api.endpoint.v1.ImageCatalogV1Endpoint;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImageCatalogRequest;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImageCatalogResponse;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.v2.mock.SuiteInitializerMock;
import com.sequenceiq.it.config.IntegrationTestConfiguration;
import com.sequenceiq.it.util.CleanupService;

@ContextConfiguration(classes = IntegrationTestConfiguration.class, initializers = ConfigFileApplicationContextInitializer.class)
public class MockSuiteInitializer extends AbstractTestNGSpringContextTests {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockSuiteInitializer.class);

    private static final String MOCK_IMAGE_CATALOG_NAME = "mock-image-catalog-name";

    @Value("${mock.image.catalog.url:https://localhost:9443/imagecatalog}")
    private String imageCatalogUrl;

    @Value("${integrationtest.testsuite.cleanUpOnFailure}")
    private boolean cleanUpOnFailure;

    @Value("${integrationtest.cleanup.cleanupBeforeStart}")
    private boolean cleanUpBeforeStart;

    @Inject
    private CleanupService cleanUpService;

    @Inject
    private SuiteContext suiteContext;

    private IntegrationTestContext itContext;

    @BeforeSuite(dependsOnGroups = "suiteInit")
    public void initContext(ITestContext testContext) throws Exception {
        // Workaround of https://jira.spring.io/browse/SPR-4072
        springTestContextBeforeTestClass();
        springTestContextPrepareTestInstance();

        itContext = suiteContext.getItContext(testContext.getSuite().getName());
    }

    @BeforeSuite(dependsOnMethods = "initContext")
    @Parameters("mockPort")
    public void initSuiteMap(ITestContext testContext, @Optional("9443") int mockPort) {
        CloudbreakClient cloudbreakClient = itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT, CloudbreakClient.class);

        if (cleanUpBeforeStart && isImageCatalogExists(cloudbreakClient.imageCatalogEndpoint(), MOCK_IMAGE_CATALOG_NAME)) {
            cleanUpService.deleteImageCatalog(cloudbreakClient, MOCK_IMAGE_CATALOG_NAME);
        }

        if (!isImageCatalogExists(cloudbreakClient.imageCatalogEndpoint(), MOCK_IMAGE_CATALOG_NAME)) {
            SuiteInitializerMock suiteInitializerMock = (SuiteInitializerMock) applicationContext.getBean(SuiteInitializerMock.NAME, mockPort);
            suiteInitializerMock.mockImageCatalogResponse(itContext);

            createMockImageCatalog(cloudbreakClient.imageCatalogEndpoint());

            suiteInitializerMock.stop();
        }
    }

    private boolean isImageCatalogExists(ImageCatalogV1Endpoint endpoint, String mockImageCatalogName) {
        try {
            return endpoint.getByName(mockImageCatalogName, false) != null;
        } catch (ForbiddenException e) {
            return false;
        }
    }

    private void createMockImageCatalog(ImageCatalogV1Endpoint endpoint) {
        ImageCatalogRequest imageCatalogRequest = new ImageCatalogRequest();
        imageCatalogRequest.setName(MOCK_IMAGE_CATALOG_NAME);
        imageCatalogRequest.setUrl(imageCatalogUrl);
        ImageCatalogResponse imageCatalogResponse = endpoint.postPublic(imageCatalogRequest);

        if (imageCatalogResponse == null) {
            throw new IllegalArgumentException("ImageCatalog creation failed.");
        }

        endpoint.putSetDefaultByName(MOCK_IMAGE_CATALOG_NAME);
    }

    @AfterSuite(alwaysRun = true)
    @Parameters("cleanUp")
    public void cleanUp(@Optional("true") boolean cleanUp) {
        if (isCleanUpNeeded(cleanUp)) {
            CloudbreakClient cloudbreakClient = itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT, CloudbreakClient.class);
            cleanUpService.deleteImageCatalog(cloudbreakClient, MOCK_IMAGE_CATALOG_NAME);
        }
    }

    private boolean isCleanUpNeeded(boolean cleanUp) {
        boolean noTestsFailed = CollectionUtils.isEmpty(itContext.getContextParam(CloudbreakITContextConstants.FAILED_TESTS, List.class));
        return cleanUp && (cleanUpOnFailure || noTestsFailed);
    }

}

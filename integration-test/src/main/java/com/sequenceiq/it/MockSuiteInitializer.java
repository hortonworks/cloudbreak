package com.sequenceiq.it;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;

import org.apache.commons.lang3.StringUtils;
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

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.ImageCatalogV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests.ImageCatalogV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageCatalogV4Response;
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
        Long workspaceId = itContext.getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);

        if (cleanUpBeforeStart && isImageCatalogExists(cloudbreakClient.imageCatalogV4Endpoint(), MOCK_IMAGE_CATALOG_NAME, workspaceId)) {
            cleanUpService.deleteImageCatalog(cloudbreakClient, MOCK_IMAGE_CATALOG_NAME, workspaceId);
        }

        if (!isImageCatalogExists(cloudbreakClient.imageCatalogV4Endpoint(), MOCK_IMAGE_CATALOG_NAME, workspaceId)) {
            SuiteInitializerMock suiteInitializerMock = (SuiteInitializerMock) applicationContext.getBean(SuiteInitializerMock.NAME, mockPort);
            suiteInitializerMock.mockImageCatalogResponse(itContext);

            createMockImageCatalog(cloudbreakClient.imageCatalogV4Endpoint(), workspaceId);

            suiteInitializerMock.stop();
        }
    }

    private boolean isImageCatalogExists(ImageCatalogV4Endpoint endpoint, String mockImageCatalogName, Long workspaceId) {
        try {
            return endpoint.list(workspaceId)
                    .getResponses()
                    .stream()
                    .anyMatch(imageCatalog -> StringUtils.equals(imageCatalog.getName(), mockImageCatalogName));
        } catch (ForbiddenException e) {
            return false;
        }
    }

    private void createMockImageCatalog(ImageCatalogV4Endpoint endpoint, Long workspaceId) {
        ImageCatalogV4Request imageCatalogRequest = new ImageCatalogV4Request();
        imageCatalogRequest.setName(MOCK_IMAGE_CATALOG_NAME);
        imageCatalogRequest.setUrl(imageCatalogUrl);
        ImageCatalogV4Response imageCatalogResponse = endpoint.create(workspaceId, imageCatalogRequest);

        if (imageCatalogResponse == null) {
            throw new IllegalArgumentException("ImageCatalog creation failed.");
        }

        endpoint.setDefault(workspaceId, MOCK_IMAGE_CATALOG_NAME);
    }

    @AfterSuite(alwaysRun = true)
    @Parameters("cleanUp")
    public void cleanUp(@Optional("true") boolean cleanUp) {
        if (isCleanUpNeeded(cleanUp)) {
            CloudbreakClient cloudbreakClient = itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT, CloudbreakClient.class);
            Long workspaceId = itContext.getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
            cleanUpService.deleteImageCatalog(cloudbreakClient, MOCK_IMAGE_CATALOG_NAME, workspaceId);
        }
    }

    private boolean isCleanUpNeeded(boolean cleanUp) {
        boolean noTestsFailed = CollectionUtils.isEmpty(itContext.getContextParam(CloudbreakITContextConstants.FAILED_TESTS, List.class));
        return cleanUp && (cleanUpOnFailure || noTestsFailed);
    }

}
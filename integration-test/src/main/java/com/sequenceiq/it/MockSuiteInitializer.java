package com.sequenceiq.it;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.ITestContext;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.ImageCatalogV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.filter.GetImageCatalogV4Filter;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests.ImageCatalogV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests.UpdateImageCatalogV4Request;
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
    public void initSuiteMap(ITestContext testContext, @Optional("9443") int mockPort) throws Exception {
        CloudbreakClient cloudbreakClient = itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        com.sequenceiq.it.cloudbreak.newway.CloudbreakClient clientContext =
                com.sequenceiq.it.cloudbreak.newway.CloudbreakClient.getTestContextCloudbreakClient().apply(itContext);

        if (!isImageCatalogExists(clientContext.getWorkspaceId(), cloudbreakClient.imageCatalogV4Endpoint(), MOCK_IMAGE_CATALOG_NAME)) {
            SuiteInitializerMock suiteInitializerMock = (SuiteInitializerMock) applicationContext.getBean(SuiteInitializerMock.NAME, mockPort);
            suiteInitializerMock.mockImageCatalogResponse(itContext);

            createMockImageCatalog(clientContext.getWorkspaceId(), cloudbreakClient.imageCatalogV4Endpoint());

            suiteInitializerMock.stop();
        }
    }

    private boolean isImageCatalogExists(Long workspaceId, ImageCatalogV4Endpoint endpoint, String mockImageCatalogName) {
        try {
            GetImageCatalogV4Filter filter = new GetImageCatalogV4Filter();
            filter.setWithImages(false);
            return endpoint.get(workspaceId, mockImageCatalogName, filter) != null;
        } catch (ForbiddenException e) {
            return false;
        }
    }

    private void createMockImageCatalog(Long workspaceId, ImageCatalogV4Endpoint endpoint) throws Exception {
        ImageCatalogV4Request imageCatalogRequest = new ImageCatalogV4Request();
        imageCatalogRequest.setName(MOCK_IMAGE_CATALOG_NAME);
        imageCatalogRequest.setUrl(imageCatalogUrl);
        ImageCatalogV4Response imageCatalogResponse = endpoint.create(workspaceId, imageCatalogRequest);

        if (imageCatalogResponse == null) {
            throw new IllegalArgumentException("ImageCatalog creation failed.");
        }

        UpdateImageCatalogV4Request updateImageCatalogV4Request = new UpdateImageCatalogV4Request();
        updateImageCatalogV4Request.setName(MOCK_IMAGE_CATALOG_NAME);
        updateImageCatalogV4Request.setUrl(imageCatalogUrl);
        endpoint.update(workspaceId, updateImageCatalogV4Request);
    }

}

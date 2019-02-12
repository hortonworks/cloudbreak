package com.sequenceiq.it.cloudbreak.newway;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.xml.XmlSuite;

import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProviderHelper;
import com.sequenceiq.it.cloudbreak.newway.cloud.MockCloudProvider;

public class CloudbreakMockClusterTest extends CloudbreakTest {
    private static final String VALID_IMAGECATALOG_NAME = "mock-image-catalog";

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakMockClusterTest.class);

    private static final String WARNING_TEXT = "Following variables must be set whether as environment variables or (test) application.yaml: "
            + "INTEGRATIONTEST_CLOUDBREAK_SERVER INTEGRATIONTEST_UAA_SERVER INTEGRATIONTEST_UAA_USER INTEGRATIONTEST_UAA_PASSWORD";

    private CloudProvider mockProvider;

    public CloudbreakMockClusterTest() {
        mockProvider = CloudProviderHelper.providerFactory(MockCloudProvider.MOCK, getTestParameter());
    }

    public CloudProvider getMockProvider() {
        return mockProvider;
    }

    @BeforeSuite
    public void checkNecessaryArguments() {
        Objects.requireNonNull(getTestParameter().get("INTEGRATIONTEST_CLOUDBREAK_SERVER"), WARNING_TEXT);
        Objects.requireNonNull(getTestParameter().get("INTEGRATIONTEST_UAA_SERVER"), WARNING_TEXT);
        Objects.requireNonNull(getTestParameter().get("INTEGRATIONTEST_UAA_USER"), WARNING_TEXT);
        Objects.requireNonNull(getTestParameter().get("INTEGRATIONTEST_UAA_PASSWORD"), WARNING_TEXT);
    }

    @BeforeSuite
    public void startAndSetImageCatalog(ITestContext context) throws Exception {

        mockSetup(context);

        try {
            deleteImageCatalog();
        } catch (Exception e) {
            LOGGER.info("Could not remove image catalog, possibly it is not exist", e);
        }

        given(CloudbreakClient.created());
//        given(Mock.imageCatalogServiceIsStarted());
//        given(ImageCatalog.request()
//                .withName(VALID_IMAGECATALOG_NAME), "an imagecatalog request and set as default"
//        );
//        when(ImageCatalog.post());
//        when(ImageCatalog.setDefault());
//        then(ImageCatalog.assertThis(
//                (imageCatalog, t) -> {
//                    Assert.assertEquals(imageCatalog.getResponse().getName(), VALID_IMAGECATALOG_NAME);
//                    Assert.assertEquals(imageCatalog.getResponse().isUsedAsDefault(), true);
//                }), "check imagecatalog is created and set as default");
    }

    private void mockSetup(ITestContext context) {
        XmlSuite.ParallelMode p = context.getSuite().getXmlSuite().getParallel();
        int threadCount = 1;
        String hostName = getTestParameter().get("mock.server.address");
        if (p != XmlSuite.ParallelMode.FALSE) {
            threadCount = context.getSuite().getXmlSuite().getThreadCount();
        }
        Mock.setup(hostName, threadCount);
    }

    @BeforeClass
    public void prepare() throws Exception {
        given(Mock.isCreated());
        given(CloudbreakClient.created());
        given(getMockProvider().aValidCredential());
    }

    @AfterClass(alwaysRun = true)
    public void cleanMockRoutesAndMockedStacks() throws Exception {
        when(Mock.deleteStack());
        when(Mock.delete());
        //when(Mock.deleteCredential());
    }

    @AfterSuite(alwaysRun = true)
    public void stopUnsetImageCatalogAndStopMock() throws Exception {
        deleteImageCatalog();
        Mock.shutdown();
    }

    private void deleteImageCatalog() throws Exception {
        given(CloudbreakClient.created());
//        given(ImageCatalog.request()
//                .withName(VALID_IMAGECATALOG_NAME), "an imagecatalog request and set as default"
//        );
//        when(ImageCatalog.delete());
    }
}

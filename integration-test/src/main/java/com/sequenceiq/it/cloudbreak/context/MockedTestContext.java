package com.sequenceiq.it.cloudbreak.context;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.mock.DefaultModel;
import com.sequenceiq.it.cloudbreak.mock.ImageCatalogMockServerSetup;
import com.sequenceiq.it.cloudbreak.mock.ThreadLocalProfiles;
import com.sequenceiq.it.cloudbreak.spark.DynamicRouteStack;
import com.sequenceiq.it.cloudbreak.spark.SparkServer;
import com.sequenceiq.it.cloudbreak.spark.SparkServerFactory;

@Prototype
public class MockedTestContext extends TestContext implements AutoCloseable, MockTestContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockedTestContext.class);

    @Value("${mock.server.address:localhost}")
    private String mockServerAddress;

    @Inject
    private SparkServerFactory sparkServerFactory;

    @Inject
    private ImageCatalogMockServerSetup imageCatalogMockServerSetup;

    @Inject
    private ApplicationContext applicationContext;

    private SparkServer sparkServer;

    private DefaultModel model;

    @PostConstruct
    void init() throws InterruptedException {
        MockedTestContext.LOGGER.info("Creating mocked TestContext");
        sparkServer = sparkServerFactory.construct();
        MockedTestContext.LOGGER.info("MockedTestContext got spark server: {}", sparkServer);
        imageCatalogMockServerSetup.configureImgCatalogWithExistingSparkServer(sparkServer);
        model = new DefaultModel();
        model.startModel(sparkServer.getSparkService(), mockServerAddress, ThreadLocalProfiles.getActiveProfiles());
    }

    @Override
    public DefaultModel getModel() {
        return model;
    }

    @Override
    public SparkServer getSparkServer() {
        return sparkServer;
    }

    @Override
    public ImageCatalogMockServerSetup getImageCatalogMockServerSetup() {
        return imageCatalogMockServerSetup;
    }

    @Override
    public DynamicRouteStack dynamicRouteStack() {
        return model.getAmbariMock().getDynamicRouteStack();
    }

    @Override
    public void cleanupTestContext() {
        LOGGER.info("MockedTestContext cleaned up. {}", sparkServer);
        super.cleanupTestContext();
        sparkServerFactory.release(sparkServer);
        setShutdown(true);
    }

    @Override
    public void close() {
        LOGGER.info("MockedTestContext closed. {}", sparkServer);
        sparkServerFactory.release(sparkServer);
        setShutdown(true);
    }
}

package com.sequenceiq.it.cloudbreak.context;

import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.mock.DefaultModel;
import com.sequenceiq.it.cloudbreak.mock.ImageCatalogMockServerSetup;
import com.sequenceiq.it.cloudbreak.mock.ThreadLocalProfiles;
import com.sequenceiq.it.cloudbreak.spark.DynamicRouteStack;
import com.sequenceiq.it.cloudbreak.spark.SparkServer;
import com.sequenceiq.it.cloudbreak.spark.SparkServerPool;

@Prototype
public class MockedTestContext extends TestContext implements MockTestContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockedTestContext.class);

    @Value("${mock.server.address:localhost}")
    private String mockServerAddress;

    @Inject
    private SparkServerPool sparkServerPool;

    @Inject
    private TestParameter testParameter;

    private SparkServer sparkServer;

    private DefaultModel model;

    private ImageCatalogMockServerSetup imageCatalogMockServerSetup;

    public void initModelAndImageCatalogIfNecessary() {
        initImageCatalogIfNecessary();
        initModelIfNecessary();
    }

    @Override
    public DefaultModel getModel() {
        initModelIfNecessary();
        return model;
    }

    private void initModelIfNecessary() {
        if (model == null) {
            LOGGER.info("Model is null, initialize it");
            model = new DefaultModel();
            model.startModel(getSparkServer().getSparkService(), mockServerAddress, ThreadLocalProfiles.getActiveProfiles());
        }
    }

    @Override
    public SparkServer getSparkServer() {
        initSparkServerIfNecessary();
        initImageCatalogIfNecessary();
        return sparkServer;
    }

    private void initSparkServerIfNecessary() {
        if (sparkServer == null) {
            LOGGER.info("Creating spark server for Test Context: {}", this);
            sparkServer = sparkServerPool.popSecure();
            LOGGER.info("MockedTestContext got spark server: {}", sparkServer.getEndpoint());
        }
    }

    private void initImageCatalogIfNecessary() {
        if (imageCatalogMockServerSetup == null) {
            imageCatalogMockServerSetup = new ImageCatalogMockServerSetup(sparkServerPool.popInsecure());
            imageCatalogMockServerSetup.configureImgCatalogWithExistingSparkServer(testParameter);
        }
    }

    @Override
    public ImageCatalogMockServerSetup getImageCatalogMockServerSetup() {
        initImageCatalogIfNecessary();
        return imageCatalogMockServerSetup;
    }

    @Override
    public DynamicRouteStack dynamicRouteStack() {
        return model.getAmbariMock().getDynamicRouteStack();
    }

    @Override
    public void cleanupTestContext() {
        super.cleanupTestContext();
        if (sparkServer != null) {
            LOGGER.info("Cleaning up MockedTestContext. {}", sparkServer.getEndpoint());
            sparkServerPool.putSecure(sparkServer);
            sparkServerPool.putInsecure(imageCatalogMockServerSetup.getSparkServer());
            imageCatalogMockServerSetup = null;
            sparkServer = null;
            model = null;
        }
    }

    @PreDestroy
    public void preDestroy() {
        LOGGER.info("MockedTestContext destroyed. {}", sparkServer.getEndpoint());
        cleanupTestContext();
    }
}

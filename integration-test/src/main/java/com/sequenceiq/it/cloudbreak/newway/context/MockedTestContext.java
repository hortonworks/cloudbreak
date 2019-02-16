package com.sequenceiq.it.cloudbreak.newway.context;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.config.SparkServer;
import com.sequenceiq.it.cloudbreak.newway.mock.DefaultModel;
import com.sequenceiq.it.cloudbreak.newway.mock.ImageCatalogMockServerSetup;
import com.sequenceiq.it.spark.DynamicRouteStack;

@Prototype
public class MockedTestContext extends TestContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockedTestContext.class);

    @Value("${mock.server.address:localhost}")
    private String mockServerAddress;

    @Inject
    private SparkServer sparkServer;

    @Inject
    private ImageCatalogMockServerSetup imageCatalogMockServerSetup;

    private DefaultModel model;

    @PostConstruct
    private void init() {
        sparkServer.initSparkService(9750, 9900);
        imageCatalogMockServerSetup.configureImgCatalogMock();
        model = new DefaultModel();
        model.startModel(sparkServer.getSparkService(), mockServerAddress);
    }

    public DefaultModel getModel() {
        return model;
    }

    public SparkServer getSparkServer() {
        return sparkServer;
    }

    public ImageCatalogMockServerSetup getImageCatalogMockServerSetup() {
        return imageCatalogMockServerSetup;
    }

    public void shutdown() {
        sparkServer.shutdown();
        imageCatalogMockServerSetup.shutdown();
        setShutdown(true);
    }

    public DynamicRouteStack dynamicRouteStack() {
        return model.getAmbariMock().getDynamicRouteStack();
    }

}

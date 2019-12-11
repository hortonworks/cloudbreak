package com.sequenceiq.it.cloudbreak.context;

import com.sequenceiq.it.cloudbreak.mock.DefaultModel;
import com.sequenceiq.it.cloudbreak.mock.ImageCatalogMockServerSetup;
import com.sequenceiq.it.cloudbreak.spark.DynamicRouteStack;
import com.sequenceiq.it.cloudbreak.spark.SparkServer;

public interface MockTestContext {

    DefaultModel getModel();

    SparkServer getSparkServer();

    ImageCatalogMockServerSetup getImageCatalogMockServerSetup();

    DynamicRouteStack dynamicRouteStack();
}

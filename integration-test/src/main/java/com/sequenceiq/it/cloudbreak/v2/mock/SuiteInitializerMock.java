package com.sequenceiq.it.cloudbreak.v2.mock;

import static com.sequenceiq.it.spark.ITResponse.IMAGE_CATALOG;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.IntegrationTestContext;

@Component(SuiteInitializerMock.NAME)
@Scope("prototype")
public class SuiteInitializerMock extends MockServer {
    public static final String NAME = "SuiteInitializerMock";

    private static final int FAKE_SSH_PORT = 22222;

    public SuiteInitializerMock(int mockPort) {
        super(mockPort, FAKE_SSH_PORT, 0);
    }

    public void mockImageCatalogResponse(IntegrationTestContext itContext) {
        getSparkService().get(IMAGE_CATALOG, (request, response) -> responseFromJsonFile("imagecatalog/catalog.json"));
    }
}

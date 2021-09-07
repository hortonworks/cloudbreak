package com.sequenceiq.it.cloudbreak.testcase.mock;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.mock.ExecuteQueryToMockInfrastructure;
import com.sequenceiq.it.cloudbreak.mock.ImageCatalogMockServerSetup;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public abstract class AbstractMockTest extends AbstractIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMockTest.class);

    @Inject
    private ExecuteQueryToMockInfrastructure executeQueryToMockInfrastructure;

    @Inject
    private ImageCatalogMockServerSetup imageCatalogMockServerSetup;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultEnvironment(testContext);
        createDefaultFreeIpa(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
    }

    protected void createDefaultFreeIpa(TestContext testContext) {
        testContext
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.create())
                .await(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.AVAILABLE)
                .validate();
    }

    public ExecuteQueryToMockInfrastructure getExecuteQueryToMockInfrastructure() {
        return executeQueryToMockInfrastructure;
    }

    public ImageCatalogMockServerSetup getImageCatalogMockServerSetup() {
        return imageCatalogMockServerSetup;
    }
}

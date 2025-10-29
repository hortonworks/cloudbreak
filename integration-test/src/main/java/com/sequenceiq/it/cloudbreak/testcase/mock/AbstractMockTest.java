package com.sequenceiq.it.cloudbreak.testcase.mock;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.mock.MockProperties;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
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

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private MockProperties mockProperties;

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

    @Override
    /**
     * This should work, but now, only works at localhost because something missing in the jenkins jobs (maybe the supported freeipa)
     */
    protected void createEnvironmentWithFreeIpa(TestContext testContext) {
        String freeIpaImageCatalogUrl = testContext.getCloudProvider().getFreeIpaImageCatalogUrl();
        String imageId = mockProperties.getBaseimage().getRedhat7().getImageId();
        testContext
                .given(EnvironmentTestDto.class)
                    .withFreeIpaImage(freeIpaImageCatalogUrl, imageId)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .when(environmentTestClient.describe())
                .validate();
    }

    @Override
    protected void createDefaultDatalake(TestContext testContext) {
        createDefaultEnvironment(testContext);
        createDefaultFreeIpa(testContext);
        createDatalake(testContext);
    }
}

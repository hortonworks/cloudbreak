package com.sequenceiq.it.cloudbreak.testcase.mock;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterApiStatus;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.ExternalizedComputeClusterTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.externalizedcompute.ExternalizedComputeClusterTestDto;
import com.sequenceiq.it.cloudbreak.mock.ImageCatalogMockServerSetup;

public class ExternalizedComputeClusterTest extends AbstractMockTest {

    @Inject
    private ImageCatalogMockServerSetup imageCatalogMockServerSetup;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private ExternalizedComputeClusterTestClient externalizedComputeClusterTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "create an env with externalized compute service",
            then = "these should be available")
    public void testCreateExternalizedComputeClusterThenDelete(MockedTestContext testContext) {
        testContext
                .given(EnvironmentNetworkTestDto.class)
                .given(EnvironmentTestDto.class)
                .withExternalizedComputeCluster()
                .withNetwork()
                .withCreateFreeIpa(true)
                .withOneFreeIpaNode()
                .withFreeIpaImage(imageCatalogMockServerSetup.getFreeIpaImageCatalogUrl(), "f6e778fc-7f17-4535-9021-515351df3691")
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(ExternalizedComputeClusterTestDto.class)
                .when(externalizedComputeClusterTestClient.describeDefault())
                .await(ExternalizedComputeClusterApiStatus.AVAILABLE)
                .when(externalizedComputeClusterTestClient.describe())
                .when(externalizedComputeClusterTestClient.delete())
                .awaitForFlow()
                .await(ExternalizedComputeClusterApiStatus.DELETED)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "create an env with externalized compute service, then delete env",
            then = "env should delete externalized compute service also")
    public void testCreateExternalizedComputeClusterThenDeleteWithEnvDelete(MockedTestContext testContext) {
        testContext
                .given(EnvironmentNetworkTestDto.class)
                .given(EnvironmentTestDto.class)
                .withExternalizedComputeCluster()
                .withNetwork()
                .withCreateFreeIpa(true)
                .withOneFreeIpaNode()
                .withFreeIpaImage(imageCatalogMockServerSetup.getFreeIpaImageCatalogUrl(), "f6e778fc-7f17-4535-9021-515351df3691")
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(ExternalizedComputeClusterTestDto.class)
                .when(externalizedComputeClusterTestClient.describeDefault())
                .await(ExternalizedComputeClusterApiStatus.AVAILABLE)
                .when(externalizedComputeClusterTestClient.describe())
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.delete())
                .awaitForFlow()
                .given(ExternalizedComputeClusterTestDto.class)
                .when(externalizedComputeClusterTestClient.describeDeleted())
                .await(ExternalizedComputeClusterApiStatus.DELETED)
                .validate();
    }
}

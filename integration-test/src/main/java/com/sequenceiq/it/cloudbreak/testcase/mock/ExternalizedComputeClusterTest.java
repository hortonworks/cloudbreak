package com.sequenceiq.it.cloudbreak.testcase.mock;

import java.util.Objects;

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
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
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
                .await(EnvironmentStatus.ARCHIVED)
                .given(ExternalizedComputeClusterTestDto.class)
                .when(externalizedComputeClusterTestClient.describeDeleted())
                .await(ExternalizedComputeClusterApiStatus.DELETED)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "create an env with externalized compute service, then force reinitialized it",
            then = "externalized cluster should be successfully created")
    public void testCreateExternalizedComputeClusterThenReinitialize(MockedTestContext testContext) {
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
                .then((tc, t, c) -> {
                    t.setSavedLiftieCrn(t.getResponse().getLiftieClusterCrn());
                    return t;
                })
                .given(ExternalizedComputeClusterTestDto.class)
                .when(externalizedComputeClusterTestClient.forceReinitialize())
                .awaitForFlow()
                .await(ExternalizedComputeClusterApiStatus.AVAILABLE)
                .when(externalizedComputeClusterTestClient.describe())
                .then((tc, t, c) -> {
                    String firstLiftieCrn = t.getSavedLiftieCrn();
                    String newLiftieCrn = t.getResponse().getLiftieClusterCrn();
                    if (Objects.equals(firstLiftieCrn, newLiftieCrn)) {
                        throw new TestFailException("The first liftie crn should not be the same as the new one");
                    }
                    return t;
                })
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.delete())
                .awaitForFlow()
                .await(EnvironmentStatus.ARCHIVED)
                .given(ExternalizedComputeClusterTestDto.class)
                .when(externalizedComputeClusterTestClient.describeDeleted())
                .await(ExternalizedComputeClusterApiStatus.DELETED)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "create an env without externalized compute service, then migrate it, then force reinitialize",
            then = "externalized cluster should be successfully created")
    public void testCreateV1EnvAndMigrateToV2ThenForceReinitialize(MockedTestContext testContext) {
        testContext
                .given(EnvironmentNetworkTestDto.class)
                .given(EnvironmentTestDto.class)
                .withNetwork()
                .withCreateFreeIpa(true)
                .withOneFreeIpaNode()
                .withFreeIpaImage(imageCatalogMockServerSetup.getFreeIpaImageCatalogUrl(), "f6e778fc-7f17-4535-9021-515351df3691")
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .when(environmentTestClient.createDefaultExternalizedComputeCluster())
                .awaitForFlow()
                .await(EnvironmentStatus.AVAILABLE)
                .given(ExternalizedComputeClusterTestDto.class)
                .when(externalizedComputeClusterTestClient.describeDefault())
                .await(ExternalizedComputeClusterApiStatus.AVAILABLE)
                .when(externalizedComputeClusterTestClient.describe())
                .then((tc, t, c) -> {
                    t.setSavedLiftieCrn(t.getResponse().getLiftieClusterCrn());
                    return t;
                })
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.reInitializeDefaultExternalizedComputeCluster(true))
                .awaitForFlow()
                .await(EnvironmentStatus.AVAILABLE)
                .when(environmentTestClient.describe())
                .then((tc, t, c) -> {
                    boolean privateCluster = t.getResponse().getExternalizedComputeCluster().isPrivateCluster();
                    if (!privateCluster) {
                        throw new TestFailException("compute cluster is not a private cluster");
                    }
                    return t;
                })
                .given(ExternalizedComputeClusterTestDto.class)
                .when(externalizedComputeClusterTestClient.describe())
                .then((tc, t, c) -> {
                    String firstLiftieCrn = t.getSavedLiftieCrn();
                    String newLiftieCrn = t.getResponse().getLiftieClusterCrn();
                    if (Objects.equals(firstLiftieCrn, newLiftieCrn)) {
                        throw new TestFailException("The first liftie crn should not be the same as the new one");
                    }
                    return t;
                })
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.delete())
                .awaitForFlow()
                .await(EnvironmentStatus.ARCHIVED)
                .given(ExternalizedComputeClusterTestDto.class)
                .when(externalizedComputeClusterTestClient.describeDeleted())
                .await(ExternalizedComputeClusterApiStatus.DELETED)
                .validate();
    }
}

package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;
import org.testng.annotations.Test;

import com.cloudera.thunderhead.service.liftieshared.LiftieSharedProto.CreateClusterRequest;
import com.cloudera.thunderhead.service.liftieshared.LiftieSharedProto.CreateClusterRequest.Builder;
import com.cloudera.thunderhead.service.liftieshared.LiftieSharedProto.ListClusterItem;
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
import com.sequenceiq.it.cloudbreak.mock.ExecuteQueryToMockInfrastructure;
import com.sequenceiq.it.cloudbreak.mock.ImageCatalogMockServerSetup;
import com.sequenceiq.liftie.client.LiftieGrpcClient;

public class ExternalizedComputeClusterTest extends AbstractMockTest {

    @Inject
    private ImageCatalogMockServerSetup imageCatalogMockServerSetup;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private ExternalizedComputeClusterTestClient externalizedComputeClusterTestClient;

    @Inject
    private LiftieGrpcClient liftieGrpcClient;

    @Inject
    private ExecuteQueryToMockInfrastructure executeQuery;

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
                .awaitForCreationFlow()
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
            when = "create an env with externalized compute service",
            then = "these should be available")
    public void testCreateExternalizedComputeClusterValidationFail(MockedTestContext testContext) {
        String envName = StringUtils.substring("validationfailenv" + UUID.randomUUID().getMostSignificantBits(), 0, 27);
        testContext
                .given(EnvironmentNetworkTestDto.class)
                .given(EnvironmentTestDto.class)
                .withName(envName)
                .withExternalizedComputeCluster()
                .withNetwork()
                .withCreateFreeIpa(true)
                .withOneFreeIpaNode()
                .withFreeIpaImage(imageCatalogMockServerSetup.getFreeIpaImageCatalogUrl(), "f6e778fc-7f17-4535-9021-515351df3691")
                .when(environmentTestClient.create())
                .awaitForFlow(emptyRunningParameter().withWaitForFlowFail())
                .when(environmentTestClient.describe())
                .then((testContext1, envDto, client) -> {
                    String statusReason = envDto.getResponse().getStatusReason();
                    if (!statusReason.contains("Validation error!")) {
                        throw new TestFailException("Status reason should contain 'Validation error!' message");
                    }
                    return envDto;
                })
                .given(ExternalizedComputeClusterTestDto.class)
                .when(externalizedComputeClusterTestClient.describeDefaultNotExists())
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.reInitializeDefaultExternalizedComputeCluster(true))
                .awaitForFlow(emptyRunningParameter().withWaitForFlowFail())
                .when(environmentTestClient.describe())
                .then((testContext1, envDto, client) -> {
                    EnvironmentStatus environmentStatus = envDto.getResponse().getEnvironmentStatus();
                    if (!EnvironmentStatus.AVAILABLE.equals(environmentStatus)) {
                        throw new TestFailException("Environment status should be AVAILABLE");
                    }
                    String statusReason = envDto.getResponse().getStatusReason();
                    if (!statusReason.contains("Validation error!")) {
                        throw new TestFailException("Status reason should contain 'Validation error!' message");
                    }
                    return envDto;
                })
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
            when = "create an env with externalized compute service, then create auxiliary clusters, then delete env",
            then = "env should delete externalized compute cluster and auxiliary clusters also")
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
                .awaitForCreationFlow()
                .given(ExternalizedComputeClusterTestDto.class)
                .when(externalizedComputeClusterTestClient.describeDefault())
                .await(ExternalizedComputeClusterApiStatus.AVAILABLE)
                .when(externalizedComputeClusterTestClient.describe())
                .given(EnvironmentTestDto.class)
                .then((testContext1, testDto, client) -> {
                    createCluster(testContext, testDto, "mock1");
                    createCluster(testContext, testDto, "mock2");
                    return testDto;
                })
                .when(environmentTestClient.delete())
                .awaitForFlow()
                .await(EnvironmentStatus.ARCHIVED)
                .then((testContext1, testDto, client) -> {
                    List<ListClusterItem> auxClusters = liftieGrpcClient.listAuxClusters(testDto.getResourceCrn(), testContext.getActingUserCrn().toString());
                    if (!auxClusters.isEmpty()) {
                        throw new TestFailException("Auxiliary clusters should be deleted with env deletion!");
                    }
                    return testDto;
                })
                .given(ExternalizedComputeClusterTestDto.class)
                .when(externalizedComputeClusterTestClient.describeDeleted())
                .await(ExternalizedComputeClusterApiStatus.DELETED)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "create an env with externalized compute service, then create auxiliary clusters, then delete env, " +
                    "but it should fail, then force delete",
            then = "env should delete externalized compute cluster and auxiliary clusters also, only failed should remain")
    public void testCreateExternalizedComputeClusterThenDeleteWithEnvDeleteButFailsAndForceShouldWork(MockedTestContext testContext) {
        Set<String> workerNodeSubnets = Set.of("net1", "net2");
        testContext
                .given(EnvironmentNetworkTestDto.class)
                .given(EnvironmentTestDto.class)
                .withExternalizedComputeCluster(workerNodeSubnets)
                .withNetwork()
                .withCreateFreeIpa(true)
                .withOneFreeIpaNode()
                .withFreeIpaImage(imageCatalogMockServerSetup.getFreeIpaImageCatalogUrl(), "f6e778fc-7f17-4535-9021-515351df3691")
                .when(environmentTestClient.create())
                .awaitForCreationFlow()
                .when(environmentTestClient.describe())
                .then((testContext1, testDto, client) -> {
                    Set<String> returnedSubnets = testDto.getResponse().getExternalizedComputeCluster().getWorkerNodeSubnetIds();
                    if (!returnedSubnets.equals(workerNodeSubnets)) {
                        throw new TestFailException("Worker subnets do not match for compute cluster!");
                    }
                    return testDto;
                })
                .given(ExternalizedComputeClusterTestDto.class)
                .when(externalizedComputeClusterTestClient.describeDefault())
                .await(ExternalizedComputeClusterApiStatus.AVAILABLE)
                .when(externalizedComputeClusterTestClient.describe())
                .given(EnvironmentTestDto.class)
                .then((testContext1, testDto, client) -> {
                    createCluster(testContext, testDto, "mock1");
                    createCluster(testContext, testDto, "mock2");
                    return testDto;
                })
                .then((testContext1, testDto, client) -> {
                    List<ListClusterItem> auxClusters = liftieGrpcClient.listAuxClusters(testDto.getCrn(), testContext.getActingUserCrn().toString());
                    for (ListClusterItem auxCluster : auxClusters) {
                        if ("mock1".equals(auxCluster.getClusterName())) {
                            failLiftieCommands(auxCluster);
                        }
                    }
                    return testDto;
                })
                .when(environmentTestClient.delete())
                .awaitForFlow()
                .await(EnvironmentStatus.DELETE_FAILED)
                .when(environmentTestClient.forceDelete())
                .awaitForFlow()
                .await(EnvironmentStatus.ARCHIVED)
                .then((testContext1, testDto, client) -> {
                    List<ListClusterItem> auxClusters = liftieGrpcClient.listAuxClusters(testDto.getResourceCrn(), testContext.getActingUserCrn().toString());
                    if (auxClusters.size() != 1) {
                        throw new TestFailException("There should be 1 auxiliary cluster left over!");
                    }
                    ListClusterItem first = auxClusters.getFirst();
                    if (!"mock1".equals(first.getClusterName())) {
                        throw new TestFailException("'mock1' cluster should be the left over auxiliary cluster!");
                    }
                    return testDto;
                })
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
                .awaitForCreationFlow()
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
                .awaitForCreationFlow()
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

    private void failLiftieCommands(ListClusterItem auxCluster) {
        executeQuery.executeMethod(HttpMethod.POST, "/liftie/{clusterId}/failure/commands", Map.of(), null, r -> r,
                w -> w.resolveTemplate("clusterId", auxCluster.getClusterId()));
    }

    private void createCluster(MockedTestContext testContext, EnvironmentTestDto testDto, String liftieName) {
        Builder liftieBuilder = CreateClusterRequest.newBuilder();
        liftieBuilder.setEnvironment(testDto.getResourceCrn());
        liftieBuilder.setName(liftieName);
        liftieGrpcClient.createCluster(liftieBuilder.build(), testContext.getActingUserCrn().toString());
    }
}

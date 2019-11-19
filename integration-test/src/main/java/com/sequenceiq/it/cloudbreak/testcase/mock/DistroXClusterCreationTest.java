package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;
import org.testng.annotations.Test;

import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkMockParams;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.assertion.MockVerification;
import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.clouderamanager.DistroXClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXImageTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.mock.model.ClouderaManagerMock;
import com.sequenceiq.it.cloudbreak.testcase.mock.clouderamanager.AbstractClouderaManagerTest;
import com.sequenceiq.sdx.api.model.SdxCloudStorageRequest;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

public class DistroXClusterCreationTest extends AbstractClouderaManagerTest {

    private static final String IMAGE_CATALOG_ID = "f6e778fc-7f17-4535-9021-515351df3691";

    private static final String MOCK_HOSTNAME = "mockrdshost";

    private static final String CM_FOR_DISTRO_X = "cm4dstrx";

    private static final String CLUSTER_KEY = "cmdistrox";

    private static final String DIX_IMG_KEY = "dixImg";

    private static final String DIX_NET_KEY = "dixNet";

    private static final String DISTRO_X_STACK = "distroxstack";

    private static final String HOST_TEMPLATE_REF_NAME_FORMAT = "\"hostTemplateRefName\":\"%s\"";

    private static final String ENVIRONMENT_LOCATION = "London";

    @Inject
    private BlueprintTestClient blueprintTestClient;

    @Inject
    private DistroXTestClient distroXClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a DistroX with Cloudera Manager is created",
            then = "the cluster should be available")
    public void testCreateNewRegularDistroXCluster(MockedTestContext testContext) {
        testContext
                .given(DIX_NET_KEY, DistroXNetworkTestDto.class)
                .given(DIX_IMG_KEY, DistroXImageTestDto.class)
                .withImageCatalog(getImageCatalogName(testContext))
                .withImageId(IMAGE_CATALOG_ID)
                .given(CM_FOR_DISTRO_X, DistroXClouderaManagerTestDto.class)
                .given(CLUSTER_KEY, DistroXClusterTestDto.class)
                .withBlueprintName(getBlueprintName(testContext))
                .withValidateBlueprint(false)
                .withClouderaManager(CM_FOR_DISTRO_X)
                .given(DistroXTestDto.class)
                .withGatewayPort(testContext.getSparkServer().getPort())
                .withCluster(CLUSTER_KEY)
                .withImageSettings(DIX_IMG_KEY)
                .withNetwork(DIX_NET_KEY)
                .when(distroXClient.create())
                .await(STACK_AVAILABLE)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a DistroX cluster has created",
            then = "the cluster should be available AND the generated cm template should contain the expected values")
    public void testCreateNewRegularDistroXClusterWhileValidatingCMTemplate(MockedTestContext testContext) {
        testContext
                .given(DIX_NET_KEY, DistroXNetworkTestDto.class)
                .given(DIX_IMG_KEY, DistroXImageTestDto.class)
                .withImageCatalog(getImageCatalogName(testContext))
                .withImageId(IMAGE_CATALOG_ID)
                .given(CM_FOR_DISTRO_X, DistroXClouderaManagerTestDto.class)
                .given(CLUSTER_KEY, DistroXClusterTestDto.class)
                .withBlueprintName(getBlueprintName(testContext))
                .withValidateBlueprint(false)
                .withClouderaManager(CM_FOR_DISTRO_X)
                .given(DistroXTestDto.class)
                .withGatewayPort(testContext.getSparkServer().getPort())
                .withCluster(CLUSTER_KEY)
                .withImageSettings(DIX_IMG_KEY)
                .withNetwork(DIX_NET_KEY)
                .when(distroXClient.create())
                .await(STACK_AVAILABLE)
                .then(DistroXClusterCreationTest::distroxClusterTemplateContainsCDHAsProduct)
                .then(DistroXClusterCreationTest::distroxClusterTemplateContainsComputeNode)
                .then(DistroXClusterCreationTest::distroxClusterTemplateContainsMasterNode)
                .then(DistroXClusterCreationTest::distroxClusterTemplateContainsWorkerNode)
                .then(DistroXClusterCreationTest::distroxClusterTemplateContainsClusterName)
                .then(DistroXClusterCreationTest::distroxClusterTemplateContainsRepositories)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running Cloudbreak",
            when = "a valid SDX create request is sent with an attached cloud storage",
            then = "DistroX should be available with the inherited cloud storage AND both the DistroX and the SDX is deletable"
    )
    public void testDistroxCloudStorage(MockedTestContext testContext) {
        String storageEnvKey = resourcePropertyProvider().getName();

        String sdxInternal = resourcePropertyProvider().getName();
        String stack = resourcePropertyProvider().getName();
        String clouderaManager = "cm";
        String cluster = "cmcluster";
        String networkKey = "someNetwork";

        testContext
                .given(networkKey, EnvironmentNetworkTestDto.class)
                .withMock(new EnvironmentNetworkMockParams())
                .given(storageEnvKey, EnvironmentTestDto.class)
                .withDescription("Env with telemetry")
                .withLocation(ENVIRONMENT_LOCATION)
                .withNetwork(networkKey)
                .withTelemetry(telemetry())
                .withCreateFreeIpa(Boolean.FALSE)
                .withMockIDBMS()
                .when(getEnvironmentTestClient().create(), key(storageEnvKey))
                .await(EnvironmentStatus.AVAILABLE)
                .when(getEnvironmentTestClient().describe(), key(storageEnvKey))
                .given(clouderaManager, ClouderaManagerTestDto.class)
                .given(cluster, ClusterTestDto.class).withClouderaManager(clouderaManager)
                .given(stack, StackTestDto.class).withCluster(cluster).withGatewayPort(testContext.getSparkServer().getPort()).withEnvironmentKey(storageEnvKey)
                .given(sdxInternal, SdxInternalTestDto.class).withStackRequest(stack, cluster).withDatabase(sdxDatabaseRequestWithCreateTrue())
                .withCloudStorage(testStorage()).withEnvironmentKey(key(storageEnvKey))
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING)
                .given(DIX_NET_KEY, DistroXNetworkTestDto.class)
                .given(DIX_IMG_KEY, DistroXImageTestDto.class)
                .withImageCatalog(getImageCatalogName(testContext))
                .withImageId(IMAGE_CATALOG_ID)
                .given(CM_FOR_DISTRO_X, DistroXClouderaManagerTestDto.class)
                .given(CLUSTER_KEY, DistroXClusterTestDto.class)
                .withBlueprintName(getBlueprintName(testContext))
                .withValidateBlueprint(false)
                .withClouderaManager(CM_FOR_DISTRO_X)
                .withRdsConfigNames()
                .given(DistroXTestDto.class)
                .withEnvironmentKey(storageEnvKey)
                .withGatewayPort(testContext.getSparkServer().getPort())
                .withCluster(CLUSTER_KEY)
                .withImageSettings(DIX_IMG_KEY)
                .withNetwork(DIX_NET_KEY)
                .when(distroXClient.create(), key(DISTRO_X_STACK))
                .await(STACK_AVAILABLE)
                .then(DistroXClusterCreationTest::distroxInheritedCloudStorage)
                .then(DistroXClusterCreationTest::distroxCloudStorageLocationNotEmpty)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a running Cloudbreak",
            when = "a valid SDX has created with an external database",
            and = "a valid Distrox create request is sent",
            then = "DistroX should be available with the inherited database AND both the DistroX and the SDX is deletable"
    )
    public void testSdxWithAttachedDatabase(MockedTestContext testContext) {
        String envName = resourcePropertyProvider().getEnvironmentName();
        String envKey = "dbEnvKey";

        String sdxInternal = resourcePropertyProvider().getName();
        String stack = resourcePropertyProvider().getName();
        String clouderaManager = "cm";
        String cluster = "cmcluster";
        String networkKey = "someNetwork";

        testContext
                .given(networkKey, EnvironmentNetworkTestDto.class)
                .withMock(new EnvironmentNetworkMockParams())
                .given(envKey, EnvironmentTestDto.class)
                .withNetwork(networkKey)
                .withCreateFreeIpa(Boolean.FALSE)
                .withName(envName)
                .when(getEnvironmentTestClient().create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(clouderaManager, ClouderaManagerTestDto.class)
                .given(cluster, ClusterTestDto.class).withClouderaManager(clouderaManager)
                .given(stack, StackTestDto.class).withCluster(cluster).withGatewayPort(testContext.getSparkServer().getPort())
                .given(sdxInternal, SdxInternalTestDto.class).withStackRequest(stack, cluster).withDatabase(sdxDatabaseRequestWithCreateTrue())
                .withEnvironmentKey(key(envKey))
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING)
                .given(DIX_NET_KEY, DistroXNetworkTestDto.class)
                .given(DIX_IMG_KEY, DistroXImageTestDto.class)
                .withImageCatalog(getImageCatalogName(testContext))
                .withImageId(IMAGE_CATALOG_ID)
                .given(CM_FOR_DISTRO_X, DistroXClouderaManagerTestDto.class)
                .given(CLUSTER_KEY, DistroXClusterTestDto.class)
                .withBlueprintName(getBlueprintName(testContext))
                .withValidateBlueprint(false)
                .withClouderaManager(CM_FOR_DISTRO_X)
                .withRdsConfigNames()
                .given(DistroXTestDto.class)
                .withEnvironmentKey(envKey)
                .withGatewayPort(testContext.getSparkServer().getPort())
                .withCluster(CLUSTER_KEY)
                .withImageSettings(DIX_IMG_KEY)
                .withNetwork(DIX_NET_KEY)
                .when(distroXClient.create(), key(DISTRO_X_STACK))
                .await(STACK_AVAILABLE)
                .then(DistroXClusterCreationTest::distroxClusterTemplateContainsMockHostname)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a DistroX with Cloudera Manager is created",
            then = "the cluster should be available AND internal distrox crn should be equal with non-internal distrox response crn")
    public void testInternalDistroXResponse(MockedTestContext testContext) {
        testContext
                .given(DIX_NET_KEY, DistroXNetworkTestDto.class)
                .given(DIX_IMG_KEY, DistroXImageTestDto.class)
                .withImageCatalog(getImageCatalogName(testContext))
                .withImageId(IMAGE_CATALOG_ID)
                .given(CM_FOR_DISTRO_X, DistroXClouderaManagerTestDto.class)
                .given(CLUSTER_KEY, DistroXClusterTestDto.class)
                .withBlueprintName(getBlueprintName(testContext))
                .withValidateBlueprint(false)
                .withClouderaManager(CM_FOR_DISTRO_X)
                .given(DistroXTestDto.class)
                .withGatewayPort(testContext.getSparkServer().getPort())
                .withCluster(CLUSTER_KEY)
                .withImageSettings(DIX_IMG_KEY)
                .withNetwork(DIX_NET_KEY)
                .when(distroXClient.create())
                .await(STACK_AVAILABLE)
                .when(distroXClient.getInternal())
                .then(DistroXClusterCreationTest::assertInternalStackResponse)
                .validate();
    }

    @Override
    protected BlueprintTestClient blueprintTestClient() {
        return blueprintTestClient;
    }

    private String getImageCatalogName(TestContext testContext) {
        return testContext.get(ImageCatalogTestDto.class).getRequest().getName();
    }

    @SuppressWarnings("unchecked")
    private static DistroXTestDto distroxClusterTemplateContainsMockHostname(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) {
        return testDto.then(
                MockVerification.verify(HttpMethod.POST,
                        ClouderaManagerMock.IMPORT_CLUSTERTEMPLATE)
                        .bodyContains(MOCK_HOSTNAME)
                        .exactTimes(2),
                key(DISTRO_X_STACK)
        );
    }

    @SuppressWarnings("unchecked")
    private static DistroXTestDto distroxClusterTemplateContainsCDHAsProduct(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) {
        return testDto.then(
                MockVerification.verify(HttpMethod.POST,
                        ClouderaManagerMock.IMPORT_CLUSTERTEMPLATE)
                        .bodyContains("\"product\":\"CDH\"")
                        .exactTimes(1),
                key(DISTRO_X_STACK)
        );
    }

    @SuppressWarnings("unchecked")
    private static DistroXTestDto distroxClusterTemplateContainsComputeNode(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) {
        return testDto.then(
                MockVerification.verify(HttpMethod.POST,
                        ClouderaManagerMock.IMPORT_CLUSTERTEMPLATE)
                        .bodyContains(String.format(HOST_TEMPLATE_REF_NAME_FORMAT, "compute"))
                        .exactTimes(1),
                key(DISTRO_X_STACK)
        );
    }

    @SuppressWarnings("unchecked")
    private static DistroXTestDto distroxClusterTemplateContainsWorkerNode(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) {
        return testDto.then(
                MockVerification.verify(HttpMethod.POST,
                        ClouderaManagerMock.IMPORT_CLUSTERTEMPLATE)
                        .bodyContains(String.format(HOST_TEMPLATE_REF_NAME_FORMAT, "worker"))
                        .exactTimes(1),
                key(DISTRO_X_STACK)
        );
    }

    @SuppressWarnings("unchecked")
    private static DistroXTestDto distroxClusterTemplateContainsMasterNode(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) {
        return testDto.then(
                MockVerification.verify(HttpMethod.POST,
                        ClouderaManagerMock.IMPORT_CLUSTERTEMPLATE)
                        .bodyContains(String.format(HOST_TEMPLATE_REF_NAME_FORMAT, "master"))
                        .exactTimes(1),
                key(DISTRO_X_STACK)
        );
    }

    @SuppressWarnings("unchecked")
    private static DistroXTestDto distroxClusterTemplateContainsClusterName(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) {
        return testDto.then(
                MockVerification.verify(HttpMethod.POST,
                        ClouderaManagerMock.IMPORT_CLUSTERTEMPLATE)
                        .bodyContains(String.format("\"clusterName\":\"%s\"", testContext.get(DistroXTestDto.class).getName()))
                        .exactTimes(1),
                key(DISTRO_X_STACK)
        );
    }

    @SuppressWarnings("unchecked")
    private static DistroXTestDto distroxClusterTemplateContainsRepositories(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) {
        return testDto.then(
                MockVerification.verify(HttpMethod.POST,
                        ClouderaManagerMock.IMPORT_CLUSTERTEMPLATE)
                        .bodyContains("repositories")
                        .exactTimes(1),
                key(DISTRO_X_STACK)
        )
                .then(MockVerification.verify(HttpMethod.POST,
                        ClouderaManagerMock.IMPORT_CLUSTERTEMPLATE)
                                .bodyContains("https://archive.cloudera.com/cdh")
                                .exactTimes(1),
                        key(DISTRO_X_STACK));
    }

    private static DistroXTestDto distroxInheritedCloudStorage(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) {
        vaildateDistroxHasCloudStorage(testDto);
        return testDto;
    }

    private static DistroXTestDto distroxCloudStorageLocationNotEmpty(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) {
        vaildateDistroxHasCloudStorage(testDto);
        if (testDto.getResponse().getCluster().getCloudStorage().getLocations().isEmpty()) {
            throw new TestFailException("Cloud storage locations should not be empty on DistroX");
        }
        return testDto;
    }

    private static DistroXTestDto assertInternalStackResponse(TestContext testContext, DistroXTestDto testDto, CloudbreakClient cloudbreakClient) {
        if (testDto.getResponse() == null) {
            throw new TestFailException("Stack response cannot be empty.");
        }
        if (testDto.getInternalStackResponse() == null) {
            throw new TestFailException("Internal stack response (by internal actor) cannot be empty.");
        }
        if (StringUtils.isBlank(testDto.getResponse().getCrn())) {
            throw new TestFailException("Crn from stack response be empty.");
        }
        if (!testDto.getResponse().getCrn().equals(testDto.getInternalStackResponse().getCrn())) {
            throw new TestFailException("Stack Response CRN and Internal Stack Response CRN should be equal.");
        }
        return testDto;
    }

    private static void vaildateDistroxHasCloudStorage(DistroXTestDto testDto) {
        if (testDto.getResponse().getCluster().getCloudStorage() == null) {
            throw new TestFailException("Cloud storage should be set on DistroX");
        }
    }

    private SdxCloudStorageRequest testStorage() {
        SdxCloudStorageRequest cloudStorage = new SdxCloudStorageRequest();
        cloudStorage.setBaseLocation(getS3Location());
        cloudStorage.setFileSystemType(FileSystemType.S3);
        cloudStorage.setS3(getS3Params());
        return cloudStorage;
    }

    private String getBlueprintName(MockedTestContext testContext) {
        return testContext.get(BlueprintTestDto.class).getRequest().getName();
    }

    private SdxDatabaseRequest sdxDatabaseRequestWithCreateTrue() {
        SdxDatabaseRequest dbRequest = new SdxDatabaseRequest();
        dbRequest.setCreate(Boolean.TRUE);
        return dbRequest;
    }

    private TelemetryRequest telemetry() {
        TelemetryRequest telemetry = new TelemetryRequest();
        LoggingRequest logging = new LoggingRequest();
        logging.setS3(getS3Params());
        logging.setStorageLocation(getS3Location());
        telemetry.setLogging(logging);
        return telemetry;
    }

    private S3CloudStorageV1Parameters getS3Params() {
        S3CloudStorageV1Parameters params = new S3CloudStorageV1Parameters();
        params.setInstanceProfile("someInstanceProfileStuff");
        return params;
    }

    private String getS3Location() {
        return "s3asomeBaseLocation:" + ENVIRONMENT_LOCATION;
    }

}

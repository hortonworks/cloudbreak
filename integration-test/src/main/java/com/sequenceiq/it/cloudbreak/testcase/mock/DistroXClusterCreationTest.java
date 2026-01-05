package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.expectedMessage;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.pollingInterval;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.Test;

import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.assertion.audit.DatahubAuditGrpcServiceAssertion;
import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.RedbeamsDatabaseServerTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.config.server.ServerProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.dto.database.RedbeamsDatabaseServerTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.clouderamanager.DistroXClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXImageTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.testcase.mock.clouderamanager.AbstractClouderaManagerTest;
import com.sequenceiq.sdx.api.model.SdxCloudStorageRequest;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

public class DistroXClusterCreationTest extends AbstractClouderaManagerTest {

    private static final Duration POLLING_INTERVAL = Duration.of(3000, ChronoUnit.MILLIS);

    private static final String IMAGE_CATALOG_ID = "f6e778fc-7f17-4535-9021-515351df3691";

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

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private DatahubAuditGrpcServiceAssertion auditGrpcServiceAssertion;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private ServerProperties serverProperties;

    @Inject
    private RedbeamsDatabaseServerTestClient redbeamsDatabaseServerTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
        createCmBlueprint(testContext);
        createDefaultDatalake(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a DistroX with Cloudera Manager is created",
            then = "the cluster should be available")
    public void testCreateNewRegularDistroXCluster(MockedTestContext testContext) {
        testContext
                .given(DIX_NET_KEY, DistroXNetworkTestDto.class)
                .given(DIX_IMG_KEY, DistroXImageTestDto.class)
                .withImageCatalog()
                .withImageId(IMAGE_CATALOG_ID)
                .given(CM_FOR_DISTRO_X, DistroXClouderaManagerTestDto.class)
                .given(CLUSTER_KEY, DistroXClusterTestDto.class)
                .withBlueprintName(getBlueprintName(testContext))
                .withValidateBlueprint(false)
                .withClouderaManager(CM_FOR_DISTRO_X)
                .given(DistroXTestDto.class)
                .withCluster(CLUSTER_KEY)
                .withImageSettings(DIX_IMG_KEY)
                .withNetwork(DIX_NET_KEY)
                .when(distroXClient.postStackForBlueprint())
                .then(DistroXClusterCreationTest::distroxClusterGeneratedBlueprintCheck)
                .when(distroXClient.create())
                .await(STACK_AVAILABLE)
                .then(DistroXClusterCreationTest::distroxServiceTypeTagExists)
                .then(auditGrpcServiceAssertion::create)
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.delete())
                .await(EnvironmentStatus.ARCHIVED)
                .given(DistroXTestDto.class)
                .then(auditGrpcServiceAssertion::delete)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a stopped environment",
            when = "a DistroX creation request sent",
            then = "its creation should not be started due to the unavailability of the environment")
    public void testWhenEnvIsStoppedUnableToCreateDistroX(MockedTestContext testContext) {
        EnvironmentTestDto environment = testContext.get(EnvironmentTestDto.class);
        givenAnEnvironmentInStoppedState(testContext)
                .given(DIX_NET_KEY, DistroXNetworkTestDto.class)
                .given(DIX_IMG_KEY, DistroXImageTestDto.class)
                .withImageCatalog()
                .withImageId(IMAGE_CATALOG_ID)
                .given(CM_FOR_DISTRO_X, DistroXClouderaManagerTestDto.class)
                .given(CLUSTER_KEY, DistroXClusterTestDto.class)
                .withBlueprintName(getBlueprintName(testContext))
                .withValidateBlueprint(false)
                .withClouderaManager(CM_FOR_DISTRO_X)
                .given(DistroXTestDto.class)
                .withCluster(CLUSTER_KEY)
                .withImageSettings(DIX_IMG_KEY)
                .withNetwork(DIX_NET_KEY)
                .whenException(distroXClient.create(), BadRequestException.class,
                        expectedMessage(String.format("If you want to provision a Data Hub then the FreeIPA instance must be running in the '%s' Environment.",
                                environment.getName())))
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
                .withImageCatalog()
                .withImageId(IMAGE_CATALOG_ID)
                .given(CM_FOR_DISTRO_X, DistroXClouderaManagerTestDto.class)
                .given(CLUSTER_KEY, DistroXClusterTestDto.class)
                .withBlueprintName(getBlueprintName(testContext))
                .withValidateBlueprint(false)
                .withClouderaManager(CM_FOR_DISTRO_X)
                .given(DistroXTestDto.class)
                .withCluster(CLUSTER_KEY)
                .withImageSettings(DIX_IMG_KEY)
                .withNetwork(DIX_NET_KEY)
                .when(distroXClient.create())
                .enableVerification()
                .await(STACK_AVAILABLE)
                .mockCm().cmImportClusterTemplate().post().bodyContains("\"product\":\"CDH\"", 1).verify()
                .mockCm().cmImportClusterTemplate().post().bodyContains(String.format(HOST_TEMPLATE_REF_NAME_FORMAT, "compute"), 1).verify()
                .mockCm().cmImportClusterTemplate().post().bodyContains(String.format(HOST_TEMPLATE_REF_NAME_FORMAT, "master"), 1).verify()
                .mockCm().cmImportClusterTemplate().post().bodyContains(String.format(HOST_TEMPLATE_REF_NAME_FORMAT, "worker"), 3).verify()
                .mockCm().cmImportClusterTemplate().post()
                .bodyContains(String.format("\"clusterName\":\"%s\"", testContext.get(DistroXTestDto.class).getName()), 1).verify()
                .mockCm().cmImportClusterTemplate().post().bodyContains("repositories", 1).verify()
                .mockCm().cmImportClusterTemplate().post().bodyContains(serverProperties.getMockImageCatalogAddr(), 4).verify()
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

        testContext
                .given(storageEnvKey, EnvironmentTestDto.class)
                .withDescription("Env with telemetry")
                .withLocation(ENVIRONMENT_LOCATION)
                .withNetwork()
                .withTelemetry(telemetry())
                .withCreateFreeIpa(Boolean.FALSE)
                .withMockIDBMS()
                .when(getEnvironmentTestClient().create(), key(storageEnvKey))
                .await(EnvironmentStatus.AVAILABLE)
                .when(getEnvironmentTestClient().describe(), key(storageEnvKey))
                .given(FreeIpaTestDto.class).withEnvironment(storageEnvKey)
                .when(freeIpaTestClient.create())
                .awaitForCreationFlow()
                .given(sdxInternal, SdxInternalTestDto.class)
                .withDatabase(sdxDatabaseRequestWithCreateTrue())
                .withCloudStorage(testStorage())
                .withEnvironmentKey(key(storageEnvKey))
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING)
                .given(DIX_NET_KEY, DistroXNetworkTestDto.class)
                .given(DIX_IMG_KEY, DistroXImageTestDto.class)
                .withImageCatalog()
                .withImageId(IMAGE_CATALOG_ID)
                .given(CM_FOR_DISTRO_X, DistroXClouderaManagerTestDto.class)
                .given(CLUSTER_KEY, DistroXClusterTestDto.class)
                .withBlueprintName(getBlueprintName(testContext))
                .withValidateBlueprint(false)
                .withClouderaManager(CM_FOR_DISTRO_X)
                .withRdsConfigNames()
                .given(DistroXTestDto.class)
                .withEnvironmentKey(storageEnvKey)
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

        testContext
                .given(envKey, EnvironmentTestDto.class)
                .withNetwork()
                .withCreateFreeIpa(Boolean.FALSE)
                .withName(envName)
                .when(getEnvironmentTestClient().create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIpaTestDto.class).withEnvironment(envKey)
                .when(freeIpaTestClient.create())
                .awaitForCreationFlow()
                .given(sdxInternal, SdxInternalTestDto.class)
                .withDatabase(sdxDatabaseRequestWithCreateTrue())
                .withEnvironmentKey(key(envKey))
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING)
                .given(RedbeamsDatabaseServerTestDto.class)
                .when(redbeamsDatabaseServerTestClient.describeByClusterCrn(
                        testContext.get(envKey, EnvironmentTestDto.class).getResponse().getCrn(),
                        testContext.get(sdxInternal, SdxInternalTestDto.class).getCrn()
                ))
                .given(DIX_NET_KEY, DistroXNetworkTestDto.class)
                .given(DIX_IMG_KEY, DistroXImageTestDto.class)
                .withImageCatalog()
                .withImageId(IMAGE_CATALOG_ID)
                .given(CM_FOR_DISTRO_X, DistroXClouderaManagerTestDto.class)
                .given(CLUSTER_KEY, DistroXClusterTestDto.class)
                .withBlueprintName(getBlueprintName(testContext))
                .withValidateBlueprint(false)
                .withClouderaManager(CM_FOR_DISTRO_X)
                .withRdsConfigNames()
                .given(DistroXTestDto.class)
                .withEnvironmentKey(envKey)
                .withCluster(CLUSTER_KEY)
                .withImageSettings(DIX_IMG_KEY)
                .withNetwork(DIX_NET_KEY)
                .when(distroXClient.create(), key(DISTRO_X_STACK))
                .enableVerification()
                .await(STACK_AVAILABLE)
                .mockCm().cmImportClusterTemplate().post().bodyContains(
                        testContext.given(RedbeamsDatabaseServerTestDto.class).getResponse().getHost(), 2).verify()
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
                .withImageCatalog()
                .withImageId(IMAGE_CATALOG_ID)
                .given(CM_FOR_DISTRO_X, DistroXClouderaManagerTestDto.class)
                .given(CLUSTER_KEY, DistroXClusterTestDto.class)
                .withBlueprintName(getBlueprintName(testContext))
                .withValidateBlueprint(false)
                .withClouderaManager(CM_FOR_DISTRO_X)
                .given(DistroXTestDto.class)
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

    private static DistroXTestDto distroxClusterGeneratedBlueprintCheck(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) {
        if (testDto.getGeneratedBlueprint() == null
                || testDto.getGeneratedBlueprint().getBlueprintText() == null
                || testDto.getGeneratedBlueprint().getBlueprintText().isEmpty()) {
            throw new TestFailException("Template Generation does not work properly because you get empty response");
        }
        return testDto;
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

    private static DistroXTestDto distroxServiceTypeTagExists(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) {
        if (testDto.getResponse().getTags() == null || testDto.getResponse().getTags().getApplication() == null) {
            throw new TestFailException("Application tags cannot be empty for DistroX cluster");
        }
        if (!testDto.getResponse().getTags().getApplication().containsKey("Cloudera-Service-Type")) {
            throw new TestFailException("Application tag 'Cloudera-Service-Type' needs to exist for DistroX cluster");
        }
        return testDto;
    }

    private EnvironmentTestDto givenAnEnvironmentInStoppedState(MockedTestContext testContext) {
        return testContext
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.stop())
                .await(EnvironmentStatus.ENV_STOPPED, pollingInterval(POLLING_INTERVAL));
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
        dbRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NON_HA);
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

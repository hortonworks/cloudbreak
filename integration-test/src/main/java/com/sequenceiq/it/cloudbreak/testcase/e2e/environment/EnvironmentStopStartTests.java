package com.sequenceiq.it.cloudbreak.testcase.e2e.environment;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.PRE_SERVICE_DEPLOYMENT;
import static com.sequenceiq.it.cloudbreak.assertion.freeipa.RecipeTestAssertion.validateFilesOnFreeIpa;
import static java.lang.String.format;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.util.SanitizerUtil;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseAvailabilityType;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
import com.sequenceiq.it.cloudbreak.assertion.util.CloudProviderSideTagAssertion;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.dto.verticalscale.VerticalScalingTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.microservice.MicroserviceClient;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.DistroxUtil;
import com.sequenceiq.it.cloudbreak.util.RecipeUtil;
import com.sequenceiq.it.cloudbreak.util.clouderamanager.ClouderaManagerUtil;
import com.sequenceiq.it.cloudbreak.util.ssh.SshJUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class EnvironmentStopStartTests extends AbstractE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentStopStartTests.class);

    private static final Map<String, String> ENV_TAGS = Map.of("envTagKey", "envTagValue");

    private static final Map<String, String> SDX_TAGS = Map.of("sdxTagKey", "sdxTagValue");

    private static final Map<String, String> DX1_TAGS = Map.of("distroxTagKey", "distroxTagValue");

    private static final String VERTICAL_SCALE_FAIL_MSG_FORMAT = "%s vertical scale was not successful, because the expected instance type is the following: %s"
            + ", but the actual is: %s";

    private static final String MOCK_UMS_PASSWORD = "Password123!";

    private static final String FREEIPA_VERTICAL_SCALE_KEY = "freeipaVerticalScaleKey";

    private static final String SDX_VERTICAL_SCALE_KEY = "sdxVerticalScaleKey";

    private static final String DISTROX_VERTICAL_SCALE_KEY = "distroxVerticalScaleKey";

    private String telemetryStorageLocation;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private CloudProviderSideTagAssertion cloudProviderSideTagAssertion;

    @Inject
    private ClouderaManagerUtil clouderaManagerUtil;

    @Inject
    private RecipeTestClient recipeTestClient;

    @Inject
    private SshJUtil sshJUtil;

    @Inject
    private RecipeUtil recipeUtil;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private DistroxUtil distroxUtil;

    private String freeipaRequestedInstanceType;

    private String datalakeRequestedInstanceType;

    private String datahubRequestedInstanceType;

    private String freeipaRequestedGroupName;

    private String datalakeRequestedGroupName;

    private String datahubRequestedGroupName;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT, timeOut = 9000000)
    @Description(
            given = "there is a running cloudbreak",
            when = "create an attached SDX and Datahubs (in case of AWS, create one of the Datahub with external database)",
            then = "should be stopped first and started after it, and required services should be in running state in CM ")
    public void testCreateStopStartEnvironment(TestContext testContext) {
        LOGGER.info("Environment stop-start test execution has been started....");
        DistroXDatabaseRequest distroXDatabaseRequest = new DistroXDatabaseRequest();
        distroXDatabaseRequest.setAvailabilityType(DistroXDatabaseAvailabilityType.NON_HA);
        String recipeName = resourcePropertyProvider().getName();
        String filePath = "/pre-service-deployment";
        String fileName = "pre-service-deployment";

        testContext
                .given(RecipeTestDto.class)
                    .withName(recipeName)
                    .withContent(recipeUtil.generatePreDeploymentRecipeContent(applicationContext))
                    .withRecipeType(PRE_SERVICE_DEPLOYMENT)
                .when(recipeTestClient.createV4())
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given("telemetry", TelemetryTestDto.class)
                    .withLogging()
                    .withReportClusterLogs()
                .given(EnvironmentTestDto.class)
                    .withNetwork()
                    .withTelemetry("telemetry")
                    .withCreateFreeIpa(Boolean.TRUE)
                    .withOneFreeIpaNode()
                    .withFreeIpaRecipe(Set.of(recipeName))
                    .addTags(ENV_TAGS)
                .when(environmentTestClient.create())
                .then(this::getTelemetryStorageLocation)
                .given(SdxInternalTestDto.class)
                    .withTelemetry("telemetry")
                .addTags(SDX_TAGS)
                    .withCloudStorage(getCloudStorageRequest(testContext))
                .when(sdxTestClient.createInternal())
                .given(EnvironmentTestDto.class)
                .await(EnvironmentStatus.AVAILABLE)
                .then(cloudProviderSideTagAssertion.verifyEnvironmentTags(ENV_TAGS))
                .init(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .then(validateFilesOnFreeIpa(filePath, fileName, 1, sshJUtil))
                .given(SdxInternalTestDto.class)
                    .withTelemetry("telemetry")
                .await(SdxClusterStatusResponse.RUNNING)
                .then(cloudProviderSideTagAssertion.verifyInternalSdxTags(SDX_TAGS))
                .given("dx1", DistroXTestDto.class)
                    .withExternalDatabaseOnAws(distroXDatabaseRequest)
                .addTags(DX1_TAGS)
                .when(distroXTestClient.create(), RunningParameter.key("dx1"))
                .given("dx2", DistroXTestDto.class)
                .when(distroXTestClient.create(), RunningParameter.key("dx2"))
                .given("dx1", DistroXTestDto.class)
                .await(STACK_AVAILABLE, RunningParameter.key("dx1"))
                .then(cloudProviderSideTagAssertion.verifyDistroxTags(DX1_TAGS))
                .given("dx2", DistroXTestDto.class)
                .await(STACK_AVAILABLE, RunningParameter.key("dx2"))
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.stop())
                .await(EnvironmentStatus.ENV_STOPPED)

                .when(this::executeVerticalScaleIfSupported)

                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.start())
                .await(EnvironmentStatus.AVAILABLE)
                .then(this::validateClusterLogsArePresent)
                .then(this::validateClusterBackupsArePresent)
                .given("dx1", DistroXTestDto.class)
                .await(STACK_AVAILABLE, RunningParameter.key("dx1"))
                .awaitForHealthyInstances()
                .then(this::verifyCmServicesStartedSuccessfully)

                .then(this::verifyVerticalScaleOutputsIfSupported)
                .validate();

        LOGGER.info("Environment stop-start test execution has been finished....");
    }

    private <O extends CloudbreakTestDto, C extends MicroserviceClient<?, ?, ?, ?>> O executeVerticalScaleIfSupported(TestContext testContext, O testDto,
            C client) {
        if (testContext.getCloudProvider().verticalScalingSupported()) {
            VerticalScalingTestDto freeipaVerticalScalingTestDto = testContext
                    .given(FREEIPA_VERTICAL_SCALE_KEY, VerticalScalingTestDto.class).withFreeipaVerticalScale();
            freeipaRequestedInstanceType = freeipaVerticalScalingTestDto.getInstanceType();
            freeipaRequestedGroupName = freeipaVerticalScalingTestDto.getGroupName();
            freeipaVerticalScalingTestDto.withFreeipaVerticalScale()
                    .given("telemetry", TelemetryTestDto.class)
                    .withLogging()
                    .withReportClusterLogs()
                    .given(EnvironmentTestDto.class)
                    .withTelemetry("telemetry")
                    .when(environmentTestClient.verticalScale(FREEIPA_VERTICAL_SCALE_KEY))
                    .await(EnvironmentStatus.ENV_STOPPED);

            VerticalScalingTestDto datalakeVerticalScalingTestDto = testContext.given(SDX_VERTICAL_SCALE_KEY, VerticalScalingTestDto.class)
                    .withSdxVerticalScale();
            datalakeRequestedInstanceType = datalakeVerticalScalingTestDto.getInstanceType();
            datalakeRequestedGroupName = datalakeVerticalScalingTestDto.getGroupName();
            datalakeVerticalScalingTestDto.withSdxVerticalScale()
                    .given(SdxInternalTestDto.class)
                    .withTelemetry("telemetry")
                    .when(sdxTestClient.verticalScale(SDX_VERTICAL_SCALE_KEY))
                    .await(SdxClusterStatusResponse.STOPPED);

            VerticalScalingTestDto datahubVerticalScalingTestDto = testContext.given(DISTROX_VERTICAL_SCALE_KEY, VerticalScalingTestDto.class)
                    .withDistroXVerticalScale();
            datahubRequestedInstanceType = datahubVerticalScalingTestDto.getInstanceType();
            datahubRequestedGroupName = datahubVerticalScalingTestDto.getGroupName();
            datahubVerticalScalingTestDto.withDistroXVerticalScale()
                    .given("dx1", DistroXTestDto.class)
                    .when(distroXTestClient.verticalScale(DISTROX_VERTICAL_SCALE_KEY))
                    .await(STACK_STOPPED, RunningParameter.key("dx1"));
        } else {
            LOGGER.debug("No vertical scale will happen this case because at this point Cloudbreak does not support vertical scale in case of the following " +
                    "cloud platform: {}", testContext.getCloudPlatform());
        }
        return testDto;
    }

    private DistroXTestDto verifyVerticalScaleOutputsIfSupported(TestContext testContext, DistroXTestDto testDto, CloudbreakClient cloudbreakClient) {
        if (testContext.getCloudProvider().verticalScalingSupported()) {
            LOGGER.debug("Vertical scaling verification result initiated since the cloud platform '{}' suppots such operation.",
                    testContext.getCloudPlatform());
            testContext
                    .given("telemetry", TelemetryTestDto.class)
                        .withLogging()
                        .withReportClusterLogs()
                    .given(FreeIpaTestDto.class)
                        .withTelemetry("telemetry")
                    .when(freeIpaTestClient.describe())
                    .then(this::validateFreeIpaInstanceType)
                    .then(this::validateInstanceTypesOfFreeipaOnProvider)
                    .given(SdxInternalTestDto.class)
                        .withTelemetry("telemetry")
                    .when(sdxTestClient.detailedDescribeInternal())
                    .then(this::validateDataLakeInstanceType)
                    .then(this::validateInstanceTypesOfDatalakeOnProvider)
                    .given("dx1", DistroXTestDto.class)
                    .when(distroXTestClient.get())
                    .then(this::validateDataHubInstanceType)
                    .then(this::validateInstanceTypesOfDatahubOnProvider);
        } else {
            LOGGER.debug("Since Cloudbreak right now does not support vertical scaling for cloud platform {}, hence no need for verification.",
                    testContext.getCloudPlatform());
        }
        return testDto;
    }

    private FreeIpaTestDto validateInstanceTypesOfFreeipaOnProvider(TestContext testContext, FreeIpaTestDto freeipa, FreeIpaClient freeIpaClient) {
        Map<String, Set<InstanceMetaDataResponse>> metadatas = freeipa.getResponse().getInstanceGroups().stream()
                .collect(Collectors.toMap(InstanceGroupResponse::getName, InstanceGroupResponse::getMetaData));
        List<String> listOfInstanceIdsInIG = metadatas.get(freeipaRequestedGroupName).stream()
                .map(md -> md.getInstanceId())
                .collect(Collectors.toList());
        List<String> instanceTypesOnProvider = testContext.getCloudProvider().getCloudFunctionality()
                .listInstanceTypes(freeipa.getName(), listOfInstanceIdsInIG);

        Assertions.assertThat(instanceTypesOnProvider).hasSize(1);
        Assertions.assertThat(instanceTypesOnProvider.get(0)).withFailMessage("Stack cloud formation ")
                .isEqualTo(freeipaRequestedInstanceType.toLowerCase());

        return freeipa;
    }

    private DistroXTestDto validateInstanceTypesOfDatahubOnProvider(TestContext testContext, DistroXTestDto datahub, CloudbreakClient cloudbreakClient) {
        Map<String, Set<InstanceMetaDataV4Response>> metadatas = datahub.getResponse().getInstanceGroups().stream()
                .collect(Collectors.toMap(InstanceGroupV4Base::getName, InstanceGroupV4Response::getMetadata));
        List<String> listOfInstanceIdsInIG = metadatas.get(datahubRequestedGroupName).stream()
                .map(md -> md.getInstanceId())
                .collect(Collectors.toList());
        List<String> instanceTypesOnProvider = testContext.getCloudProvider().getCloudFunctionality()
                .listInstanceTypes(datahub.getName(), listOfInstanceIdsInIG);

        Assertions.assertThat(instanceTypesOnProvider).hasSize(1);
        Assertions.assertThat(instanceTypesOnProvider.get(0)).withFailMessage("Stack cloud formation ")
                .isEqualTo(datahubRequestedInstanceType.toLowerCase());

        return datahub;
    }

    private SdxInternalTestDto validateInstanceTypesOfDatalakeOnProvider(TestContext testContext, SdxInternalTestDto datalake, SdxClient sdxClient) {
        Map<String, Set<InstanceMetaDataV4Response>> metadatas = datalake.getResponse().getStackV4Response().getInstanceGroups().stream()
                .collect(Collectors.toMap(InstanceGroupV4Base::getName, InstanceGroupV4Response::getMetadata));
        List<String> listOfInstanceIdsInIG = metadatas.get(datalakeRequestedGroupName).stream()
                .map(md -> md.getInstanceId())
                .collect(Collectors.toList());
        List<String> instanceTypesOnProvider = testContext.getCloudProvider().getCloudFunctionality()
                .listInstanceTypes(datalake.getName(), listOfInstanceIdsInIG);

        Assertions.assertThat(instanceTypesOnProvider).hasSize(1);
        Assertions.assertThat(instanceTypesOnProvider.get(0)).withFailMessage("Stack cloud formation ")
                .isEqualTo(datalakeRequestedInstanceType.toLowerCase());

        return datalake;
    }

    private DistroXTestDto verifyCmServicesStartedSuccessfully(TestContext testContext, DistroXTestDto testDto, CloudbreakClient cloudbreakClient) {
        String username = testContext.getActingUserCrn().getResource();
        String sanitizedUserName = SanitizerUtil.sanitizeWorkloadUsername(username);
        clouderaManagerUtil.checkCmServicesStartedSuccessfully(testDto, sanitizedUserName, MOCK_UMS_PASSWORD);
        return testDto;
    }

    private FreeIpaTestDto validateFreeIpaInstanceType(TestContext testContext, FreeIpaTestDto testDto, FreeIpaClient freeIpaClient) {
        validateInstanceType(testDto.findInstanceGroupByType(freeipaRequestedGroupName).getInstanceTemplate().getInstanceType(),
                freeipaRequestedInstanceType, "FreeIPA");
        return testDto;
    }

    public SdxInternalTestDto validateDataLakeInstanceType(TestContext testContext, SdxInternalTestDto testDto, SdxClient sdxClient) {
        validateInstanceType(testDto.findInstanceGroupByName(datalakeRequestedGroupName).getTemplate().getInstanceType(), datalakeRequestedInstanceType,
                "Data Lake");
        return testDto;
    }

    private DistroXTestDto validateDataHubInstanceType(TestContext testContext1, DistroXTestDto testDto, CloudbreakClient client) {
        validateInstanceType(testDto.findInstanceGroupByName(datahubRequestedGroupName).getTemplate().getInstanceType(), datahubRequestedInstanceType,
                "Data Hub");
        return testDto;
    }

    private void validateInstanceType(String instanceType, String expectedType, String service) {
        if (!instanceType.equals(expectedType)) {
            throw new TestFailException(format(VERTICAL_SCALE_FAIL_MSG_FORMAT, service, expectedType, instanceType));
        }
    }

    private EnvironmentTestDto getTelemetryStorageLocation(TestContext testContext, EnvironmentTestDto testDto, EnvironmentClient client) {
        telemetryStorageLocation = testDto.getResponse().getTelemetry().getLogging().getStorageLocation();
        if (StringUtils.isBlank(telemetryStorageLocation)) {
            LOGGER.error(format(" Telemetry Storage Location has not been set at '%s' environment! ", testDto.getName()));
            throw new TestFailException(format(" Telemetry Storage Location has not been set at '%s' environment! ", testDto.getName()));
        } else {
            LOGGER.info(format(" Telemetry Storage Location has been set to '%s' at '%s' environment! ", telemetryStorageLocation, testDto.getName()));
            Log.then(LOGGER, format(" Telemetry Storage Location has been set to '%s' at '%s' environment! ", telemetryStorageLocation, testDto.getName()));
        }
        return testDto;
    }

    private EnvironmentTestDto validateClusterLogsArePresent(TestContext testContext, EnvironmentTestDto testDto, EnvironmentClient client) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageListContainer(telemetryStorageLocation, "cluster-logs", true);
        return testDto;
    }

    private EnvironmentTestDto validateClusterBackupsArePresent(TestContext testContext, EnvironmentTestDto testDto, EnvironmentClient client) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageListContainer(telemetryStorageLocation, "cluster-backups", true);
        return testDto;
    }
}

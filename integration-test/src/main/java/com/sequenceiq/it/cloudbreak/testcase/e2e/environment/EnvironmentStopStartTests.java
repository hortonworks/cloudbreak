package com.sequenceiq.it.cloudbreak.testcase.e2e.environment;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.PRE_SERVICE_DEPLOYMENT;
import static com.sequenceiq.it.cloudbreak.assertion.freeipa.RecipeTestAssertion.validateFilesOnFreeIpa;
import static java.lang.String.format;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
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
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.DistroxUtil;
import com.sequenceiq.it.cloudbreak.util.RecipeUtil;
import com.sequenceiq.it.cloudbreak.util.clouderamanager.ClouderaManagerUtil;
import com.sequenceiq.it.cloudbreak.util.ssh.SshJUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

public class EnvironmentStopStartTests extends AbstractE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentStopStartTests.class);

    private static final Map<String, String> ENV_TAGS = Map.of("envTagKey", "envTagValue");

    private static final Map<String, String> SDX_TAGS = Map.of("sdxTagKey", "sdxTagValue");

    private static final Map<String, String> DX1_TAGS = Map.of("distroxTagKey", "distroxTagValue");

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

    @Test(dataProvider = TEST_CONTEXT, timeOut = 10800000)
    @Description(
            given = "there is a running cloudbreak",
            when = "create an attached SDX and Datahubs (in case of AWS, create one of the Datahub with external database)",
            then = "should be stopped first and started after it, and required services should be in running state in CM ")
    public void testCreateStopStartEnvironment(TestContext testContext) {
        LOGGER.info("Environment stop-start test execution has been started....");
        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NON_HA);

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
                    .withFreeIpaNodes(getFreeIpaInstanceCountByProvider(testContext))
                    .withFreeIpaRecipe(Set.of(recipeName))
                    .withResourceEncryption(testContext.isResourceEncryptionEnabled())
                    .addTags(ENV_TAGS)
                .when(environmentTestClient.create())
                .then(this::getTelemetryStorageLocation)
                .given(SdxInternalTestDto.class)
                    .withTelemetry("telemetry")
                .addTags(SDX_TAGS)
                    .withCloudStorage(getCloudStorageRequest(testContext))
                    .withDatabase(sdxDatabaseRequest)
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

                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.start())
                .await(EnvironmentStatus.AVAILABLE)
                .then(this::validateClusterLogsArePresent)
                .then(this::validateClusterBackupsArePresent)
                .given("dx1", DistroXTestDto.class)
                .await(STACK_AVAILABLE, RunningParameter.key("dx1"))
                .awaitForHealthyInstances()
                .then(this::verifyCmServicesStartedSuccessfully)
                .validate();

        LOGGER.info("Environment stop-start test execution has been finished....");
    }

    private FreeIpaTestDto validateInstanceTypesOfFreeipaOnProvider(TestContext testContext, FreeIpaTestDto freeipa, FreeIpaClient freeIpaClient) {
        Map<String, Set<InstanceMetaDataResponse>> metadatas = freeipa.getResponse().getInstanceGroups().stream()
                .collect(Collectors.toMap(InstanceGroupResponse::getName, InstanceGroupResponse::getMetaData));
        List<String> listOfInstanceIdsInIG = metadatas.get(freeipaRequestedGroupName).stream()
                .map(md -> md.getInstanceId())
                .collect(Collectors.toList());
        List<String> instanceTypesOnProvider = testContext.getCloudProvider().getCloudFunctionality()
                .listInstanceTypes(freeipa.getName(), listOfInstanceIdsInIG);

        Assertions.assertThat(instanceTypesOnProvider).hasSize(getFreeIpaInstanceCountByProvider(testContext));
        LOGGER.info("FreeIPA {} instances {} on provider {}.", freeipa.getResponse().getName(), instanceTypesOnProvider,
                testContext.getCloudPlatform().name());
        instanceTypesOnProvider.forEach(instanceTypeOnProvider -> {
            Assertions.assertThat(instanceTypeOnProvider).withFailMessage(
                            "freeipa's instance type does not match with the requested instance type (freeipa: %s, actual: %s, requested: %s)",
                            freeipa.getName(), instanceTypeOnProvider, freeipaRequestedInstanceType.toLowerCase(Locale.ROOT))
                    .isEqualTo(freeipaRequestedInstanceType.toLowerCase());
        });

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
        String actualInstanceType = instanceTypesOnProvider.get(0).toLowerCase();
        Assertions.assertThat(actualInstanceType).withFailMessage(
                "datahub's instance type does not match with the requested instance type (datahub: %s, actual: %s, requested: %s)",
                        datahub.getName(), actualInstanceType, datahubRequestedInstanceType.toLowerCase())
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
        String actualInstanceType = instanceTypesOnProvider.get(0).toLowerCase();
        Assertions.assertThat(actualInstanceType).withFailMessage(
                        "datahub's instance type does not match with the requested instance type (datahub: %s, actual: %s, requested: %s)",
                        datalake.getName(), actualInstanceType, datalakeRequestedInstanceType.toLowerCase())
                .isEqualTo(datalakeRequestedInstanceType.toLowerCase());

        return datalake;
    }

    private DistroXTestDto verifyCmServicesStartedSuccessfully(TestContext testContext, DistroXTestDto testDto, CloudbreakClient cloudbreakClient) {
        clouderaManagerUtil.checkCmServicesStartedSuccessfully(testDto, testContext);
        return testDto;
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

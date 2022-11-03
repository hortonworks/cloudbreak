package com.sequenceiq.it.cloudbreak.testcase.e2e.environment;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.PRE_SERVICE_DEPLOYMENT;
import static com.sequenceiq.it.cloudbreak.assertion.freeipa.RecipeTestAssertion.validateFilesOnFreeIpa;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.util.SanitizerUtil;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseAvailabilityType;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.SdxClient;
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
import com.sequenceiq.it.cloudbreak.dto.verticalscale.VerticalScalingTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
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

    private static final String TARGET_INSTANCE_GROUP_TYPE = "master";

    private static final String UPGRADED_FREEIPA_INSTANCE_TYPE = "m5.2xlarge";

    private static final String DEFAULT_DATALAKE_INSTANCE_TYPE = "m5.2xlarge";

    private static final String DEFAULT_DATAHUB_INSTANCE_TYPE = "m5.2xlarge";

    private static final String UPGRADED_DATALAKE_INSTANCE_TYPE = "m5.4xlarge";

    private static final String UPGRADED_DATAHUB_INSTANCE_TYPE = "m5.4xlarge";

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

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT, timeOut = 9000000)
    @Description(
            given = "there is a running cloudbreak",
            when = "create an attached SDX and Datahubs (in case of AWS, create one of the Datahub with external database)",
            then = "should be stopped first and started after it, and required services should be in running state in CM")
    public void testCreateStopStartEnvironment(TestContext testContext) {
        LOGGER.info("Environment stop-start test execution has been started....");
        DistroXDatabaseRequest distroXDatabaseRequest = new DistroXDatabaseRequest();
        distroXDatabaseRequest.setAvailabilityType(DistroXDatabaseAvailabilityType.NON_HA);
        String recipeName = resourcePropertyProvider().getName();
        String filePath = "/pre-service-deployment";
        String fileName = "pre-service-deployment";

        String freeipaVerticalScaleKey = "freeipaVerticalScaleKey";
        String sdxVerticalScaleKey = "sdxVerticalScaleKey";
        String distroxVerticalScaleKey = "distroxVerticalScaleKey";

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
                .given(SdxInternalTestDto.class)
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

                .given(freeipaVerticalScaleKey, VerticalScalingTestDto.class)
                .withFreeipaVerticalScale()
                .withGroup(TARGET_INSTANCE_GROUP_TYPE)
                .withInstanceType(UPGRADED_FREEIPA_INSTANCE_TYPE)
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.verticalScale(freeipaVerticalScaleKey))
                .await(EnvironmentStatus.ENV_STOPPED)

                .given(sdxVerticalScaleKey, VerticalScalingTestDto.class)
                .withSdxVerticalScale()
                .withGroup(TARGET_INSTANCE_GROUP_TYPE)
                .withInstanceType(UPGRADED_DATALAKE_INSTANCE_TYPE)
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.verticalScale(sdxVerticalScaleKey))
                .await(SdxClusterStatusResponse.STOPPED)

                .given(distroxVerticalScaleKey, VerticalScalingTestDto.class)
                .withDistroXVerticalScale()
                .withInstanceType(UPGRADED_DATAHUB_INSTANCE_TYPE)
                .withGroup(TARGET_INSTANCE_GROUP_TYPE)

                .given("dx1", DistroXTestDto.class)
                .when(distroXTestClient.verticalScale(distroxVerticalScaleKey))
                .await(STACK_STOPPED, RunningParameter.key("dx1"))

                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.start())
                .await(EnvironmentStatus.AVAILABLE)
                .given("dx1", DistroXTestDto.class)
                .await(STACK_AVAILABLE, RunningParameter.key("dx1"))
                .awaitForHealthyInstances()
                .then(this::verifyCmServicesStartedSuccessfully)

                // vertical scale validation start:
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .then(this::validateFreeIpaInstanceType)
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.detailedDescribeInternal())
                .then(this::validateDataLakeInstanceType)
                .given("dx1", DistroXTestDto.class)
                .when(distroXTestClient.get())
                .then(this::validateDataHubInstanceType)

                .validate();

        LOGGER.info("Environment stop-start test execution has been finished....");
    }

    private DistroXTestDto verifyCmServicesStartedSuccessfully(TestContext testContext, DistroXTestDto testDto, CloudbreakClient cloudbreakClient) {
        String username = testContext.getActingUserCrn().getResource();
        String sanitizedUserName = SanitizerUtil.sanitizeWorkloadUsername(username);
        clouderaManagerUtil.checkCmServicesStartedSuccessfully(testDto, sanitizedUserName, MOCK_UMS_PASSWORD);
        return testDto;
    }

    private FreeIpaTestDto validateFreeIpaInstanceType(TestContext testContext, FreeIpaTestDto testDto, FreeIpaClient freeIpaClient) {
        validateInstanceType(testDto.findInstanceGroupByType(TARGET_INSTANCE_GROUP_TYPE).getInstanceTemplate().getInstanceType(),
                UPGRADED_FREEIPA_INSTANCE_TYPE, "FreeIPA");
        return testDto;
    }

    public SdxInternalTestDto validateDataLakeInstanceType(TestContext testContext, SdxInternalTestDto testDto, SdxClient sdxClient) {
        validateInstanceType(testDto.findInstanceGroupByName(TARGET_INSTANCE_GROUP_TYPE).getTemplate().getInstanceType(), UPGRADED_DATALAKE_INSTANCE_TYPE,
                "Data Lake");
        return testDto;
    }

    private DistroXTestDto validateDataHubInstanceType(TestContext testContext1, DistroXTestDto testDto, CloudbreakClient client) {
        validateInstanceType(testDto.findInstanceGroupByName(TARGET_INSTANCE_GROUP_TYPE).getTemplate().getInstanceType(), UPGRADED_DATAHUB_INSTANCE_TYPE,
                "Data Hub");
        return testDto;
    }

    private void validateInstanceType(String instanceType, String expectedType, String service) {
        if (!instanceType.equals(expectedType)) {
            throw new TestFailException(String.format(VERTICAL_SCALE_FAIL_MSG_FORMAT, service, expectedType, instanceType));
        }
    }

}

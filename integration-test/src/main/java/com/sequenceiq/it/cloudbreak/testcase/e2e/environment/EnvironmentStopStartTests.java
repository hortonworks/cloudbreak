package com.sequenceiq.it.cloudbreak.testcase.e2e.environment;

import java.time.Duration;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.util.SanitizerUtil;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseAvailabilityType;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.assertion.util.CloudProviderSideTagAssertion;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.clouderamanager.ClouderaManagerUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class EnvironmentStopStartTests extends AbstractE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentStopStartTests.class);

    private static final Map<String, String> ENV_TAGS = Map.of("envTagKey", "envTagValue");

    private static final Map<String, String> SDX_TAGS = Map.of("sdxTagKey", "sdxTagValue");

    private static final Map<String, String> DX1_TAGS = Map.of("distroxTagKey", "distroxTagValue");

    private static final String MOCK_UMS_PASSWORD = "Password123!";

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
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given("telemetry", TelemetryTestDto.class)
                    .withLogging()
                    .withReportClusterLogs()
                .given(EnvironmentTestDto.class)
                    .withNetwork()
                    .withTelemetry("telemetry")
                    .withCreateFreeIpa(Boolean.TRUE)
                    .addTags(ENV_TAGS)
                .when(environmentTestClient.create())
                .given(SdxInternalTestDto.class)
                    .addTags(SDX_TAGS)
                    .withCloudStorage(getCloudStorageRequest(testContext))
                .when(sdxTestClient.createInternal())
                .given(EnvironmentTestDto.class)
                .await(EnvironmentStatus.AVAILABLE)
                .then(cloudProviderSideTagAssertion.verifyEnvironmentTags(ENV_TAGS))
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
                //TODO workaround until CB-15932 has not been implemented
                .waitingFor(Duration.ofMinutes(2), "Waiting for FreeIpa nodes to really be stopped on Gcp too has been interrupted")
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.start())
                .await(EnvironmentStatus.AVAILABLE)
                .given("dx1", DistroXTestDto.class)
                .await(STACK_AVAILABLE, RunningParameter.key("dx1"))
                .awaitForHealthyInstances()
                .then(this::verifyCmServicesStartedSuccessfully)
                .validate();

        LOGGER.info("Environment stop-start test execution has been finished....");
    }

    private DistroXTestDto verifyCmServicesStartedSuccessfully(TestContext testContext, DistroXTestDto testDto, CloudbreakClient cloudbreakClient) {
        String username = testContext.getActingUserCrn().getResource();
        String sanitizedUserName = SanitizerUtil.sanitizeWorkloadUsername(username);
        clouderaManagerUtil.checkCmServicesStartedSuccessfully(testDto, sanitizedUserName, MOCK_UMS_PASSWORD);
        return testDto;
    }
}

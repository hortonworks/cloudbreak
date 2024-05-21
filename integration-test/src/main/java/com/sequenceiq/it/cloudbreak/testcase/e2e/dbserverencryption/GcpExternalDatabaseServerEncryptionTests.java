package com.sequenceiq.it.cloudbreak.testcase.e2e.dbserverencryption;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseServerSslMode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.StackDatabaseServerResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxDatabaseServerAction;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.sdx.PreconditionSdxE2ETest;
import com.sequenceiq.it.cloudbreak.testcase.e2e.sdx.SdxUpgradeDatabaseTestUtil;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.it.cloudbreak.util.ssh.SshJUtil;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

public class GcpExternalDatabaseServerEncryptionTests extends PreconditionSdxE2ETest {

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private SdxUpgradeDatabaseTestUtil sdxUpgradeDatabaseTestUtil;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private SshJUtil sshJUtil;

    @Inject
    private SdxDatabaseServerAction sdxDatabaseServerAction;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak, and an encrypted environment is created on GCP with external database",
            when = "SDX is created with external encrypted database",
            then = "SSL encryption is enabled for GCP, the cluster should be up and running"
    )
    public void testExternalDatabaseEncryptionOnGCP(TestContext testContext) {
        String sdx = resourcePropertyProvider().getName();

        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NON_HA);
        sdxDatabaseRequest.setDatabaseEngineVersion(sdxUpgradeDatabaseTestUtil.getTargetMajorVersion().getMajorVersion());

        testContext
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()
                .given(EnvironmentTestDto.class)
                .withNetwork()
                .withTelemetry("telemetry")
                .withTunnel(testContext.getTunnel())
                .withResourceEncryption()
                .withCreateFreeIpa(Boolean.TRUE)
                .withFreeIpaNodes(getFreeIpaInstanceCountByProdiver(testContext))
                .withFreeIpaImage(commonCloudProperties().getImageValidation().getFreeIpaImageCatalog(),
                        commonCloudProperties().getImageValidation().getFreeIpaImageUuid())
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.describe())
                .given(sdx, SdxTestDto.class)
                .withCloudStorage()
                .withExternalDatabase(sdxDatabaseRequest)
                .when(sdxTestClient.create(), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .awaitForHealthyInstances()
                .given(sdx, SdxTestDto.class)
                .when(sdxTestClient.describe())
                .then((tc, testDto, client) -> {
                    validateEncryption(testDto, tc, client);
                    return testDto;
                })
                .validate();
    }

    private void validateEncryption(SdxTestDto sdxTestDto, TestContext tc, SdxClient sdxClient) throws Exception {
        SdxClusterDetailResponse sdxClusterDetailResponse = sdxTestDto.getResponse();
        StackDatabaseServerResponse stackDatabaseServerResponse = sdxDatabaseServerAction.getSdxExternalDatabaseConfigs(sdxClusterDetailResponse.getCrn(),
                sdxClient);
        if (!DatabaseServerSslMode.ENABLED.equals(stackDatabaseServerResponse.getSslConfig().getSslMode())) {
            throw new TestFailException("SSL Encryption is not enabled for external database for cluster : " + sdxClusterDetailResponse.getName());
        }

        List<InstanceGroupV4Response> instanceGroups = sdxClusterDetailResponse.getStackV4Response().getInstanceGroups();
        Map<String, Pair<Integer, String>> dbSslModeConnectionMap = sshJUtil.getSSLModeForExternalDBByIp(instanceGroups, List.of("master"),
                commonCloudProperties().getDefaultPrivateKeyFile());

        for (String ip : dbSslModeConnectionMap.keySet()) {
            Pair<Integer, String> connectionUrlPair = dbSslModeConnectionMap.get(ip);
            if (!connectionUrlPair.getValue().contains("verify-ca")) {
                throw new TestFailException("Connection URL for external database has SSL Mode set to 'verify-full' for instance ip: " + ip);
            }
        }
    }

    protected CloudFunctionality getCloudFunctionality(TestContext testContext) {
        return testContext.getCloudProvider().getCloudFunctionality();
    }
}

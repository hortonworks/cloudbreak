package com.sequenceiq.it.cloudbreak.testcase.e2e.l0promotion;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS_NATIVE;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretType.CCMV2_JUMPGATE_AGENT_ACCESS_KEY;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static java.lang.String.format;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.HealthDetailsFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.NodeHealthDetails;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaHealthDetailsDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaRotationTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.EnvironmentUtil;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.it.cloudbreak.util.ssh.SshJUtil;
import com.sequenceiq.it.util.imagevalidation.ImageValidatorE2ETest;
import com.sequenceiq.it.util.imagevalidation.ImageValidatorE2ETestUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxRepairRequest;

public class DatalakeCcmUpgradeAndRotationTest extends AbstractE2ETest implements ImageValidatorE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeCcmUpgradeAndRotationTest.class);

    private static final String EXPORT_IS_CCM_V_2_JUMPGATE_ENABLED_TRUE = "export IS_CCM_V2_JUMPGATE_ENABLED=true";

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private EnvironmentUtil environmentUtil;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private SdxUtil sdxUtil;

    @Inject
    private SshJUtil sshJUtil;

    @Inject
    private ImageValidatorE2ETestUtil imageValidatorE2ETestUtil;

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
            given = "There is a running environment with datalake connected via CCMv2",
            when = "CCM Rotation happens",
            then = "Datalake then CCM V2 Jumpgate agent access key rotation should be successful.")
    public void testCcmV2Rotation(TestContext testContext) {
        createEnvironmentWithCcm(testContext, Tunnel.CCMV2_JUMPGATE);
        createSdxForEnvironment(testContext);
        validateCcmServices(testContext, Tunnel.CCMV2_JUMPGATE);
        if (!imageValidatorE2ETestUtil.isImageValidation()) {
            // secret rotation testing is not needed for image validation
            rotateCcmV2JumpgateAgentAccessKey(testContext);
            validateCcmServices(testContext, Tunnel.CCMV2_JUMPGATE);
        }
        repairMasterNodes(testContext);
        validateCcmServices(testContext, Tunnel.CCMV2_JUMPGATE);
    }

    private void createEnvironmentWithCcm(TestContext testContext, Tunnel ccmVersion) {
        environmentUtil
                .createEnvironmentWithDefinedCcm(testContext, ccmVersion)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .when(environmentTestClient.describe())
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.getLastSyncOperationStatus())
                .await(OperationState.COMPLETED)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .validate();
    }

    private void validateCcmServices(TestContext testContext, Tunnel ccmVersion) {
        testContext
                .given(FreeIpaTestDto.class)
                .then((tc, testDto, client) -> {
                    Map<String, Boolean> serviceStatusesByName = Map.of(
                            "ccm-tunnel@GATEWAY", ccmVersion.useCcmV1(),
                            "ccm-tunnel@KNOX", false,
                            "jumpgate-agent", ccmVersion.useCcmV2OrJumpgate()
                    );
                    return sshJUtil.checkSystemctlServiceStatus(testDto, testDto.getEnvironmentCrn(), client, serviceStatusesByName);
                })
                .given(SdxInternalTestDto.class)
                .then((tc, testDto, client) -> {
                    Map<String, Boolean> serviceStatusesByName = Map.of(
                            "ccm-tunnel@GATEWAY", ccmVersion.useCcmV1(),
                            "ccm-tunnel@KNOX", ccmVersion.useCcmV1(),
                            "jumpgate-agent", ccmVersion.useCcmV2()
                    );
                    List<InstanceGroupV4Response> instanceGroups = testDto.getResponse().getStackV4Response().getInstanceGroups();
                    return sshJUtil.checkSystemctlServiceStatus(testDto, instanceGroups, List.of(MASTER.getName()), serviceStatusesByName);
                })
                .validate();
    }

    private void createSdxForEnvironment(TestContext testContext) {
        testContext
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.describe())
                .given(SdxInternalTestDto.class)
                    .withEnvironment()
                .when(sdxTestClient.createInternal())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .validate();
    }

    private void rotateCcmV2JumpgateAgentAccessKey(TestContext testContext) {
        testContext
                .given(FreeIpaRotationTestDto.class)
                    .withSecrets(List.of(CCMV2_JUMPGATE_AGENT_ACCESS_KEY))
                .when(freeIpaTestClient.rotateSecret())
                .awaitForFlow()
                .given(FreeIpaHealthDetailsDto.class)
                    .withEnvironmentCrn()
                .when(freeIpaTestClient.getHealthDetails())
                .then(validateCcmV2JumpgateAgentAccessKeyRotation())
                .validate();
    }

    private static Assertion<FreeIpaHealthDetailsDto, FreeIpaClient> validateCcmV2JumpgateAgentAccessKeyRotation() {
        return (testContext, freeIpaHealthDetailsDto, freeIpaClient) -> {
            HealthDetailsFreeIpaResponse healthDetailsResponse = freeIpaHealthDetailsDto.getResponse();
            if (healthDetailsResponse != null) {
                if (Status.AVAILABLE != healthDetailsResponse.getStatus()) {
                    throw new TestFailException(format("FreeIPA status should be AVAILABLE but actual status is: %s", healthDetailsResponse.getStatus()));
                }
                List<String> unhealthyInstances = healthDetailsResponse.getNodeHealthDetails().stream()
                        .filter(nodeHealthDetails -> !nodeHealthDetails.getStatus().isAvailable())
                        .map(NodeHealthDetails::getInstanceId)
                        .toList();
                if (!unhealthyInstances.isEmpty()) {
                    throw new TestFailException(format("FreeIPA unhealthy instances: %s", unhealthyInstances));
                }
            } else {
                String freeIpaCrn = testContext.get(FreeIpaTestDto.class).getCrn();
                String environmentCrn = testContext.get(EnvironmentTestDto.class).getCrn();
                throw new TestFailException(format("FreeIPA Health Details response is null for freeIpa: %s at environment: %s!", freeIpaCrn, environmentCrn));
            }
            return freeIpaHealthDetailsDto;
        };
    }

    private Assertion<EnvironmentTestDto, EnvironmentClient> validateCcmUpgradeOnEnvironment() {
        return (testContext1, environmentTestDto, environmentClient) -> {
            FreeIpaTestDto freeIpaTestDto = testContext1.get(FreeIpaTestDto.class);
            if (!AWS_NATIVE.equals(freeIpaTestDto.getVariant())) {
                CloudFunctionality cloudFunctionality = testContext1.getCloudProvider().getCloudFunctionality();
                Map<String, String> launchTemplateUserData = cloudFunctionality.getLaunchTemplateUserData(environmentTestDto.getName());
                boolean ccmV2Enabled = launchTemplateUserData.entrySet().stream().allMatch(ud -> {
                    Pattern p = Pattern.compile(EXPORT_IS_CCM_V_2_JUMPGATE_ENABLED_TRUE);
                    Matcher m = p.matcher(ud.getValue());

                    boolean result = m.find();
                    if (!result) {
                        Log.then(LOGGER,
                                format("the %s launch template user data does not contain %s ", ud.getKey(), EXPORT_IS_CCM_V_2_JUMPGATE_ENABLED_TRUE));
                    }
                    return result;
                });
                if (!ccmV2Enabled) {
                    throw new TestFailException(format("user data is not updated by %s", EXPORT_IS_CCM_V_2_JUMPGATE_ENABLED_TRUE));
                }
            }
            return environmentTestDto;
        };
    }

    private void repairMasterNodes(TestContext testContext) {
        testContext
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.describe())
                .await(EnvironmentStatus.AVAILABLE)
                .given(SdxInternalTestDto.class)
                    .withEnvironment()
                .then((tc, testDto, client) -> {
                    String sdxName = testDto.getName();
                    List<String> instancesToRepair = sdxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    LOGGER.info("Gathered instance ids to repair '{}' for SDX with name '{}'", instancesToRepair, sdxName);
                    SdxRepairRequest clusterRepairRequest = new SdxRepairRequest();
                    clusterRepairRequest.setNodesIds(instancesToRepair);
                    LOGGER.debug("Sending repair request to SDX: '{}'", clusterRepairRequest);
                    FlowIdentifier flowIdentifier = client.getDefaultClient().sdxEndpoint().repairCluster(sdxName, clusterRepairRequest);
                    LOGGER.info("Repair with flow id '{}' has been initiated on the SDX with name '{}'", flowIdentifier, sdxName);
                    testDto.setFlow("SDX repair", flowIdentifier);
                    return testDto;
                })
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .validate();
    }
}

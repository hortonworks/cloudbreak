package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.timeoutChecker;

import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.polling.AttemptBasedTimeoutChecker;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.verticalscale.VerticalScalingTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxRollingVerticalScaleTest extends PreconditionSdxE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxRollingVerticalScaleTest.class);

    private static final int SDX_CREATION_MAX_ATTEMPTS = 5000;

    private static final int VERTICAL_SCALING_MAX_ATTEMPTS = 4000;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Test(dataProvider = TEST_CONTEXT, timeOut = 9000000)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak, and an SDX cluster in available state",
            when = "a valid stack vertical scale request is sent with ONE_BY_ONE orchestrator",
            then = "the stack should be vertically scaled with the new instance type and the cluster should be up and running"
    )
    public void testSdxRollingVerticalScale(TestContext testContext) {
        String sdx = resourcePropertyProvider().getName();
        String sdxVerticalScaleKey = "sdxVerticalScaleKey";
        String targetInstanceGroup = "master";

        testContext
                .given(EnvironmentTestDto.class)
                .given(sdx, SdxTestDto.class)
                .withCloudStorage()
                .withClusterShape(SdxClusterShape.ENTERPRISE)
                .withRuntimeVersion(commonClusterManagerProperties.getRuntimeVersion())
                .when(sdxTestClient.create(), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx).withTimeoutChecker(new AttemptBasedTimeoutChecker(SDX_CREATION_MAX_ATTEMPTS)))
                .awaitForHealthyInstances()
                .given(sdx, SdxTestDto.class)
                .then((tc, dto, client) -> {
                    CloudbreakClient cbClient = tc.getMicroserviceClient(CloudbreakClient.class);
                    StackV4Response stackV4Response = cbClient.getDefaultClient(testContext).stackV4Endpoint().getByCrn(0L, dto.getCrn(), Set.of());

                    String currentInstanceType = stackV4Response.getInstanceGroups().stream()
                            .filter(ig -> ig.getName().equals(targetInstanceGroup))
                            .findFirst()
                            .orElseThrow(() -> new TestFailException(
                                    "Instance group '" + targetInstanceGroup + "' not found in stack response"))
                            .getTemplate().getInstanceType();

                    String targetInstanceType = selectTargetInstanceType(currentInstanceType, tc.getCloudPlatform().name().toLowerCase());

                    LOGGER.info("Pre-scaling validation: current='{}', target='{}'", currentInstanceType, targetInstanceType);

                    tc.given(sdxVerticalScaleKey, VerticalScalingTestDto.class)
                            .withGroup(targetInstanceGroup)
                            .withInstanceType(targetInstanceType);

                    return dto;
                })
                .when(sdxTestClient.oneByOneVerticalScaleByCrn(sdxVerticalScaleKey))
                .awaitForFlow(timeoutChecker(new AttemptBasedTimeoutChecker(VERTICAL_SCALING_MAX_ATTEMPTS)))
                .await(SdxClusterStatusResponse.RUNNING,
                        timeoutChecker(new AttemptBasedTimeoutChecker(VERTICAL_SCALING_MAX_ATTEMPTS)).withoutWaitForFlow())
                .awaitForHealthyInstances()
                .then((tc, dto, client) -> {
                    CloudbreakClient cbClient = tc.getMicroserviceClient(CloudbreakClient.class);
                    StackV4Response stackV4Response = cbClient.getDefaultClient(testContext).stackV4Endpoint().getByCrn(0L, dto.getCrn(), Set.of());
                    String finalInstanceType = stackV4Response.getInstanceGroups().stream()
                            .filter(ig -> ig.getName().equals(targetInstanceGroup))
                            .findFirst()
                            .orElseThrow(() -> new TestFailException(
                                    "Instance group '" + targetInstanceGroup + "' not found in stack response"))
                            .getTemplate().getInstanceType();

                    String expectedInstanceType = tc.get(sdxVerticalScaleKey, VerticalScalingTestDto.class).getInstanceType();

                    if (!finalInstanceType.equals(expectedInstanceType)) {
                        throw new TestFailException("Vertical scaling failed. Expected: " + expectedInstanceType + ", Actual: " + finalInstanceType);
                    }

                    LOGGER.info("Post-scaling validation successful: final instance type='{}'", finalInstanceType);
                    return dto;
                })
                .validate();
    }

    private String selectTargetInstanceType(String currentInstanceType, String cloudPlatform) {
        String targetInstanceType = switch (cloudPlatform) {
            case "aws" -> selectAwsTargetInstanceType(currentInstanceType);
            case "azure" -> selectAzureTargetInstanceType(currentInstanceType);
            case "gcp" -> selectGcpTargetInstanceType(currentInstanceType);
            default -> throw new TestFailException("Custom instanceType has no value for cloud platform: " + cloudPlatform);
        };

        LOGGER.info("Selected target instance type '{}' for current '{}' on platform '{}'",
                targetInstanceType, currentInstanceType, cloudPlatform);
        return targetInstanceType;
    }

    private String selectAwsTargetInstanceType(String currentInstanceType) {
        return switch (currentInstanceType) {
            case "m5.xlarge" -> "m5.2xlarge";
            default -> {
                if (currentInstanceType.startsWith("m5")) {
                    yield "r5.4xlarge";
                } else if (currentInstanceType.startsWith("r5")) {
                    yield "m5.4xlarge";
                } else {
                    yield "m5.4xlarge";
                }
            }
        };
    }

    private String selectAzureTargetInstanceType(String currentInstanceType) {
        return switch (currentInstanceType) {
            case "Standard_D2s_v3" -> "Standard_D4s_v3";
            case "Standard_D4s_v3" -> "Standard_D8s_v3";
            case "Standard_D8s_v3" -> "Standard_E8s_v3";
            case "Standard_D16s_v3" -> "Standard_E16s_v3";
            case "Standard_E2s_v3" -> "Standard_E4s_v3";
            case "Standard_E4s_v3" -> "Standard_E8s_v3";
            case "Standard_E8s_v3" -> "Standard_D8s_v3";
            case "Standard_E16s_v3" -> "Standard_D16s_v3";
            default -> {
                if (currentInstanceType.startsWith("Standard_D")) {
                    yield "Standard_E8s_v3";
                } else if (currentInstanceType.startsWith("Standard_E")) {
                    yield "Standard_D8s_v3";
                } else {
                    yield "Standard_D8s_v3";
                }
            }
        };
    }

    private String selectGcpTargetInstanceType(String currentInstanceType) {
        return switch (currentInstanceType) {
            case "n2-standard-2" -> "n2-standard-4";
            case "n2-standard-4" -> "n2-standard-8";
            case "n2-standard-8" -> "n2-highmem-8";
            case "n2-standard-16" -> "n2-highmem-16";
            case "n2-highmem-2" -> "n2-highmem-4";
            case "n2-highmem-4" -> "n2-highmem-8";
            case "n2-highmem-8" -> "n2-standard-8";
            case "n2-highmem-16" -> "n2-standard-16";
            default -> {
                if (currentInstanceType.startsWith("n2-standard")) {
                    yield "n2-highmem-8";
                } else if (currentInstanceType.startsWith("n2-highmem")) {
                    yield "n2-standard-8";
                } else {
                    yield "n2-standard-8";
                }
            }
        };
    }
}

package com.sequenceiq.it.cloudbreak.testcase.e2e.freeipa;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaDownscaleTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUpscaleTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;

public class FreeIpaMultiAzScalingTests extends AbstractE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaScalingTests.class);

    private static final Status FREEIPA_AVAILABLE = Status.AVAILABLE;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid MultiAz stack create request is sent with 1 FreeIPA instance " +
                    "AND the MultiAz stack is scaled up to HA " +
                    "AND the MultiAz stack is scaled down to TWO_NODE_BASED " +
                    "AND the MultiAz stack is scaled up to HA ",
            then = "the MultiAz stack should be available AND deletable and have 3 nodes AND the primary gateway should not change")
    public void testMultiAzFreeIpaUpAndDownscale(TestContext testContext) {
        String freeIpa = resourcePropertyProvider().getName();
        Set<String> primaryGatewayInstanceId = new HashSet<>();
        testContext
                .given(freeIpa, FreeIpaTestDto.class)
                .withTelemetry("telemetry")
                .withEnableMultiAz(true)
                .when(freeIpaTestClient.create(), key(freeIpa))
                .await(FREEIPA_AVAILABLE)
                .then((tc, testDto, client) -> {
                    Set<InstanceMetaDataResponse> instanceMetaDataResponses = getInstanceMetaDataResponses(testDto.getRequest().getEnvironmentCrn(), client);
                    primaryGatewayInstanceId.add(getPrimaryGatewayId(instanceMetaDataResponses));
                    return testDto;
                })
                .given(FreeIpaUpscaleTestDto.class)
                .withAvailabilityType(AvailabilityType.HA)
                .when(freeIpaTestClient.upscale(), key(freeIpa))
                .await(FREEIPA_AVAILABLE)
                .then((tc, testDto, client) -> {
                    Set<InstanceMetaDataResponse> instanceMetaDataResponses = getInstanceMetaDataResponses(testDto.getRequest().getEnvironmentCrn(), client);
                    assertInstanceCount(testDto.getRequest().getTargetAvailabilityType(), instanceMetaDataResponses);
                    assertPrimaryGatewayHasNotChanged(primaryGatewayInstanceId, instanceMetaDataResponses);
                    validateMultiAz(testDto.getRequest().getEnvironmentCrn(), client, OperationType.UPSCALE);
                    return testDto;
                })

                .given(FreeIpaDownscaleTestDto.class)
                .withAvailabilityType(AvailabilityType.TWO_NODE_BASED)
                .when(freeIpaTestClient.downscale())
                .await(FREEIPA_AVAILABLE)
                .then((tc, testDto, client) -> {
                    Set<InstanceMetaDataResponse> instanceMetaDataResponses = getInstanceMetaDataResponses(testDto.getRequest().getEnvironmentCrn(), client);
                    assertInstanceCount(testDto.getRequest().getTargetAvailabilityType(), instanceMetaDataResponses);
                    assertPrimaryGatewayHasNotChanged(primaryGatewayInstanceId, instanceMetaDataResponses);
                    validateMultiAz(testDto.getRequest().getEnvironmentCrn(), client, OperationType.DOWNSCALE);
                    return testDto;
                })

                .given(FreeIpaUpscaleTestDto.class)
                .withAvailabilityType(AvailabilityType.HA)
                .when(freeIpaTestClient.upscale(), key(freeIpa))
                .await(FREEIPA_AVAILABLE)
                .then((tc, testDto, client) -> {
                    Set<InstanceMetaDataResponse> instanceMetaDataResponses = getInstanceMetaDataResponses(testDto.getRequest().getEnvironmentCrn(), client);
                    assertInstanceCount(testDto.getRequest().getTargetAvailabilityType(), instanceMetaDataResponses);
                    assertPrimaryGatewayHasNotChanged(primaryGatewayInstanceId, instanceMetaDataResponses);
                    validateMultiAz(testDto.getRequest().getEnvironmentCrn(), client, OperationType.UPSCALE);
                    return testDto;
                })
                .validate();
    }

    @Override
    protected void createDefaultEnvironment(TestContext testContext) {
        testContext
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()
                .given(EnvironmentTestDto.class)
                .withTelemetry("telemetry")
                .withCreateFreeIpa(Boolean.FALSE)
                .withEnableMultiAzFreeIpa()
                .when(getEnvironmentTestClient().create())
                .await(EnvironmentStatus.AVAILABLE)
                .when(getEnvironmentTestClient().describe())
                .validate();
    }

    private String getPrimaryGatewayId(Set<InstanceMetaDataResponse> instanceMetaDataResponses) {
        Optional<InstanceMetaDataResponse> primaryGatewayOptional = instanceMetaDataResponses.stream()
                .filter(imd -> InstanceMetadataType.GATEWAY_PRIMARY.equals(imd.getInstanceType())).findFirst();
        if (primaryGatewayOptional.isEmpty()) {
            String message = "Freeipa does not have a primary gateway";
            LOGGER.error(message);
            throw new TestFailException(message);
        }
        return primaryGatewayOptional.get().getInstanceId();
    }

    private Set<InstanceMetaDataResponse> getInstanceMetaDataResponses(String environmentCrn, FreeIpaClient client) {
        DescribeFreeIpaResponse describeFreeIpaResponse = client.getDefaultClient().getFreeIpaV1Endpoint().describe(environmentCrn);
        Set<InstanceMetaDataResponse> instanceMetaDataResponses = describeFreeIpaResponse.getInstanceGroups().stream()
                .map(InstanceGroupResponse::getMetaData)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
        return instanceMetaDataResponses;
    }

    private void assertPrimaryGatewayHasNotChanged(Set<String> originalPrimaryGatewayInstanceId, Set<InstanceMetaDataResponse> instanceMetaDataResponses) {
        String currentPrimaryGatewayInstanceId = getPrimaryGatewayId(instanceMetaDataResponses);
        if (!originalPrimaryGatewayInstanceId.contains(currentPrimaryGatewayInstanceId)) {
            String message = String.format("Freeipa primary gateway instance id has changed: %s => %s.",
                    originalPrimaryGatewayInstanceId, currentPrimaryGatewayInstanceId);
            LOGGER.error(message);
            throw new TestFailException(message);
        }
    }

    private void assertInstanceCount(AvailabilityType availabilityType, Set<InstanceMetaDataResponse> imd) {
        if (availabilityType.getInstanceCount() != imd.size()) {
            String message = String.format("Expected number of freeipa instances %s do not match the actual instance count %s ",
                    availabilityType.getInstanceCount(), imd.size());
            LOGGER.error(message);
            throw new TestFailException(message);
        }
    }

    private void validateMultiAz(String environmentCrn, FreeIpaClient client, OperationType operationType) {
        DescribeFreeIpaResponse freeIpaResponse = client.getDefaultClient().getFreeIpaV1Endpoint().describe(environmentCrn);
        if (!freeIpaResponse.isEnableMultiAz()) {
            throw new TestFailException(String.format("MultiAz is not enabled for %s after %s", freeIpaResponse.getName(),
                    operationType.getLowerCaseName()));
        }
        List<String> instancesWithNoAz = freeIpaResponse.getInstanceGroups().stream()
                .map(ig -> ig.getMetaData())
                .filter(Objects::nonNull)
                .flatMap(ins -> ins.stream())
                .filter(igm1 -> igm1.getAvailabilityZone() == null)
                .map(igm2 -> igm2.getInstanceId())
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(instancesWithNoAz)) {
            throw new TestFailException(String.format("Availability Zones is missing for instances %s in %s after %s",
                    instancesWithNoAz.stream().collect(Collectors.joining(",")), freeIpaResponse.getName(),
                    operationType.getLowerCaseName()));
        }
    }

}

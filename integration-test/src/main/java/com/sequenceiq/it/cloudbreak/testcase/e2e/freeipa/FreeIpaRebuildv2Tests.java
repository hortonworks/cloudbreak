package com.sequenceiq.it.cloudbreak.testcase.e2e.freeipa;

import static com.sequenceiq.freeipa.api.v1.operation.model.OperationState.COMPLETED;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.waitForFlow;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaOperationStatusTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUpscaleTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;

public class FreeIpaRebuildv2Tests extends AbstractE2ETest {

    protected static final Status FREEIPA_AVAILABLE = Status.AVAILABLE;

    protected static final Status FREEIPA_DELETE_COMPLETED = Status.DELETE_COMPLETED;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        initializeTest(testContext);
        createEnvironment(testContext, Boolean.FALSE, 1);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid stack create request is sent with 2 FreeIPA instances " +
                    "AND the stack is deleted " +
                    "AND the stack is rebuilt",
            then = "the stack should be available AND deletable")
    public void testRebuildv2FreeIpaWithTwoInstances(TestContext testContext) {
        String freeIpa = resourcePropertyProvider().getName();

        int instanceGroupCount = 1;
        int instanceCountByGroup = 2;

        testContext
                .given(freeIpa, FreeIpaTestDto.class)
                    .withFreeIpaHa(instanceGroupCount, instanceCountByGroup)
                    .withTelemetry("telemetry")
                .when(freeIpaTestClient.create(), key(freeIpa))
                .await(FREEIPA_AVAILABLE)
                .then((tc, testDto, client) -> {
                    validateAz(testDto.getRequest().getEnvironmentCrn(), client, tc);
                    return testDto;
                })
                .when(freeIpaTestClient.rebuildv2())
                .await(Status.REBUILD_IN_PROGRESS, waitForFlow().withWaitForFlow(Boolean.FALSE))
                .given(FreeIpaOperationStatusTestDto.class)
                .await(COMPLETED)
                .given(freeIpa, FreeIpaTestDto.class)
                .await(FREEIPA_AVAILABLE)
                .then((tc, testDto, client) -> {
                    validateAz(testDto.getRequest().getEnvironmentCrn(), client, tc);
                    return testDto;
                })
                .given(FreeIpaUpscaleTestDto.class)
                .withAvailabilityType(AvailabilityType.TWO_NODE_BASED)
                .when(freeIpaTestClient.upscale(), key(freeIpa))
                .given(FreeIpaOperationStatusTestDto.class).withOperationId(testContext.get(FreeIpaUpscaleTestDto.class).getOperationId())
                .await(COMPLETED)
                .given(freeIpa, FreeIpaTestDto.class)
                .then((tc, testDto, client) -> {
                    validateAz(testDto.getRequest().getEnvironmentCrn(), client, tc);
                    return testDto;
                })
                .then((tc, testDto, client) -> freeIpaTestClient.delete().action(tc, testDto, client))
                .await(FREEIPA_DELETE_COMPLETED)
                .validate();
    }

    private void validateAz(String environmentCrn, FreeIpaClient client, TestContext tc) {
        if (tc.getCloudPlatform() == CloudPlatform.GCP) {
            DescribeFreeIpaResponse freeIpaResponse = client.getDefaultClient().getFreeIpaV1Endpoint().describe(environmentCrn);
            Map<String, String> instanceZoneMap = freeIpaResponse.getInstanceGroups().stream()
                    .map(ig -> ig.getMetaData())
                    .filter(Objects::nonNull)
                    .flatMap(ins -> ins.stream())
                    .collect(Collectors.toMap(InstanceMetaDataResponse::getInstanceId, InstanceMetaDataResponse::getAvailabilityZone));
            Map<String, String> availabilityZoneForVms = getCloudFunctionality(tc).listAvailabilityZonesForVms(freeIpaResponse.getName(), instanceZoneMap);
            List<String> instancesWithNoAz = availabilityZoneForVms.entrySet().stream()
                    .filter(entry -> StringUtils.isEmpty(entry.getValue()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(instancesWithNoAz)) {
                throw new TestFailException(String.format("Availability Zone is missing for instances %s in %s",
                        instancesWithNoAz.stream().collect(Collectors.joining(",")), freeIpaResponse.getName()));
            }
            Set<String> zones = availabilityZoneForVms.values().stream().collect(Collectors.toSet());
            if (zones.size() > 1) {
                throw new TestFailException(String.format("There are multiple Availability zones %s for instances in %s",
                        zones.stream().collect(Collectors.joining(",")), freeIpaResponse.getName()));
            }
        }
    }

    private CloudFunctionality getCloudFunctionality(TestContext testContext) {
        return testContext.getCloudProvider().getCloudFunctionality();
    }
}

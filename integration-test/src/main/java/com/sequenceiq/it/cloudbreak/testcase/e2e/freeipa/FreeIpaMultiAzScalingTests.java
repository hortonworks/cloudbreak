package com.sequenceiq.it.cloudbreak.testcase.e2e.freeipa;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
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
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaDownscaleTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUpscaleTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;

public class FreeIpaMultiAzScalingTests extends AbstractE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaScalingTests.class);

    private static final Status FREEIPA_AVAILABLE = Status.AVAILABLE;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        initializeTest(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid MultiAz stack create request is sent with 1 FreeIPA instance " +
                    "AND the MultiAz stack is scaled up to HA " +
                    "AND the MultiAz stack is scaled down to TWO_NODE_BASED " +
                    "AND the MultiAz stack is scaled up to HA ",
            then = "the MultiAz stack should be available AND deletable and have 3 nodes AND the primary gateway should not change")
    public void testMultiAzFreeIpaUpAndDownscale(TestContext testContext) {
        Set<String> primaryGatewayInstanceId = new HashSet<>();
        setUpEnvironmentTestDto(testContext, Boolean.TRUE, 1)
                .withEnableMultiAzFreeIpa()
                .when(getEnvironmentTestClient().create())
                .await(EnvironmentStatus.AVAILABLE)
                .when(getEnvironmentTestClient().describe())
                .given(FreeIpaTestDto.class)
                .when(getFreeIpaTestClient().describe())
                .await(FREEIPA_AVAILABLE)
                .then((tc, testDto, client) -> {
                    Set<InstanceMetaDataResponse> instanceMetaDataResponses = getInstanceMetaDataResponses(testDto.getRequest().getEnvironmentCrn(), client);
                    primaryGatewayInstanceId.add(getPrimaryGatewayId(instanceMetaDataResponses));
                    return testDto;
                })
                .given(FreeIpaUpscaleTestDto.class)
                .withAvailabilityType(AvailabilityType.HA)
                .when(freeIpaTestClient.upscale())
                .await(FREEIPA_AVAILABLE)
                .then((tc, testDto, client) -> {
                    Set<InstanceMetaDataResponse> instanceMetaDataResponses = getInstanceMetaDataResponses(testDto.getRequest().getEnvironmentCrn(), client);
                    assertInstanceCount(testDto.getRequest().getTargetAvailabilityType(), instanceMetaDataResponses);
                    assertPrimaryGatewayHasNotChanged(primaryGatewayInstanceId, instanceMetaDataResponses);
                    validateMultiAz(testDto.getRequest().getEnvironmentCrn(), client, testDto.getName(), tc, OperationType.UPSCALE);
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
                    validateMultiAz(testDto.getRequest().getEnvironmentCrn(), client, testDto.getName(), tc, OperationType.DOWNSCALE);
                    return testDto;
                })

                .given(FreeIpaUpscaleTestDto.class)
                .withAvailabilityType(AvailabilityType.HA)
                .when(freeIpaTestClient.upscale())
                .await(FREEIPA_AVAILABLE)
                .then((tc, testDto, client) -> {
                    Set<InstanceMetaDataResponse> instanceMetaDataResponses = getInstanceMetaDataResponses(testDto.getRequest().getEnvironmentCrn(), client);
                    assertInstanceCount(testDto.getRequest().getTargetAvailabilityType(), instanceMetaDataResponses);
                    assertPrimaryGatewayHasNotChanged(primaryGatewayInstanceId, instanceMetaDataResponses);
                    validateMultiAz(testDto.getRequest().getEnvironmentCrn(), client, testDto.getName(), tc, OperationType.UPSCALE);
                    return testDto;
                })
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

    private void validateMultiAz(String environmentCrn, FreeIpaClient client, String freeIpa, TestContext tc, OperationType operationType) {
        DescribeFreeIpaResponse freeIpaResponse = client.getDefaultClient().getFreeIpaV1Endpoint().describe(environmentCrn);
        if (!freeIpaResponse.isEnableMultiAz()) {
            throw new TestFailException(String.format("MultiAz is not enabled for %s after %s", freeIpaResponse.getName(), operationType.getLowerCaseName()));
        }
        for (InstanceGroupResponse instanceGroup : freeIpaResponse.getInstanceGroups()) {
            if (!CollectionUtils.isEmpty(instanceGroup.getMetaData())) {
                Map<String, String> instanceZoneMap = instanceGroup.getMetaData().stream()
                        .collect(Collectors.toMap(InstanceMetaDataResponse::getInstanceId, InstanceMetaDataResponse::getAvailabilityZone));
                validateMultiAzDistribution(freeIpa, tc, operationType, instanceZoneMap, instanceGroup.getName());
            }
        }
    }

    private void validateMultiAzDistribution(String freeIpa, TestContext tc, OperationType operationType, Map<String, String> instanceZoneMap,
            String hostGroup) {
        Map<String, String> availabilityZoneForVms = getCloudFunctionality(tc).listAvailabilityZonesForVms(freeIpa, instanceZoneMap);
        LOGGER.info("Availability Zone for Vms {}", availabilityZoneForVms);
        List<String> instancesWithNoAz = instanceZoneMap.keySet().stream().filter(instance -> StringUtils.isEmpty(availabilityZoneForVms.get(instance)))
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(instancesWithNoAz)) {
            throw new TestFailException(String.format("Availability Zones is missing for instances %s in %s",
                    String.join(",", instancesWithNoAz), freeIpa));
        }
        Map<String, Integer> zoneToNodeCountMap = new HashMap<>();
        for (Map.Entry<String, String> entry : availabilityZoneForVms.entrySet()) {
            zoneToNodeCountMap.put(entry.getValue(), zoneToNodeCountMap.getOrDefault(entry.getValue(), 0) + 1);
        }
        LOGGER.info("Zone to Node count {} after  {}", zoneToNodeCountMap, operationType);
        int numInstances = instanceZoneMap.size();
        int numZones = zoneToNodeCountMap.size();
        int numZonesWithDesiredNumInstances;
        if (instanceZoneMap.size() >= zoneToNodeCountMap.size()) {
            numZonesWithDesiredNumInstances = countZonesWithDesiredNumberOfInstances(zoneToNodeCountMap, numInstances / numZones);
            if (numZones - numInstances % numZones != numZonesWithDesiredNumInstances) {
                throw new TestFailException(String.format("Distribution of nodes in AZs is not correct in host group: %s after %s for %s." +
                                "There are %s instance and %s zones.Number of Zones with number of instances %s should be %s but is %s",
                        hostGroup, operationType, freeIpa, numInstances, numZones, numInstances / numZones, numZones - numInstances % numZones,
                        numZonesWithDesiredNumInstances));
            }
        }
        numZonesWithDesiredNumInstances = countZonesWithDesiredNumberOfInstances(zoneToNodeCountMap, numInstances / numZones + 1);
        if (numInstances % numZones != numZonesWithDesiredNumInstances) {
            throw new TestFailException(String.format("Distribution of nodes in AZs is not correct in host group: %s after %s for %s." +
                            "There are %s instance and %s zones.Number of Zones with number of instances %s should be %s but is %s",
                    hostGroup, operationType, freeIpa, numInstances, numZones, numInstances / numZones + 1, numInstances % numZones,
                    numZonesWithDesiredNumInstances));
        }
    }

    private int countZonesWithDesiredNumberOfInstances(Map<String, Integer> zoneToNodeCountMap, int desiredCount) {
        return (int) zoneToNodeCountMap.entrySet().stream()
                .filter(entry -> entry.getValue() == desiredCount).count();
    }

    private CloudFunctionality getCloudFunctionality(TestContext testContext) {
        return testContext.getCloudProvider().getCloudFunctionality();
    }

}

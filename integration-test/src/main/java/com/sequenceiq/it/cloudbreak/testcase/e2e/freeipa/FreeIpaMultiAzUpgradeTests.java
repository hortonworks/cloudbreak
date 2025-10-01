package com.sequenceiq.it.cloudbreak.testcase.e2e.freeipa;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.waitForFlow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;

public class FreeIpaMultiAzUpgradeTests extends AbstractE2ETest {

    protected static final Status FREEIPA_AVAILABLE = Status.AVAILABLE;

    protected static final Status FREEIPA_DELETE_COMPLETED = Status.DELETE_COMPLETED;

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaMultiAzUpgradeTests.class);

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        initializeTest(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid MultiAz stack create request is sent with 3 FreeIPA instances " +
                    "AND the MultiAz stack is upgraded one node at a time",
            then = "the MultiAz stack should be available AND deletable")
    public void testHAFreeIpaMultiAzInstanceUpgrade(TestContext testContext) {

        setUpEnvironmentTestDto(testContext, Boolean.TRUE, 3)
                .withEnableMultiAzFreeIpa()
                .withFreeIpaImage(testContext.getCloudProvider().getFreeIpaUpgradeImageCatalog(), testContext.getCloudProvider()
                        .getFreeIpaUpgradeImageId())
                .when(getEnvironmentTestClient().create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .when(freeIpaTestClient.upgrade())
                .await(FREEIPA_AVAILABLE)
                .then((tc, testDto, client) -> {
                    validateMultiAz(testDto.getRequest().getEnvironmentCrn(), client, testDto.getName(), tc);
                    return testDto;
                })
                .then((tc, testDto, client) -> freeIpaTestClient.delete().action(tc, testDto, client))
                .await(FREEIPA_DELETE_COMPLETED, waitForFlow().withWaitForFlow(Boolean.FALSE))
                .validate();
    }

    private void validateMultiAz(String environmentCrn, FreeIpaClient client, String freeIpa, TestContext tc) {
        DescribeFreeIpaResponse freeIpaResponse = client.getDefaultClient().getFreeIpaV1Endpoint().describe(environmentCrn);
        if (!freeIpaResponse.isEnableMultiAz()) {
            throw new TestFailException(String.format("MultiAz is not enabled for %s after upgrade", freeIpaResponse.getName()));
        }
        for (InstanceGroupResponse instanceGroup : freeIpaResponse.getInstanceGroups()) {
            if (!CollectionUtils.isEmpty(instanceGroup.getMetaData())) {
                Map<String, String> instanceZoneMap = instanceGroup.getMetaData().stream()
                        .collect(Collectors.toMap(InstanceMetaDataResponse::getInstanceId, InstanceMetaDataResponse::getAvailabilityZone));
                validateMultiAzDistribution(freeIpa, tc, instanceZoneMap, instanceGroup.getName());
            }
        }
    }

    private void validateMultiAzDistribution(String freeIpa, TestContext tc, Map<String, String> instanceZoneMap,
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
        LOGGER.info("Zone to Node count {} after upgrade", zoneToNodeCountMap);
        int numInstances = instanceZoneMap.size();
        int numZones = zoneToNodeCountMap.size();
        int numZonesWithDesiredNumInstances;
        if (instanceZoneMap.size() >= zoneToNodeCountMap.size()) {
            numZonesWithDesiredNumInstances = countZonesWithDesiredNumberOfInstances(zoneToNodeCountMap, numInstances / numZones);
            if (numZones - numInstances % numZones != numZonesWithDesiredNumInstances) {
                throw new TestFailException(String.format("Distribution of nodes in AZs is not correct in host group: %s after upgrade for %s." +
                                "There are %s instance and %s zones.Number of Zones with number of instances %s should be %s but is %s",
                        hostGroup, freeIpa, numInstances, numZones, numInstances / numZones, numZones - numInstances % numZones,
                        numZonesWithDesiredNumInstances));
            }
        }
        numZonesWithDesiredNumInstances = countZonesWithDesiredNumberOfInstances(zoneToNodeCountMap, numInstances / numZones + 1);
        if (numInstances % numZones != numZonesWithDesiredNumInstances) {
            throw new TestFailException(String.format("Distribution of nodes in AZs is not correct in host group: %s after upgrade for %s." +
                            "There are %s instance and %s zones.Number of Zones with number of instances %s should be %s but is %s",
                    hostGroup, freeIpa, numInstances, numZones, numInstances / numZones + 1, numInstances % numZones,
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

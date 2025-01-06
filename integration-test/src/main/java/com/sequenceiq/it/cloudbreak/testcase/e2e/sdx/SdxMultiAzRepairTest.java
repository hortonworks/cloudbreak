package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

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

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxMultiAzRepairTest extends PreconditionSdxE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxMultiAzRepairTest.class);

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Inject
    private SdxUtil sdxUtil;

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak",
            when = "MultiAz data lake is provisioned followed by repair operation",
            then = "distribution of nodes across AZs should be even after provisioning and repair"
    )
    public void testSDXMultiAzRepair(TestContext testContext) {
        String sdx = resourcePropertyProvider().getName();

        testContext
                .given(sdx, SdxTestDto.class)
                .withName(sdx)
                .withCloudStorage()
                .withClusterShape(SdxClusterShape.ENTERPRISE)
                .withRuntimeVersion(commonClusterManagerProperties.getRuntimeVersion())
                .withEnableMultiAz(true)
                .when(sdxTestClient.create(), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .awaitForHealthyInstances()
                .given(sdx, SdxTestDto.class)
                .when(sdxTestClient.describe(), key(sdx))
                .then((tc, testDto, client) -> {
                    validateMultiAz(testDto, tc, "provisioning");
                    return testDto;
                })
                .then((tc, testDto, client) -> {
                    Map<String, String> instancesToDelete = sdxUtil.getInstancesWithAz(testDto, client, MASTER.getName());
                    getCloudFunctionality(tc).deleteInstances(testDto.getName(), instancesToDelete);
                    return testDto;
                })
                .awaitForHostGroups(List.of(MASTER.getName()), InstanceStatus.DELETED_ON_PROVIDER_SIDE)
                .when(sdxTestClient.repair(MASTER.getName()), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .given(sdx, SdxTestDto.class)
                .when(sdxTestClient.describe(), key(sdx))
                .then((tc, testDto, client) -> {
                    validateMultiAz(testDto, tc, "repair");
                    return testDto;
                })
                .validate();
    }

    private void validateMultiAz(SdxTestDto sdxTestDto, TestContext tc, String operation) {
        SdxClusterDetailResponse sdxClusterDetailResponse = sdxTestDto.getResponse();
        if (!sdxClusterDetailResponse.isEnableMultiAz()) {
            throw new TestFailException(String.format("MultiAz is not enabled for %s", sdxClusterDetailResponse.getName()));
        }
        for (InstanceGroupV4Response instanceGroup : sdxClusterDetailResponse.getStackV4Response().getInstanceGroups()) {
            if (!CollectionUtils.isEmpty(instanceGroup.getMetadata())) {
                Map<String, String> instanceZoneMap = instanceGroup.getMetadata().stream()
                        .collect(Collectors.toMap(InstanceMetaDataV4Response::getInstanceId, InstanceMetaDataV4Response::getAvailabilityZone));
                validateMultiAzDistribution(sdxClusterDetailResponse.getName(), tc, operation, instanceZoneMap, instanceGroup.getName());
            }
        }
    }

    private void validateMultiAzDistribution(String dataLakeName, TestContext tc, String operation, Map<String, String> instanceZoneMap,
            String hostGroup) {
        Map<String, String> availabilityZoneForVms = getCloudFunctionality(tc).listAvailabilityZonesForVms(dataLakeName, instanceZoneMap);
        LOGGER.info("Availability Zone for Vms {}", availabilityZoneForVms);
        List<String> instancesWithNoAz = instanceZoneMap.keySet().stream().filter(instance -> StringUtils.isEmpty(availabilityZoneForVms.get(instance)))
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(instancesWithNoAz)) {
            throw new TestFailException(String.format("Availability Zones is missing for instances %s in %s",
                    String.join(",", instancesWithNoAz), dataLakeName));
        }
        Map<String, Integer> zoneToNodeCountMap = new HashMap<>();
        for (Map.Entry<String, String> entry : availabilityZoneForVms.entrySet()) {
            zoneToNodeCountMap.put(entry.getValue(), zoneToNodeCountMap.getOrDefault(entry.getValue(), 0) + 1);
        }
        LOGGER.info("Zone to Node count {} after  {}", zoneToNodeCountMap, operation);
        int numInstances = instanceZoneMap.size();
        int numZones = zoneToNodeCountMap.size();
        int numZonesWithDesiredNumInstances;
        if (instanceZoneMap.size() >= zoneToNodeCountMap.size()) {
            numZonesWithDesiredNumInstances = countZonesWithDesiredNumberOfInstances(zoneToNodeCountMap, numInstances / numZones);
            if (numZones - numInstances % numZones != numZonesWithDesiredNumInstances) {
                throw new TestFailException(String.format("Distribution of nodes in AZs is not correct in host group: %s after %s for %s." +
                                "There are %s instance and %s zones.Number of Zones with number of instances %s should be %s but is %s",
                        hostGroup, operation, dataLakeName, numInstances, numZones, numInstances / numZones, numZones - numInstances % numZones,
                        numZonesWithDesiredNumInstances));
            }
        }
        numZonesWithDesiredNumInstances = countZonesWithDesiredNumberOfInstances(zoneToNodeCountMap, numInstances / numZones + 1);
        if (numInstances % numZones != numZonesWithDesiredNumInstances) {
            throw new TestFailException(String.format("Distribution of nodes in AZs is not correct in host group: %s after %s for %s." +
                            "There are %s instance and %s zones.Number of Zones with number of instances %s should be %s but is %s",
                    hostGroup, operation, dataLakeName, numInstances, numZones, numInstances / numZones + 1, numInstances % numZones,
                    numZonesWithDesiredNumInstances));
        }
    }

    private int countZonesWithDesiredNumberOfInstances(Map<String, Integer> zoneToNodeCountMap, int desiredCount) {
        return (int) zoneToNodeCountMap.entrySet().stream()
                .filter(entry -> entry.getValue() == desiredCount).count();
    }

    protected CloudFunctionality getCloudFunctionality(TestContext testContext) {
        return testContext.getCloudProvider().getCloudFunctionality();
    }
}

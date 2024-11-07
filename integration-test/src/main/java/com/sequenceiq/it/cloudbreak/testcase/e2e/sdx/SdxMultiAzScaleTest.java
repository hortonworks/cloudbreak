package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxScaleTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxMultiAzScaleTest extends PreconditionSdxE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxMultiAzScaleTest.class);

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak",
            when = "MultiAz data lake is provisioned followed by upscale and downscale operation",
            then = "distribution of nodes across AZs should be even after provisioning, upscale and downscale"
    )
    public void testSDXMultiAzScaling(TestContext testContext) {
        String sdx = resourcePropertyProvider().getName();

        testContext
                .given(sdx, SdxTestDto.class)
                .withName(sdx)
                .withCloudStorage()
                .withClusterShape(SdxClusterShape.ENTERPRISE)
                .withRuntimeVersion(commonClusterManagerProperties.getRuntimeVersion())
                .withEnableMultiAz(true)
                .when(sdxTestClient.create(), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx).withPollingInterval(Duration.ofMinutes(60L)))
                .awaitForHealthyInstances()
                .given(sdx, SdxTestDto.class)
                .when(sdxTestClient.describe(), key(sdx))
                .then((tc, testDto, client) -> {
                    validateMultiAz(testDto, tc, "provisioning");
                    return testDto;
                })
                .given(SdxScaleTestDto.class)
                .withName(sdx)
                .withGroup("hms_scale_out")
                .withDesiredCount(5)
                .when(sdxTestClient.scale())
                .await(SdxClusterStatusResponse.RUNNING, emptyRunningParameter().withPollingInterval(Duration.ofMinutes(60L)))
                .given(sdx, SdxTestDto.class)
                .when(sdxTestClient.describe(), key(sdx))
                .then((tc, testDto, client) -> {
                    validateMultiAz(testDto, tc, "Upscale");
                    return testDto;
                })
                .given(SdxScaleTestDto.class)
                .withName(sdx)
                .withGroup("hms_scale_out")
                .withDesiredCount(3)
                .when(sdxTestClient.scale())
                .await(SdxClusterStatusResponse.RUNNING, emptyRunningParameter().withPollingInterval(Duration.ofMinutes(60L)))
                .given(sdx, SdxTestDto.class)
                .when(sdxTestClient.describe(), key(sdx))
                .then((tc, testDto, client) -> {
                    validateMultiAz(testDto, tc, "Downscale");
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
                List<String> instanceIds = instanceGroup.getMetadata().stream()
                        .map(InstanceMetaDataV4Response::getInstanceId)
                        .collect(Collectors.toList());
                validateMultiAzDistribution(sdxClusterDetailResponse.getName(), tc, operation, instanceIds, instanceGroup.getName());
            }
        }
    }

    private void validateMultiAzDistribution(String dataLakeName, TestContext tc, String operation, List<String> instanceIds, String hostGroup) {
        Map<String, Set<String>> availabilityZoneForVms = getCloudFunctionality(tc).listAvailabilityZonesForVms(dataLakeName, instanceIds);
        LOGGER.info("Availability Zone for Vms {}", availabilityZoneForVms);
        List<String> instancesWithNoAz = instanceIds.stream().filter(instance -> CollectionUtils.isEmpty(availabilityZoneForVms.get(instance)))
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(instancesWithNoAz)) {
            throw new TestFailException(String.format("Availability Zones is missing for instances %s in %s",
                    String.join(",", instancesWithNoAz), dataLakeName));
        }
        Map<String, Integer> zoneToNodeCountMap = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : availabilityZoneForVms.entrySet()) {
            if (!CollectionUtils.isEmpty(entry.getValue())) {
                String availabilityZone = entry.getValue().iterator().next();
                zoneToNodeCountMap.put(availabilityZone, zoneToNodeCountMap.getOrDefault(availabilityZone, 0) + 1);
            }
        }
        LOGGER.info("Zone to Node count {} after  {}", zoneToNodeCountMap, operation);
        int numInstances = instanceIds.size();
        int numZones = zoneToNodeCountMap.size();
        int numZonesWithDesiredNumInstances;
        if (instanceIds.size() >= zoneToNodeCountMap.size()) {
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

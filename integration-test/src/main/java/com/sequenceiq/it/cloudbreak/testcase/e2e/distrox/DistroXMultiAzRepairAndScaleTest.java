package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

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
import org.testng.ITestContext;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupsBuilder;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2EWithReusableResourcesTest;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.DistroxUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class DistroXMultiAzRepairAndScaleTest extends AbstractE2EWithReusableResourcesTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXMultiAzRepairAndScaleTest.class);

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private DistroxUtil distroxUtil;

    @Override
    protected void setupClass(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
        initializeAzureMarketplaceTermsPolicy(testContext);
        createEnvironmentWithFreeIpa(testContext);
        testContext.given(SdxInternalTestDto.class)
                .withCloudStorage()
                .when(sdxTestClient.createInternal())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running Cloudbreak, and an SDX cluster in available state",
            when = " valid MultiAz Datahub is provisioned, scaled up and scaled down",
            then = "the MultiAz Datahub should be available and nodes should be distributed correctly across multiple AZs after each operation")
    public void testDistroXMultiAzProvisionAndRepair(TestContext testContext, ITestContext iTestContext) {
        String datahubKey = "multiAzDistroxRepair";

        testContext.given(DistroXTestDto.class)
                .given(datahubKey, DistroXTestDto.class)
                .withEnableMultiAz(true)
                .withInstanceGroupsEntity(new DistroXInstanceGroupsBuilder(testContext)
                        .defaultHostGroup()
                        .withStorageOptimizedInstancetype()
                        .build())
                .when(distroXTestClient.create(), key(datahubKey))
                .await(STACK_AVAILABLE, key(datahubKey))
                .awaitForHealthyInstances()
                .given(datahubKey, DistroXTestDto.class)
                .when(distroXTestClient.get(), key(datahubKey))
                .then((tc, testDto, client) -> {
                    validateMultiAz(testDto, tc, "provisioning");
                    return testDto;
                })
                .then((tc, testDto, client) -> {
                    CloudFunctionality cloudFunctionality = tc.getCloudProvider().getCloudFunctionality();
                    Map<String, String> instancesToDelete = distroxUtil.getInstancesWithAz(testDto, client, MASTER.getName());
                    cloudFunctionality.deleteInstances(testDto.getName(), instancesToDelete);
                    return testDto;
                })
                .awaitForHostGroup(MASTER.getName(), InstanceStatus.DELETED_ON_PROVIDER_SIDE)
                .when(distroXTestClient.repair(MASTER))
                .await(STACK_AVAILABLE, key(datahubKey))
                .awaitForHealthyInstances()
                .given(datahubKey, DistroXTestDto.class)
                .when(distroXTestClient.get(), key(datahubKey))
                .then((tc, testDto, client) -> {
                    validateMultiAz(testDto, tc, "repair");
                    return testDto;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running Cloudbreak, and an SDX cluster in available state",
            when = " valid MultiAz Datahub is provisioned, scaled up and scaled down",
            then = "the MultiAz Datahub should be available and nodes should be distributed correctly across multiple AZs after each operation")
    public void testDistroXMultiAzProvisionAndScale(TestContext testContext, ITestContext iTestContext) {
        String datahubKey = "multiAzDistroxScale";
        DistroXScaleTestParameters params = new DistroXScaleTestParameters(iTestContext.getCurrentXmlTest().getAllParameters());

        testContext.given(DistroXTestDto.class)
                .given(datahubKey, DistroXTestDto.class)
                .withEnableMultiAz(true)
                .withInstanceGroupsEntity(new DistroXInstanceGroupsBuilder(testContext)
                        .defaultHostGroup()
                        .withStorageOptimizedInstancetype()
                        .build())
                .when(distroXTestClient.create(), key(datahubKey))
                .await(STACK_AVAILABLE, key(datahubKey))
                .awaitForHealthyInstances()
                .given(datahubKey, DistroXTestDto.class)
                .when(distroXTestClient.get(), key(datahubKey))
                .then((tc, testDto, client) -> {
                    validateMultiAz(testDto, tc, "provisioning");
                    return testDto;
                })
                .when(distroXTestClient.scale(params.getHostGroup(), params.getScaleUpTarget()))
                .await(STACK_AVAILABLE, key(datahubKey))
                .awaitForHealthyInstances()
                .given(datahubKey, DistroXTestDto.class)
                .when(distroXTestClient.get(), key(datahubKey))
                .then((tc, testDto, client) -> {
                    validateMultiAz(testDto, tc, "Upscale");
                    return testDto;
                })
                .when(distroXTestClient.scale(params.getHostGroup(), params.getScaleDownTarget()))
                .await(STACK_AVAILABLE, key(datahubKey))
                .awaitForHealthyInstances()
                .given(datahubKey, DistroXTestDto.class)
                .when(distroXTestClient.get(), key(datahubKey))
                .then((tc, testDto, client) -> {
                    validateMultiAz(testDto, tc, "Downscale");
                    return testDto;
                })
                .validate();
    }

    private void validateMultiAz(DistroXTestDto distroxTestDto, TestContext tc, String operation) {
        StackV4Response stackV4Response = distroxTestDto.getResponse();
        if (!stackV4Response.isEnableMultiAz()) {
            throw new TestFailException(String.format("MultiAz is not enabled for %s", stackV4Response.getName()));
        }
        for (InstanceGroupV4Response instanceGroup : stackV4Response.getInstanceGroups()) {
            if (!CollectionUtils.isEmpty(instanceGroup.getMetadata())) {
                Map<String, String> instanceZoneMap = instanceGroup.getMetadata().stream()
                        .collect(Collectors.toMap(InstanceMetaDataV4Response::getInstanceId, InstanceMetaDataV4Response::getAvailabilityZone));
                validateMultiAzDistribution(stackV4Response.getName(), tc, operation, instanceZoneMap, instanceGroup.getName());
            }
        }
    }

    private void validateMultiAzDistribution(String datahubName, TestContext tc, String operation, Map<String, String> instanceZoneMap,
            String hostGroup) {
        Map<String, String> availabilityZoneForVms = getCloudFunctionality(tc).listAvailabilityZonesForVms(datahubName, instanceZoneMap);
        LOGGER.info("Availability Zone for Vms {}", availabilityZoneForVms);
        List<String> instancesWithNoAz = instanceZoneMap.keySet().stream().filter(instance -> StringUtils.isEmpty(availabilityZoneForVms.get(instance)))
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(instancesWithNoAz)) {
            throw new TestFailException(String.format("Availability Zones is missing for instances %s in %s",
                    String.join(",", instancesWithNoAz), datahubName));
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
                        hostGroup, operation, datahubName, numInstances, numZones, numInstances / numZones, numZones - numInstances % numZones,
                        numZonesWithDesiredNumInstances));
            }
        }
        numZonesWithDesiredNumInstances = countZonesWithDesiredNumberOfInstances(zoneToNodeCountMap, numInstances / numZones + 1);
        if (numInstances % numZones != numZonesWithDesiredNumInstances) {
            throw new TestFailException(String.format("Distribution of nodes in AZs is not correct in host group: %s after %s for %s." +
                            "There are %s instance and %s zones.Number of Zones with number of instances %s should be %s but is %s",
                    hostGroup, operation, datahubName, numInstances, numZones, numInstances / numZones + 1, numInstances % numZones,
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

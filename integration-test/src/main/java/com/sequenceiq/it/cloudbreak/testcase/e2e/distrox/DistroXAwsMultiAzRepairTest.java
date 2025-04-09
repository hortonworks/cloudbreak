package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.WORKER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.testng.ITestContext;
import org.testng.annotations.Test;

import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.distrox.api.v1.distrox.model.network.InstanceGroupNetworkV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.network.aws.InstanceGroupAwsNetworkV1Parameters;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.cloud.v4.aws.AwsCloudProvider;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDtoBase;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupsBuilder;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.testcase.e2e.sdx.PreconditionSdxE2ETest;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.DistroxUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class DistroXAwsMultiAzRepairTest extends PreconditionSdxE2ETest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXAwsMultiAzRepairTest.class);

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private DistroxUtil distroxUtil;

    @Inject
    private AwsCloudProvider awsCloudProvider;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running Cloudbreak, and an SDX cluster in available state",
            when = "valid MultiAz Datahub is provisioned, worker nodes scaled up and scaled down",
            then = "the MultiAz Datahub should be available and nodes should be distributed correctly across multiple AZs after each operation")
    public void testDistroXMultiAzProvisionAndRepair(TestContext testContext, ITestContext iTestContext) {
        String datahubKey = "multiAzDistroxRepair";
        DistroXScaleTestParameters params = new DistroXScaleTestParameters(iTestContext.getCurrentXmlTest().getAllParameters());

        DistroXTestDtoBase<DistroXTestDto> distroXTestDto = testContext
                .given(SdxInternalTestDto.class)
                .withCloudStorage()
                .when(sdxTestClient.createInternal())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .given(DistroXTestDto.class)
                .given(datahubKey, DistroXTestDto.class)
                .withEnableMultiAz(true)
                .withVariant("AWS_NATIVE")
                .withInstanceGroupsEntity(new DistroXInstanceGroupsBuilder(testContext)
                        .defaultHostGroup()
                        .withStorageOptimizedInstancetype()
                        .build());

        distroXTestDto.getRequest().getInstanceGroups().forEach(igr -> {
            InstanceGroupAwsNetworkV1Parameters aws = new InstanceGroupAwsNetworkV1Parameters();
            InstanceGroupNetworkV1Request instanceGroupNetworkV1Request = new InstanceGroupNetworkV1Request();
            aws.setSubnetIds(awsCloudProvider.getSubnetIDs().stream().toList());
            instanceGroupNetworkV1Request.setAws(aws);
            igr.setNetwork(instanceGroupNetworkV1Request);
        });

        distroXTestDto
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
                    Map<String, String> instancesToDelete = distroxUtil.getInstancesWithAz(testDto, client, WORKER.getName());
                    cloudFunctionality.deleteInstances(testDto.getName(), instancesToDelete.keySet().stream().toList());
                    return testDto;
                })
                .awaitForHostGroup(WORKER.getName(), InstanceStatus.DELETED_ON_PROVIDER_SIDE)
                .when(distroXTestClient.repair(WORKER))
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
        Map<String, String> instanceSubnetMap = getCloudFunctionality(tc).getInstanceSubnetMap(instanceZoneMap.keySet().stream().toList());
        LOGGER.info("Subnets for VMs {}", instanceSubnetMap);
        List<String> instancesWithNoSubnet = instanceZoneMap.keySet().stream().filter(instance -> !StringUtils.hasLength(instanceSubnetMap.get(instance)))
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(instancesWithNoSubnet)) {
            throw new TestFailException(String.format("Subnet is missing for instances %s in %s",
                    String.join(",", instancesWithNoSubnet), datahubName));
        }
        Map<String, Integer> zoneToNodeCountMap = new HashMap<>();
        for (Entry<String, String> entry : instanceSubnetMap.entrySet()) {
            zoneToNodeCountMap.put(instanceZoneMap.get(entry.getKey()), zoneToNodeCountMap.getOrDefault(entry.getValue(), 0) + 1);
        }
        LOGGER.info("Zone to Node count {} after {}", zoneToNodeCountMap, operation);
        List<Integer> instanceDistribution = getInstanceDistribution(instanceZoneMap.keySet().size(), zoneToNodeCountMap.size());

        for (Entry<String, Integer> entry: zoneToNodeCountMap.entrySet()) {
            instanceDistribution.remove(entry.getValue());
        }

        if (!instanceDistribution.isEmpty()) {
            String missingNodeNumbers = instanceDistribution.stream().map(Object::toString).collect(Collectors.joining(", "));
            String zoneToNodeCountString = Joiner.on(", ").withKeyValueSeparator("=").join(zoneToNodeCountMap);
            throw new TestFailException(String.format("""
                            Distribution of nodes in AZs is not correct in host group: %s after %s for %s.
                            There are missing groups with the following node count(s): %s from the actual node distribution: %s.
                            """, hostGroup, operation, datahubName, missingNodeNumbers, zoneToNodeCountString));
        }
    }

    private List<Integer> getInstanceDistribution(int numberOfInstances, int numberOfZones) {
        List<Integer> instanceDistribution = new ArrayList<>();

        for (int i = 0; i < numberOfZones; i++) {
            instanceDistribution.add(numberOfInstances / numberOfZones);
        }

        int remainder = numberOfInstances % numberOfZones;

        for (int i = 0; i < remainder; i++) {
            Integer oldValue = instanceDistribution.get(i);
            instanceDistribution.set(i, oldValue + 1);
        }

        return instanceDistribution;
    }
}

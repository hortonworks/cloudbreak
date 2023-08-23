package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.util.CollectionUtils;
import org.testng.annotations.Test;

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
import com.sequenceiq.it.cloudbreak.testcase.e2e.sdx.PreconditionSdxE2ETest;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class DistroXMultiAzProvisioningTest extends PreconditionSdxE2ETest {

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak, and an SDX cluster in available state",
            when = "a valid MultiAz stack create request is sent for DH",
            then = "the MultiAz stack should be available and the cluster should be up and running"
    )
    public void testDistroXMultiAzProvision(TestContext testContext) {
        String datahubKey = "multiAZdistrox";
        testContext
            .given(SdxInternalTestDto.class)
            .withCloudStorage()
            .when(sdxTestClient.createInternal())
            .await(SdxClusterStatusResponse.RUNNING)
            .awaitForHealthyInstances()
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
                validateStackForMultiAz(testDto, tc);
                return testDto;
            })
            .validate();
    }

    private void validateStackForMultiAz(DistroXTestDto distroxTestDto, TestContext tc) {
        StackV4Response stackV4Response = distroxTestDto.getResponse();
        String stackName = stackV4Response.getName();
        if (!stackV4Response.isEnableMultiAz()) {
            throw new TestFailException(String.format("MultiAz is not enabled for %s", stackName));
        }
        List<String> instanceIds = stackV4Response.getInstanceGroups().stream()
                .map(InstanceGroupV4Response::getMetadata)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .map(InstanceMetaDataV4Response::getInstanceId)
                .toList();

        Map<String, Set<String>> availabilityZoneForVms = getCloudFunctionality(tc).listAvailabilityZonesForVms(stackName, instanceIds);
        List<String> instancesWithNoAz = instanceIds.stream().filter(instance -> CollectionUtils.isEmpty(availabilityZoneForVms.get(instance)))
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(instancesWithNoAz)) {
            throw new TestFailException(String.format("Availability Zones is missing for instances %s in %s",
                    String.join(",", instancesWithNoAz), stackName));
        }
    }

    protected CloudFunctionality getCloudFunctionality(TestContext testContext) {
        return testContext.getCloudProvider().getCloudFunctionality();
    }
}

package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.util.CollectionUtils;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
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
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxAzureMultiAzVerticalScaleTest extends PreconditionSdxE2ETest {

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak, and an SDX MultiAz cluster in available state",
            when = "a valid MultiAz stack vertical scale request is sent",
            then = "the MultiAz stack should be vertically scaled with the new instance type and the cluster should be up and running"
    )
    public void testSDXAzureMultiAzVerticalScale(TestContext testContext) {
        String sdx = resourcePropertyProvider().getName();
        String sdxVerticalScaleKey = "sdxVerticalScaleKey";
        String targetInstanceType = "Standard_D8s_v5";
        String targetInstanceGroup = "master";

        testContext
                .given(EnvironmentTestDto.class)
                .given(sdxVerticalScaleKey, VerticalScalingTestDto.class)
                .withGroup(targetInstanceGroup)
                .withInstanceType(targetInstanceType)

                .given(sdx, SdxTestDto.class)
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
                    validateMultiAz(testDto, tc);
                    return testDto;
                })
                .given(sdx, SdxTestDto.class)
                .when(sdxTestClient.stop(), key(sdx))
                .await(SdxClusterStatusResponse.STOPPED, key(sdx))
                .when(sdxTestClient.verticalScaleByCrn(sdxVerticalScaleKey))
                .awaitForFlow()
                .then((tc, dto, client) -> {
                    CloudbreakClient cbClient = tc.getMicroserviceClient(CloudbreakClient.class);
                    StackV4Response stackV4Response = cbClient.getDefaultClient(testContext).stackV4Endpoint().getByCrn(0L, dto.getCrn(), Set.of());
                    String instanceType = stackV4Response.getInstanceGroups().stream().filter(ig -> ig.getName().equals(targetInstanceGroup))
                            .findFirst().get().getTemplate().getInstanceType();
                    if (!instanceType.equals(targetInstanceType)) {
                        throw new TestFailException("Vertical scaled instance type should be the same: " + instanceType);
                    }
                    return dto;
                })
                .when(sdxTestClient.start(), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .validate();
    }

    private void validateMultiAz(SdxTestDto sdxTestDto, TestContext tc) {
        SdxClusterDetailResponse sdxClusterDetailResponse = sdxTestDto.getResponse();
        if (!sdxClusterDetailResponse.isEnableMultiAz()) {
            throw new TestFailException(String.format("MultiAz is not enabled for %s", sdxClusterDetailResponse.getName()));
        }
        List<String> instanceIds = sdxClusterDetailResponse.getStackV4Response().getInstanceGroups().stream()
                .map(InstanceGroupV4Response::getMetadata)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .map(InstanceMetaDataV4Response::getInstanceId)
                .collect(Collectors.toList());
        String sdxName = sdxClusterDetailResponse.getStackV4Response().getName();
        Map<String, Set<String>> availabilityZoneForVms = getCloudFunctionality(tc).listAvailabilityZonesForVms(sdxName, instanceIds);
        List<String> instancesWithNoAz = instanceIds.stream().filter(instance -> CollectionUtils.isEmpty(availabilityZoneForVms.get(instance)))
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(instancesWithNoAz)) {
            throw new TestFailException(String.format("Availability Zones is missing for instances %s in %s",
                    String.join(",", instancesWithNoAz), sdxName));
        }
    }
}
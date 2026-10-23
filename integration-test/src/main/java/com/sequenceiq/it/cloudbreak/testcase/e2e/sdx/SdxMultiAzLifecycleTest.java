package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.IDBROKER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.loadbalancer.LoadBalancerResponse;
import com.sequenceiq.it.cloudbreak.assertion.stack.StackAssertion;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxScaleTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.SdxMultiAzValidationUtil;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.it.cloudbreak.util.TestUpgradeCandidateProvider;
import com.sequenceiq.it.cloudbreak.util.VolumeUtils;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxMultiAzLifecycleTest extends PreconditionSdxE2ETest {

    private static final String HMS_SCALE_OUT_GROUP = "hms_scale_out";

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private SdxUtil sdxUtil;

    @Inject
    private StackAssertion stackAssertion;

    @Inject
    private TestUpgradeCandidateProvider testUpgradeCandidateProvider;

    @Inject
    private SdxMultiAzValidationUtil sdxMultiAzValidationUtil;

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak and an ENTERPRISE multi-AZ Data Lake provisioned from the OS upgrade source image "
                    + "(Enterprise shape is only available on CM version greater than or equal to 7.2.17)",
            when = "the shared Data Lake is OS-upgraded, then upscaled and downscaled, then its master instances are deleted and repaired",
            then = "every operation succeeds in order on the one shared Data Lake and the distribution of nodes across AZs stays even throughout"
    )
    public void testSDXMultiAzLifecycle(TestContext testContext) {
        Pair<String, String> osUpgradePair = testUpgradeCandidateProvider.getOsUpgradeSourceAndCandidate(testContext);
        String sdx = resourcePropertyProvider().getName();
        List<String> expectedVolumeIds = new ArrayList<>();
        List<String> actualVolumeIds = new ArrayList<>();

        testContext
                .given(sdx, SdxTestDto.class)
                .withName(sdx)
                .withCloudStorage()
                .withClusterShape(SdxClusterShape.ENTERPRISE)
                .withRuntimeVersion(null)
                .withEnableMultiAz(true)
                .withImageId(osUpgradePair.getLeft())
                .when(sdxTestClient.createWithImageId(), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .awaitForHealthyInstances()
                .given(sdx, SdxTestDto.class)
                .when(sdxTestClient.describe(), key(sdx))
                .then((tc, testDto, client) -> {
                    sdxMultiAzValidationUtil.validateMultiAz(testDto, tc, "provisioning");
                    List<String> instances = sdxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    instances.addAll(sdxUtil.getInstanceIds(testDto, client, IDBROKER.getName()));
                    expectedVolumeIds.addAll(getCloudFunctionality(tc).listInstancesVolumeIds(testDto.getName(), instances));
                    return testDto;
                })
                // OS upgrade on the shared Data Lake
                .when(sdxTestClient.osUpgrade(osUpgradePair.getRight()), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> {
                    List<String> instanceIds = sdxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    instanceIds.addAll(sdxUtil.getInstanceIds(testDto, client, IDBROKER.getName()));
                    actualVolumeIds.addAll(getCloudFunctionality(tc).listInstancesVolumeIds(testDto.getName(), instanceIds));
                    return testDto;
                })
                .then((tc, testDto, client) -> VolumeUtils.compareVolumeIdsAfterRepair(testDto, actualVolumeIds, expectedVolumeIds))
                .given(sdx, SdxTestDto.class)
                .when(sdxTestClient.describe(), key(sdx))
                .then((tc, testDto, client) -> {
                    sdxMultiAzValidationUtil.validateMultiAz(testDto, tc, "Upgrade");
                    return testDto;
                })
                // Upscale then downscale the hms_scale_out group on the shared Data Lake
                .given(SdxScaleTestDto.class)
                .withName(sdx)
                .withGroup(HMS_SCALE_OUT_GROUP)
                .withDesiredCount(5)
                .when(sdxTestClient.scale(), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .given(sdx, SdxTestDto.class)
                .when(sdxTestClient.describe(), key(sdx))
                .then((tc, testDto, client) -> {
                    sdxMultiAzValidationUtil.validateMultiAz(testDto, tc, "Upscale");
                    return testDto;
                })
                .given(SdxScaleTestDto.class)
                .withName(sdx)
                .withGroup(HMS_SCALE_OUT_GROUP)
                .withDesiredCount(3)
                .when(sdxTestClient.scale(), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .given(sdx, SdxTestDto.class)
                .when(sdxTestClient.describe(), key(sdx))
                .awaitForHealthyInstances()
                .when(sdxTestClient.describe(), key(sdx))
                .then((tc, testDto, client) -> {
                    sdxMultiAzValidationUtil.validateMultiAz(testDto, tc, "Downscale");
                    return testDto;
                })
                .then((tc, testDto, client) -> {
                    List<LoadBalancerResponse> loadBalancers = sdxUtil.getLoadbalancers(testDto, client);
                    stackAssertion.validateLoadBalancerFQDNInTheHosts(testDto, loadBalancers);
                    return testDto;
                })
                // Delete the master instances provider-side and repair them on the shared Data Lake
                .then((tc, testDto, client) -> {
                    Map<String, String> instancesToDelete = sdxUtil.getInstancesWithAz(testDto, client, MASTER.getName());
                    getCloudFunctionality(tc).deleteInstances(testDto.getName(), instancesToDelete);
                    return testDto;
                })
                .awaitForHostGroups(List.of(MASTER.getName()), InstanceStatus.DELETED_ON_PROVIDER_SIDE)
                .useAlternativeServiceEndpointIfConfigured()
                .when(sdxTestClient.repair(MASTER.getName()), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .given(sdx, SdxTestDto.class)
                .when(sdxTestClient.describe(), key(sdx))
                .then((tc, testDto, client) -> {
                    sdxMultiAzValidationUtil.validateMultiAz(testDto, tc, "repair");
                    return testDto;
                })
                .validate();
    }

    protected CloudFunctionality getCloudFunctionality(TestContext testContext) {
        return testContext.getCloudProvider().getCloudFunctionality();
    }
}

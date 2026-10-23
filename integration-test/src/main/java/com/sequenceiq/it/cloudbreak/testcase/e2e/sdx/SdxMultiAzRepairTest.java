package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.SdxMultiAzValidationUtil;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxMultiAzRepairTest extends PreconditionSdxE2ETest {

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Inject
    private SdxUtil sdxUtil;

    @Inject
    private SdxMultiAzValidationUtil sdxMultiAzValidationUtil;

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
                    sdxMultiAzValidationUtil.validateMultiAz(testDto, tc, "provisioning");
                    return testDto;
                })
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

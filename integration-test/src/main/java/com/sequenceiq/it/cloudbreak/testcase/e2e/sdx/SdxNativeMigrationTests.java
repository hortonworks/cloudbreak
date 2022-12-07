package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.IDBROKER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.assertion.audit.DatalakeAuditGrpcServiceAssertion;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.it.cloudbreak.util.VolumeUtils;
import com.sequenceiq.it.cloudbreak.util.aws.AwsCloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxNativeMigrationTests extends PreconditionSdxE2ETest {

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private SdxUtil sdxUtil;

    @Inject
    private DatalakeAuditGrpcServiceAssertion datalakeAuditGrpcServiceAssertion;

    @Inject
    private AwsCloudFunctionality cloudFunctionality;

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak, and an SDX cluster in available state",
            when = "upgrade called on the SDX cluster in order to migrate it to native",
            then = "SDX upgrade should be successful, the cluster should be up and running"
    )
    public void testSDXUpgradeToMigrate(TestContext testContext) {
        String sdx = resourcePropertyProvider().getName();

        List<String> actualVolumeIds = new ArrayList<>();
        List<String> expectedVolumeIds = new ArrayList<>();

        String runtimeVersion = commonClusterManagerProperties().getUpgrade().getCurrentRuntimeVersion();
        String blueprintName = commonClusterManagerProperties().getInternalSdxBlueprintNameWithRuntimeVersion(runtimeVersion);

        testContext
                .given(ClusterTestDto.class)
                .withBlueprintName(blueprintName)
                .withValidateBlueprint(Boolean.FALSE)
                .given(sdx, SdxInternalTestDto.class)
                .withCloudStorage()
                .withRuntimeVersion(runtimeVersion)
                .withVariant("AWS")
                .when(sdxTestClient.createInternal(), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> {
                    List<String> instances = sdxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    instances.addAll(sdxUtil.getInstanceIds(testDto, client, IDBROKER.getName()));
                    expectedVolumeIds.addAll(getCloudFunctionality(tc).listInstanceVolumeIds(testDto.getName(), instances));
                    return testDto;
                })
                .then(cloudformationTemplateForStackDoesExist())
                .when(sdxTestClient.upgradeInternal(), key(sdx))
                .await(SdxClusterStatusResponse.DATALAKE_UPGRADE_IN_PROGRESS, key(sdx).withWaitForFlow(Boolean.FALSE))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> {
                    List<String> instanceIds = sdxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    instanceIds.addAll(sdxUtil.getInstanceIds(testDto, client, IDBROKER.getName()));
                    actualVolumeIds.addAll(getCloudFunctionality(tc).listInstanceVolumeIds(testDto.getName(), instanceIds));
                    return testDto;
                })
                .then((tc, testDto, client) -> VolumeUtils.compareVolumeIdsAfterRepair(testDto, actualVolumeIds, expectedVolumeIds))
                .then(cloudformationTemplateForStackDoesNotExist())
                .validate();
    }

    private Assertion<SdxInternalTestDto, SdxClient> cloudformationTemplateForStackDoesExist() {
        return getSdxInternalTestDtoSdxClientAssertion(true);
    }

    private Assertion<SdxInternalTestDto, SdxClient> cloudformationTemplateForStackDoesNotExist() {
        return getSdxInternalTestDtoSdxClientAssertion(false);
    }

    private Assertion<SdxInternalTestDto, SdxClient> getSdxInternalTestDtoSdxClientAssertion(boolean expected) {
        return (tc, testDto, client) -> {
            Boolean res = cloudFunctionality.isCloudFormationExistForStack(testDto.getName());
            Assertions.assertThat(res).isEqualTo(expected);
            return testDto;
        };
    }
}

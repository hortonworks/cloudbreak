package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.IDBROKER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.AwsNetworkParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.NetworkRequest;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.assertion.audit.DatalakeAuditGrpcServiceAssertion;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.aws.AwsCloudProvider;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.it.cloudbreak.util.VolumeUtils;
import com.sequenceiq.it.cloudbreak.util.aws.AwsCloudFunctionality;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxNativeMigrationTests extends AbstractE2ETest {
    protected static final Status FREEIPA_AVAILABLE = Status.AVAILABLE;

    protected static final String AWS = "AWS";

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private SdxUtil sdxUtil;

    @Inject
    private DatalakeAuditGrpcServiceAssertion datalakeAuditGrpcServiceAssertion;

    @Inject
    private AwsCloudFunctionality cloudFunctionality;

    @Inject
    private AwsCloudProvider awsCloudProvider;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak, and freeipa with cloudformation",
            when = "upgrade ",
            then = "migration happens into native")
    public void testSDXUpgradeToMigrate(TestContext testContext) {
        String freeIpa = resourcePropertyProvider().getName();
        String subnet = awsCloudProvider.getSubnetId();
        String sdx = resourcePropertyProvider().getName();

        List<String> actualVolumeIds = new ArrayList<>();
        List<String> expectedVolumeIds = new ArrayList<>();

        String runtimeVersion = commonClusterManagerProperties().getUpgrade().getCurrentRuntimeVersion();
        String blueprintName = commonClusterManagerProperties().getInternalSdxBlueprintNameWithRuntimeVersion(runtimeVersion);

        testContext
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs();
        FreeIpaTestDto freeipa = testContext.given(freeIpa, FreeIpaTestDto.class)
                .withNetwork(getNetworkRequest())
                .withTelemetry("telemetry")
                .withVariant(AWS)
                .withUpgradeCatalogAndImage();
        freeipa.getRequest().getInstanceGroups().stream().forEach(igr -> igr.getNetwork().getAws().setSubnetIds(List.of(subnet)));
        freeipa.when(freeIpaTestClient.create(), key(freeIpa))
                .await(FREEIPA_AVAILABLE)
                .given(ClusterTestDto.class)
                .withBlueprintName(blueprintName)
                .withValidateBlueprint(Boolean.FALSE);
        SdxInternalTestDto sdxDto = freeipa.given(sdx, SdxInternalTestDto.class)
                .withCloudStorage()
                .withRuntimeVersion(runtimeVersion)
                .withVariant(AWS)
                .withEnableMultiAz(false);
        sdxDto.getRequest().getStackV4Request().getNetwork().getAws().setSubnetId(subnet);
        sdxDto.when(sdxTestClient.createInternal(), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> {
                    List<String> instances = sdxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    instances.addAll(sdxUtil.getInstanceIds(testDto, client, IDBROKER.getName()));
                    expectedVolumeIds.addAll(cloudFunctionality.listInstanceVolumeIds(testDto.getName(), instances));
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
                    actualVolumeIds.addAll(cloudFunctionality.listInstanceVolumeIds(testDto.getName(), instanceIds));
                    return testDto;
                })
                .then((tc, testDto, client) -> VolumeUtils.compareVolumeIdsAfterRepair(testDto, actualVolumeIds, expectedVolumeIds))
                .then(cloudformationTemplateForStackDoesNotExist())
                .validate();
    }

    private NetworkRequest getNetworkRequest() {
        NetworkRequest networkRequest = new NetworkRequest();
        AwsNetworkParameters params = new AwsNetworkParameters();
        networkRequest.setAws(params);
        params.setVpcId(awsCloudProvider.getVpcId());
        String subnet = awsCloudProvider.getSubnetId();
        params.setSubnetId(subnet);

        return networkRequest;
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
            Assertions.assertThat(res).withFailMessage("Stack cloud formation "
                    + (expected ? " should exist but it is not" : " should not exist but it is"))
                    .isEqualTo(expected);
            return testDto;
        };
    }
}

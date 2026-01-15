package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.IDBROKER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.AwsNetworkParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.NetworkRequest;
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
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
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

    @Override
    protected void setupTest(TestContext testContext) {
        initializeTest(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak, and freeipa with cloudformation",
            when = "upgrade ",
            then = "migration happens into native")
    public void testSDXUpgradeToMigrate(TestContext testContext) {
        String sdx = resourcePropertyProvider().getName();

        List<String> actualVolumeIds = new ArrayList<>();
        List<String> expectedVolumeIds = new ArrayList<>();

        String runtimeVersion = commonClusterManagerProperties().getUpgrade()
                .getCurrentRuntimeVersion(testContext.getCloudProvider().getGovCloud());
        String blueprintName = commonClusterManagerProperties().getInternalSdxBlueprintNameWithRuntimeVersion(runtimeVersion);


        String subnet = awsCloudProvider.getSubnetId();
        setUpEnvironmentTestDto(testContext, Boolean.TRUE, 3)
                .withFreeIpaImage(testContext.getCloudProvider().getFreeIpaUpgradeImageCatalog(), testContext.getCloudProvider().getFreeIpaUpgradeImageId())
                .when(getEnvironmentTestClient().create())
                .awaitForCreationFlow()
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .given(ClusterTestDto.class)
                .withBlueprintName(blueprintName)
                .withValidateBlueprint(Boolean.FALSE);
        SdxInternalTestDto sdxDto = testContext.given(sdx, SdxInternalTestDto.class)
                .withCloudStorage()
                .withRuntimeVersion(runtimeVersion)
                .withVariant(AWS)
                .withEnableMultiAz(false);
        StackV4Request stackV4Request = sdxDto.getRequest().getStackV4Request();
        stackV4Request.getNetwork().getAws().setSubnetId(subnet);
        stackV4Request.setImage(null);
        sdxDto.when(sdxTestClient.createInternal(), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> {
                    List<String> instances = sdxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    instances.addAll(sdxUtil.getInstanceIds(testDto, client, IDBROKER.getName()));
                    expectedVolumeIds.addAll(cloudFunctionality.listInstancesVolumeIds(testDto.getName(), instances));
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
                    actualVolumeIds.addAll(cloudFunctionality.listInstancesVolumeIds(testDto.getName(), instanceIds));
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

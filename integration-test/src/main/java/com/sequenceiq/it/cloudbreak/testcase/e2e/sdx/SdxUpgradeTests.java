package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.GATEWAY;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.IDBROKER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.doNotWaitForFlow;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.loadbalancer.LoadBalancerResponse;
import com.sequenceiq.it.cloudbreak.assertion.audit.DatalakeAuditGrpcServiceAssertion;
import com.sequenceiq.it.cloudbreak.assertion.sdx.SdxAssertion;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CloudProvider;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxUpgradeTestDto;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.it.cloudbreak.util.TestUpgradeCandidateProvider;
import com.sequenceiq.it.cloudbreak.util.VolumeUtils;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.it.util.imagevalidation.ImageValidatorE2ETest;
import com.sequenceiq.it.util.imagevalidation.ImageValidatorE2ETestUtil;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

public class SdxUpgradeTests extends PreconditionSdxE2ETest implements ImageValidatorE2ETest {

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private SdxUtil sdxUtil;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Inject
    private DatalakeAuditGrpcServiceAssertion datalakeAuditGrpcServiceAssertion;

    @Inject
    private SdxUpgradeDatabaseTestUtil sdxUpgradeDatabaseTestUtil;

    @Inject
    private ImageValidatorE2ETestUtil imageValidatorE2ETestUtil;

    @Inject
    private TestUpgradeCandidateProvider testUpgradeCandidateProvider;

    @Inject
    private SdxAssertion sdxAssertion;

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak, and an SDX cluster in available state",
            when = "upgrade called on the SDX cluster",
            then = "SDX upgrade should be successful, the cluster should be up and running"
    )
    public void testSDXUpgrade(TestContext testContext) {
        List<String> actualVolumeIds = new ArrayList<>();
        List<String> expectedVolumeIds = new ArrayList<>();

        SdxTestDto sdxTestDto = testContext.given(SdxTestDto.class)
                .withCloudStorage()
                .withExternalDatabase(sdxDbRequest(testContext.getCloudProvider()));
        setupSourceImage(testContext, sdxTestDto);
        sdxTestDto
                .when(sdxTestClient.create())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> {
                    List<String> instances = sdxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    instances.addAll(sdxUtil.getInstanceIds(testDto, client, IDBROKER.getName()));
                    expectedVolumeIds.addAll(getCloudFunctionality(tc).listInstancesVolumeIds(testDto.getName(), instances));
                    return testDto;
                })
                .when(sdxTestClient.upgrade())
                .await(SdxClusterStatusResponse.DATALAKE_UPGRADE_IN_PROGRESS, doNotWaitForFlow())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> {
                    List<String> instanceIds = sdxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    instanceIds.addAll(sdxUtil.getInstanceIds(testDto, client, IDBROKER.getName()));
                    actualVolumeIds.addAll(getCloudFunctionality(tc).listInstancesVolumeIds(testDto.getName(), instanceIds));
                    return testDto;
                })
                .then((tc, testDto, client) -> VolumeUtils.compareVolumeIdsAfterRepair(testDto, actualVolumeIds, expectedVolumeIds))
                .then((tc, testDto, client) -> sdxUpgradeDatabaseTestUtil.checkCloudProviderDatabaseVersionFromPrimaryGateway(
                        testDto.getResponse().getDatabaseEngineVersion(), tc, testDto))
                // This assertion is disabled until the Audit Service is not configured.
                //.then(datalakeAuditGrpcServiceAssertion::upgradeClusterByNameInternal)
                .then((tc, testDto, client) -> {
                    List<LoadBalancerResponse> loadBalancers = sdxUtil.getLoadbalancers(testDto, client);
                    sdxAssertion.validateLoadBalancerFQDNInTheHosts(testDto, loadBalancers);
                    return testDto;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak, and an SDX cluster in available state",
            when = "patch upgrade called on the SDX cluster",
            then = "SDX patch upgrade should be successful, the cluster should be up and running"
    )
    public void testSDXPatchUpgrade(TestContext testContext) {
        List<String> actualVolumeIds = new ArrayList<>();
        List<String> expectedVolumeIds = new ArrayList<>();
        Pair<String, String> patchUpgradePair = testUpgradeCandidateProvider.getPatchUpgradeSourceAndCandidate(testContext);

        testContext.given(SdxUpgradeTestDto.class).withImageId(patchUpgradePair.getRight());

        SdxTestDto sdxTestDto = testContext.given(SdxTestDto.class)
                .withCloudStorage()
                .withImageId(patchUpgradePair.getLeft())
                .withExternalDatabase(sdxDbRequest(testContext.getCloudProvider()));
        sdxTestDto
                .when(sdxTestClient.create())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> {
                    List<String> instances = sdxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    instances.addAll(sdxUtil.getInstanceIds(testDto, client, IDBROKER.getName()));
                    expectedVolumeIds.addAll(getCloudFunctionality(tc).listInstancesVolumeIds(testDto.getName(), instances));
                    return testDto;
                })
                .when(sdxTestClient.upgrade())
                .await(SdxClusterStatusResponse.DATALAKE_UPGRADE_IN_PROGRESS, doNotWaitForFlow())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> {
                    List<String> instanceIds = sdxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    instanceIds.addAll(sdxUtil.getInstanceIds(testDto, client, IDBROKER.getName()));
                    actualVolumeIds.addAll(getCloudFunctionality(tc).listInstancesVolumeIds(testDto.getName(), instanceIds));
                    return testDto;
                })
                .then((tc, testDto, client) -> VolumeUtils.compareVolumeIdsAfterRepair(testDto, actualVolumeIds, expectedVolumeIds))
                .then((tc, testDto, client) -> sdxUpgradeDatabaseTestUtil.checkCloudProviderDatabaseVersionFromPrimaryGateway(
                        testDto.getResponse().getDatabaseEngineVersion(), tc, testDto))
                .then((tc, testDto, client) -> {
                    List<LoadBalancerResponse> loadBalancers = sdxUtil.getLoadbalancers(testDto, client);
                    sdxAssertion.validateLoadBalancerFQDNInTheHosts(testDto, loadBalancers);
                    return testDto;
                })
                .validate();
    }

    private SdxDatabaseRequest sdxDbRequest(CloudProvider cloudProvider) {
        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NONE);
        sdxDatabaseRequest.setDatabaseEngineVersion(cloudProvider.getEmbeddedDbUpgradeSourceVersion());
        return sdxDatabaseRequest;
    }

    private void setupSourceImage(TestContext testContext, SdxTestDto sdxTestDto) {
        if (imageValidatorE2ETestUtil.isImageValidation()) {
            ImageV4Response upgradeSourceImage = imageValidatorE2ETestUtil.getUpgradeSourceImage(testContext);
            sdxTestDto.withImage(imageValidatorE2ETestUtil.getImageCatalogName(), upgradeSourceImage.getUuid());
        } else {
            sdxTestDto.withRuntimeVersion(commonClusterManagerProperties.getUpgrade()
                    .getCurrentRuntimeVersion(testContext.getCloudProvider().getGovCloud()));
        }
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak, and a HA SDX cluster in available state",
            when = "upgrade called on the HA SDX cluster",
            then = "HA SDX upgrade should be successful, the cluster should be up and running"
    )
    public void testSDXHAUpgrade(TestContext testContext) {
        List<String> actualVolumeIds = new ArrayList<>();
        List<String> expectedVolumeIds = new ArrayList<>();

        testContext
                .given(SdxTestDto.class)
                    .withClusterShape(SdxClusterShape.ENTERPRISE)
                    .withCloudStorage()
                    .withRuntimeVersion(commonClusterManagerProperties.getUpgrade()
                            .getCurrentHARuntimeVersion(testContext.getCloudProvider().getGovCloud()))
                .when(sdxTestClient.create())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> {
                    List<String> instances = sdxUtil.getInstanceIds(testDto, client, GATEWAY.getName());
                    instances.addAll(sdxUtil.getInstanceIds(testDto, client, IDBROKER.getName()));
                    expectedVolumeIds.addAll(getCloudFunctionality(tc).listInstancesVolumeIds(testDto.getName(), instances));
                    return testDto;
                })
                .when(sdxTestClient.upgrade())
                .await(SdxClusterStatusResponse.DATALAKE_UPGRADE_IN_PROGRESS, doNotWaitForFlow())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> {
                    List<String> instanceIds = sdxUtil.getInstanceIds(testDto, client, GATEWAY.getName());
                    instanceIds.addAll(sdxUtil.getInstanceIds(testDto, client, IDBROKER.getName()));
                    actualVolumeIds.addAll(getCloudFunctionality(tc).listInstancesVolumeIds(testDto.getName(), instanceIds));
                    return testDto;
                })
                .then((tc, testDto, client) -> VolumeUtils.compareVolumeIdsAfterRepair(testDto, actualVolumeIds, expectedVolumeIds))
                // This assertion is disabled until the Audit Service is not configured.
                //.then(datalakeAuditGrpcServiceAssertion::upgradeClusterByNameInternal)
                .validate();
    }

    @Override
    public String getCbImageId(TestContext testContext) {
        return testContext.get(SdxTestDto.class).getResponse().getStackV4Response().getImage().getId();
    }
}

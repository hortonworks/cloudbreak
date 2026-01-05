package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.assertion.CBAssertion.assertEquals;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.time.Duration;
import java.util.UUID;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageComponentVersions;
import com.sequenceiq.cloudbreak.auth.crn.TestCrnGenerator;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.assertion.datalake.SdxUpgradeTestAssertion;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.client.RedbeamsDatabaseServerTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.ImageSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.InstanceTemplateV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.VolumeV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.database.RedbeamsDatabaseServerTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxUpgradeTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxUpgradeReplaceVms;
import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeResponse;

public class MockSdxUpgradeTests extends AbstractMockTest {

    @Inject
    private RedbeamsDatabaseServerTestClient redbeamsDatabaseServerTestClient;

    @Inject
    private ImageCatalogTestClient imageCatalogTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running Cloudbreak",
            when = "start an sdx cluster without attached disk on gateway, but disk attachment is supported on cloud provider side",
            then = "Upgrade option should be presented"
    )
    public void testSdxUpgradeWhenNoAttachedDisksButEmbeddedDBShouldHaveBeenOnSelfConfiguredAttachedDisk(MockedTestContext testContext) {
        String upgradeImageCatalogName = resourcePropertyProvider().getName();
        createImageCatalogForOsUpgrade(testContext, upgradeImageCatalogName);
        String sdxInternal = resourcePropertyProvider().getName();
        String stack = resourcePropertyProvider().getName();
        String cluster = "cmcluster";
        String imageSettings = "imageSettingsUpgrade";
        testContext
                .given(EnvironmentTestDto.class)
                .withCreateFreeIpa(Boolean.FALSE)
                .withName(resourcePropertyProvider().getEnvironmentName())
                .when(getEnvironmentTestClient().create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.create())
                .awaitForCreationFlow()
                .given(cluster, ClusterTestDto.class)
                .given(imageSettings, ImageSettingsTestDto.class)
                .withImageId("aaa778fc-7f17-4535-9021-515351df3691")
                .withImageCatalog(upgradeImageCatalogName)
                .given("NoAttachedDisksTemplate", InstanceTemplateV4TestDto.class)
                .withAttachedVolume(testContext.init(VolumeV4TestDto.class).withCount(0))
                .given("InstanceGroupWithoutAttachedDisk", InstanceGroupTestDto.class)
                .withHostGroup(HostGroupType.MASTER)
                .withTemplate("NoAttachedDisksTemplate")
                .given(stack, StackTestDto.class)
                .withCluster(cluster)
                .withImageSettings(imageSettings)
                .replaceInstanceGroups("InstanceGroupWithoutAttachedDisk")
                .given(sdxInternal, SdxInternalTestDto.class)
                .withStackRequest(key(cluster), key(stack))
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING)
                .then(SdxUpgradeTestAssertion.validateUpgradeCandidateWithLockedComponentIsAvailable())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running CloudSdxInternalTestDtobreak",
            when = "start an sdx cluster with external database",
            then = "Upgrade option should be presented"
    )
    public void testSdxUpgradeSuccessfulWhenExternalDatabaseIsUsed(MockedTestContext testContext) {
        String upgradeImageCatalogName = resourcePropertyProvider().getName();
        createImageCatalogForOsUpgrade(testContext, upgradeImageCatalogName);
        String sdxInternal = resourcePropertyProvider().getName();
        String stack = resourcePropertyProvider().getName();
        String clouderaManager = "cm";
        String cluster = "cmcluster";
        String imageSettings = "imageSettingsUpgrade";
        String clusterCrn = TestCrnGenerator.getDatalakeCrn(UUID.randomUUID().toString(), "cloudera");
        testContext
                .given(EnvironmentTestDto.class)
                .withCreateFreeIpa(Boolean.FALSE)
                .withName(resourcePropertyProvider().getEnvironmentName())
                .when(getEnvironmentTestClient().create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.create())
                .awaitForCreationFlow()
                .given(RedbeamsDatabaseServerTestDto.class)
                .withEnvironmentCrn(testContext.get(EnvironmentTestDto.class).getResponse().getCrn())
                .withClusterCrn(clusterCrn)
                .when(redbeamsDatabaseServerTestClient.create())
                .await(Status.AVAILABLE)
                .given(clouderaManager, ClouderaManagerTestDto.class)
                .given(cluster, ClusterTestDto.class)
                .withClouderaManager(clouderaManager)
                .withExternalDatabaseCrn()
                .given(imageSettings, ImageSettingsTestDto.class)
                .withImageId("aaa778fc-7f17-4535-9021-515351df3691")
                .withImageCatalog(upgradeImageCatalogName)
                .given(stack, StackTestDto.class)
                .withCluster(cluster)
                .withImageSettings(imageSettings)
                .given(sdxInternal, SdxInternalTestDto.class)
                .withStackRequest(key(cluster), key(stack))
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING)
                .given(SdxUpgradeTestDto.class)
                .withRuntime(null)
                .withLockComponents(true)
                .withReplaceVms(SdxUpgradeReplaceVms.ENABLED)
                .given(sdxInternal, SdxInternalTestDto.class)
                .then(setCmVersionInMockToUpgradedVersion())
                .when(sdxTestClient.upgradeInternal(), key(sdxInternal))
                .await(SdxClusterStatusResponse.DATALAKE_UPGRADE_IN_PROGRESS, key(sdxInternal).withWaitForFlow(Boolean.FALSE))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxInternal).withPollingInterval(Duration.ofSeconds(5L)))
                .awaitForHealthyInstances()
                .validate();
    }

    private Assertion<SdxInternalTestDto, SdxClient> setCmVersionInMockToUpgradedVersion() {
        return (tc, entity, sdxClient) -> {
            SdxUpgradeRequest request = new SdxUpgradeRequest();
            request.setLockComponents(true);
            request.setDryRun(true);
            SdxUpgradeResponse upgradeResponse =
                    sdxClient.getDefaultClient().sdxUpgradeEndpoint().upgradeClusterByName(entity.getName(), request);
            ImageComponentVersions componentVersions = upgradeResponse.getUpgradeCandidates().get(0).getComponentVersions();
            entity.mockCm().setCmVersion(componentVersions.getCm() + "-" + componentVersions.getCmGBN());
            return entity;
        };
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running CloudSdxInternalTestDtobreak",
            when = "start an sdx cluster with embedded database on attached disk",
            then = "Upgrade option should be presented"
    )
    public void testSdxUpgradeSuccessfulWhenEmbeddedDatabaseIsOnAttachedDisk(MockedTestContext testContext) {
        String upgradeImageCatalogName = resourcePropertyProvider().getName();
        createImageCatalogForOsUpgrade(testContext, upgradeImageCatalogName);
        String sdxInternal = resourcePropertyProvider().getName();
        String stack = resourcePropertyProvider().getName();
        String cluster = "cmcluster";
        String imageSettings = "imageSettingsUpgrade";

        testContext
                .given(EnvironmentTestDto.class)
                .withCreateFreeIpa(Boolean.FALSE)
                .withName(resourcePropertyProvider().getEnvironmentName())
                .withBackup("location/of/the/backup")
                .when(getEnvironmentTestClient().create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.create())
                .awaitForCreationFlow()
                .given(cluster, ClusterTestDto.class)
                .given(imageSettings, ImageSettingsTestDto.class)
                .withImageId("aaa778fc-7f17-4535-9021-515351df3691")
                .withImageCatalog(upgradeImageCatalogName)
                .given(stack, StackTestDto.class)
                .withCluster(cluster)
                .withImageSettings(imageSettings)
                .given(sdxInternal, SdxInternalTestDto.class)
                .withStackRequest(key(cluster), key(stack))
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING)
                .then(SdxUpgradeTestAssertion.validateUpgradeCandidateWithLockedComponentIsAvailable())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running Cloudbreak",
            when = "start an sdx cluster without attached disk on gateway, but disk attachment is supported on cloud provider side",
            then = "Upgrade option should be presented"
    )
    public void testSdxUpgradeAfterResize(MockedTestContext testContext) {
        String upgradeImageCatalogName = resourcePropertyProvider().getName();
        createImageCatalogForOsUpgrade(testContext, upgradeImageCatalogName);
        String sdxInternal = resourcePropertyProvider().getName();
        String stack = resourcePropertyProvider().getName();
        String cluster = "cmcluster";
        String imageSettings = "imageSettingsUpgrade";

        testContext
                .given(EnvironmentTestDto.class)
                .withCreateFreeIpa(Boolean.FALSE)
                .withName(resourcePropertyProvider().getEnvironmentName())
                .withBackup("location/of/the/backup")
                .when(getEnvironmentTestClient().create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.create())
                .awaitForCreationFlow()
                .given(cluster, ClusterTestDto.class)
                .given(imageSettings, ImageSettingsTestDto.class)
                .withImageId("aaa778fc-7f17-4535-9021-515351df3691")
                .withImageCatalog(upgradeImageCatalogName)
                .given("NoAttachedDisksTemplate", InstanceTemplateV4TestDto.class)
                .withAttachedVolume(testContext.init(VolumeV4TestDto.class).withCount(0))
                .given("InstanceGroupWithoutAttachedDisk", InstanceGroupTestDto.class)
                .withHostGroup(HostGroupType.MASTER)
                .withTemplate("NoAttachedDisksTemplate")
                .given(stack, StackTestDto.class)
                .withCluster(cluster)
                .withImageSettings(imageSettings)
                .replaceInstanceGroups("InstanceGroupWithoutAttachedDisk")
                .given(sdxInternal, SdxInternalTestDto.class)
                .withStackRequest(key(cluster), key(stack))
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING)
                .when(sdxTestClient.resize(), key(sdxInternal))
                .await(SdxClusterStatusResponse.STOP_IN_PROGRESS, key(sdxInternal).withWaitForFlow(Boolean.FALSE))
                .await(SdxClusterStatusResponse.STACK_CREATION_IN_PROGRESS, key(sdxInternal).withWaitForFlow(Boolean.FALSE))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxInternal).withWaitForFlow(Boolean.FALSE))
                .withClusterShape(SdxClusterShape.MEDIUM_DUTY_HA)
                .then(SdxUpgradeTestAssertion.validateUpgradeCandidateWithLockedComponentIsAvailable())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running Cloudbreak",
            when = "upgrade the cluster but during the process of backup, cancel the backup",
            then = "stack status reason should be datalake cancelled when stack turns back to be RUNNING"
    )
    public void testSdxUpgradeFailedWithBackupCancelled(MockedTestContext testContext) {
        String sdxName = resourcePropertyProvider().getName();
        String upgradeImageCatalogName = resourcePropertyProvider().getName();
        createImageCatalogForOsUpgrade(testContext, upgradeImageCatalogName);
        String sdxInternal = resourcePropertyProvider().getName();
        String stack = resourcePropertyProvider().getName();
        String cluster = "cmcluster";
        String imageSettings = "imageSettingsUpgrade";

        testContext
                .given(EnvironmentTestDto.class)
                .withCreateFreeIpa(Boolean.FALSE)
                .withName(resourcePropertyProvider().getEnvironmentName())
                .withBackup("location/of/the/backup/cancel")
                .when(getEnvironmentTestClient().create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.create())
                .awaitForCreationFlow()
                .given(cluster, ClusterTestDto.class)
                .given(imageSettings, ImageSettingsTestDto.class)
                .withImageId("aaa778fc-7f17-4535-9021-515351df3691")
                .withImageCatalog(upgradeImageCatalogName)
                .given("NoAttachedDisksTemplate", InstanceTemplateV4TestDto.class)
                .withAttachedVolume(testContext.init(VolumeV4TestDto.class).withCount(0))
                .given("InstanceGroupWithoutAttachedDisk", InstanceGroupTestDto.class)
                .withHostGroup(HostGroupType.MASTER)
                .withTemplate("NoAttachedDisksTemplate")
                .given(stack, StackTestDto.class)
                .withCluster(cluster)
                .withImageSettings(imageSettings)
                .replaceInstanceGroups("InstanceGroupWithoutAttachedDisk")
                .given(sdxInternal, SdxInternalTestDto.class)
                .withStackRequest(key(cluster), key(stack))
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING)
                .given(SdxUpgradeTestDto.class)
                .setSkipBackup(Boolean.FALSE)
                .withRuntime(null)
                .withLockComponents(true)
                .withReplaceVms(SdxUpgradeReplaceVms.ENABLED)
                .given(sdxInternal, SdxInternalTestDto.class)
                .then(setCmVersionInMockToUpgradedVersion())
                .when(sdxTestClient.upgradeInternal(), key(sdxInternal))
                .await(SdxClusterStatusResponse.DATALAKE_BACKUP_INPROGRESS, key(sdxName).withWaitForFlow(Boolean.FALSE))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxName).withWaitForFlow(Boolean.FALSE))
                .then((tc, testDto, client) -> {
                    SdxClusterDetailResponse sdx = testDto.getResponse();
                    assertEquals(sdx.getStatusReason(), "Datalake backup cancelled");
                    return testDto;
                })
                .validate();
    }

    protected ImageCatalogTestDto createImageCatalogForOsUpgrade(MockedTestContext testContext, String name) {
        return testContext
                .given(ImageCatalogTestDto.class)
                .withName(name)
                .withUrl(getImageCatalogMockServerSetup().getUpgradeImageCatalogUrl())
                .when(imageCatalogTestClient.createV4(), key(name));
    }
}

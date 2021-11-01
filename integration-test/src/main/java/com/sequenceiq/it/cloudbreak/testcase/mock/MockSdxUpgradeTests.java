package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.UUID;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.auth.crn.TestCrnGenerator;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkMockParams;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.assertion.datalake.SdxUpgradeTestAssertion;
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
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class MockSdxUpgradeTests extends AbstractMockTest {

    @Inject
    private RedbeamsDatabaseServerTestClient redbeamsDatabaseServerTestClient;

    @Inject
    private ImageCatalogTestClient imageCatalogTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

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
        String networkKey = "someOtherNetwork";
        testContext
                .given(networkKey, EnvironmentNetworkTestDto.class)
                .withMock(new EnvironmentNetworkMockParams())
                .given(EnvironmentTestDto.class)
                .withNetwork(networkKey)
                .withCreateFreeIpa(Boolean.FALSE)
                .withName(resourcePropertyProvider().getEnvironmentName())
                .when(getEnvironmentTestClient().create())
                .await(EnvironmentStatus.AVAILABLE)
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
                .then(SdxUpgradeTestAssertion.validateSuccessfulUpgrade())
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
        String networkKey = "someOtherNetwork";
        String clusterCrn = TestCrnGenerator.getDatalakeCrn(UUID.randomUUID().toString(), "cloudera");

        testContext
                .given(networkKey, EnvironmentNetworkTestDto.class)
                .withMock(new EnvironmentNetworkMockParams())
                .given(EnvironmentTestDto.class)
                .withNetwork(networkKey)
                .withCreateFreeIpa(Boolean.FALSE)
                .withName(resourcePropertyProvider().getEnvironmentName())
                .when(getEnvironmentTestClient().create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(RedbeamsDatabaseServerTestDto.class)
                .withEnvironmentCrn(testContext.get(EnvironmentTestDto.class).getResponse().getCrn())
                .withClusterCrn(clusterCrn)
                .when(redbeamsDatabaseServerTestClient.createV4())
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
                .then(SdxUpgradeTestAssertion.validateSuccessfulUpgrade())
                .validate();
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
        String networkKey = "someOtherNetwork";

        testContext
                .given(networkKey, EnvironmentNetworkTestDto.class)
                .withMock(new EnvironmentNetworkMockParams())
                .given(EnvironmentTestDto.class)
                .withNetwork(networkKey)
                .withCreateFreeIpa(Boolean.FALSE)
                .withName(resourcePropertyProvider().getEnvironmentName())
                .withBackup("location/of/the/backup")
                .when(getEnvironmentTestClient().create())
                .await(EnvironmentStatus.AVAILABLE)
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
                .then(SdxUpgradeTestAssertion.validateSuccessfulUpgrade())
                .validate();
    }

//    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
//    @Description(
//            given = "there is a running Cloudbreak",
//            when = "start an sdx cluster without attached disk on gateway, but disk attachment is supported on cloud provider side",
//            then = "Upgrade option should be presented"
//    )
//    public void testSdxUpgradeAfterResize(MockedTestContext testContext) {
//        String upgradeImageCatalogName = resourcePropertyProvider().getName();
//        createImageCatalogForOsUpgrade(testContext, upgradeImageCatalogName);
//        String sdxInternal = resourcePropertyProvider().getName();
//        String stack = resourcePropertyProvider().getName();
//        String cluster = "cmcluster";
//        String imageSettings = "imageSettingsUpgrade";
//        String networkKey = "someOtherNetwork";
//
//        testContext
//                .given(networkKey, EnvironmentNetworkTestDto.class)
//                .withMock(new EnvironmentNetworkMockParams())
//                .given(EnvironmentTestDto.class)
//                .withNetwork(networkKey)
//                .withCreateFreeIpa(Boolean.FALSE)
//                .withName(resourcePropertyProvider().getEnvironmentName())
//                .withBackup("location/of/the/backup")
//                .when(getEnvironmentTestClient().create())
//                .await(EnvironmentStatus.AVAILABLE)
//                .given(cluster, ClusterTestDto.class)
//                .given(imageSettings, ImageSettingsTestDto.class)
//                .withImageId("aaa778fc-7f17-4535-9021-515351df3691")
//                .withImageCatalog(upgradeImageCatalogName)
//                .given("NoAttachedDisksTemplate", InstanceTemplateV4TestDto.class)
//                .withAttachedVolume(testContext.init(VolumeV4TestDto.class).withCount(0))
//                .given("InstanceGroupWithoutAttachedDisk", InstanceGroupTestDto.class)
//                .withHostGroup(HostGroupType.MASTER)
//                .withTemplate("NoAttachedDisksTemplate")
//                .given(stack, StackTestDto.class)
//                .withCluster(cluster)
//                .withImageSettings(imageSettings)
//                .replaceInstanceGroups("InstanceGroupWithoutAttachedDisk")
//                .given(sdxInternal, SdxInternalTestDto.class)
//                .withStackRequest(key(cluster), key(stack))
//                .when(sdxTestClient.createInternal(), key(sdxInternal))
//                .await(SdxClusterStatusResponse.RUNNING)
//                .when(sdxTestClient.resize(), key(sdxInternal))
//                .await(SdxClusterStatusResponse.STOP_IN_PROGRESS, key(sdxInternal).withWaitForFlow(Boolean.FALSE))
//                .await(SdxClusterStatusResponse.STACK_CREATION_IN_PROGRESS, key(sdxInternal).withWaitForFlow(Boolean.FALSE))
//                .await(SdxClusterStatusResponse.RUNNING, key(sdxInternal).withWaitForFlow(Boolean.FALSE))
//                .withClusterShape(SdxClusterShape.MEDIUM_DUTY_HA)
//                .then(SdxUpgradeTestAssertion.validateSuccessfulUpgrade())
//                .validate();
//    }

    protected ImageCatalogTestDto createImageCatalogForOsUpgrade(MockedTestContext testContext, String name) {
        return testContext
                .given(ImageCatalogTestDto.class)
                .withName(name)
                .withUrl(getImageCatalogMockServerSetup().getUpgradeImageCatalogUrl())
                .when(imageCatalogTestClient.createV4(), key(name));
    }
}

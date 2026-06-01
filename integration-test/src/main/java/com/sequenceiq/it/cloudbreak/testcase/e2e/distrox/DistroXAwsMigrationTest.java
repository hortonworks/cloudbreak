package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeReplaceVms;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXUpgradeTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxUpgradeTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxUpgradeReplaceVms;

public class DistroXAwsMigrationTest extends AbstractE2ETest {

    @Inject
    private ImageCatalogTestClient imageCatalogTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Inject
    private SdxTestClient sdxTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        assertSupportedCloudPlatform(CloudPlatform.AWS);
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running cloudbreak and existing environment, distrox, datalake",
            when = "a valid DistroX",
            then = "DistroX cluster is multiAZ")
    public void testAwsNativeMigration(TestContext testContext) {
        String targetVersion = commonClusterManagerProperties.getLatestRuntimeVersion();
        String currentVersion = commonClusterManagerProperties.getUpgrade().getMatrix().get(targetVersion);
        createDatalakeWithVersion(testContext, currentVersion);

        SdxInternalTestDto sdxInternalTestDto = testContext.get(SdxInternalTestDto.class);
        useExistingDatalake(testContext, sdxInternalTestDto.getName());

        testContext
                .given(DistroXTestDto.class)
                .withTemplate(commonClusterManagerProperties.getDataEngDistroXBlueprintName(currentVersion))
                .withLoadBalancer()
                .withVariant("AWS")
                .when(distroXTestClient.create())
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .when(distroXTestClient.checkVariant("AWS"))
                .when(distroXTestClient.stop())
                .await(STACK_STOPPED)
                .given(SdxUpgradeTestDto.class)
                .withReplaceVms(SdxUpgradeReplaceVms.ENABLED)
                .withRuntime(targetVersion)
                .given(SdxTestDto.class)
                .when(sdxTestClient.upgrade())
                .await(SdxClusterStatusResponse.RUNNING)
                .given(DistroXTestDto.class)
                .when(distroXTestClient.start())
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .given(DistroXUpgradeTestDto.class)
                .withReplaceVms(DistroXUpgradeReplaceVms.ENABLED)
                .withRuntime(targetVersion)
                .given(DistroXTestDto.class)
                .when(distroXTestClient.upgrade())
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .when(distroXTestClient.checkVariant("AWS_NATIVE"))
                .validate();
        //check the other functions
        testContext
                .given(DistroXTestDto.class)
                .when(distroXTestClient.stop())
                .await(STACK_STOPPED)
                .when(distroXTestClient.checkVariant("AWS_NATIVE"))
                .when(distroXTestClient.start())
                .await(STACK_AVAILABLE)
                .when(distroXTestClient.checkVariant("AWS_NATIVE"))
                .when(distroXTestClient.scale("compute", 3))
                .await(STACK_AVAILABLE)
                .when(distroXTestClient.checkVariant("AWS_NATIVE"))
                .when(distroXTestClient.scale("compute", 1))
                .await(STACK_AVAILABLE)
                .when(distroXTestClient.checkVariant("AWS_NATIVE"))
                .when(distroXTestClient.delete())
                .await(STACK_DELETED)
                .validate();

    }
}

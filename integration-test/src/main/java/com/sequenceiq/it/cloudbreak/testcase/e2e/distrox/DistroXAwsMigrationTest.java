package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeReplaceVms;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXUpgradeTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXImageTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;

public class DistroXAwsMigrationTest extends AbstractE2ETest {

    @Inject
    private ImageCatalogTestClient imageCatalogTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
        createEnvironmentWithFreeIpaAndDatalake(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running cloudbreak and existin environment, distrox, datalake",
            when = "a valid DistroX",
            then = "DistroX cluster is mutliaz")
    public void testAwsNativeMigration(TestContext testContext) {
        String imgCatalogKey = "os-upgrade-img-cat";
        testContext
                .given(imgCatalogKey, ImageCatalogTestDto.class)
                .withName(imgCatalogKey)
                .withUrl("https://gist.githubusercontent.com/topolyai5/387af3f4b4914b0dab6e7090552bbcee/raw/f3531920f87f1bcc75aa2a28b042a9d47b5b36a7/" +
                        "os-upgrade-image-catalog.json")
                .when(imageCatalogTestClient.createIfNotExistV4())
                .given(DistroXImageTestDto.class)
                .withImageCatalog(imgCatalogKey)
                .given(DistroXTestDto.class)
                .withTemplate(String.format(commonClusterManagerProperties.getInternalDistroXBlueprintType(), commonClusterManagerProperties.getUpgrade()
                        .getDistroXUpgradeCurrentVersion()))
                .withImageSettings()
                .when(distroXTestClient.create())
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .when(distroXTestClient.checkVariant("AWS"))
                .given(DistroXUpgradeTestDto.class)
                .withReplaceVms(DistroXUpgradeReplaceVms.ENABLED)
                .withRuntime(commonClusterManagerProperties.getUpgrade().getDistroXUpgradeCurrentVersion())
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

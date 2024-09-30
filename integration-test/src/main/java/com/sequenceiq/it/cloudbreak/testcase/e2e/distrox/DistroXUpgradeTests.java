package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.assertion.distrox.AwsAvailabilityZoneAssertion;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXUpgradeTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXImageTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.DistroxUtil;
import com.sequenceiq.it.cloudbreak.util.FreeIpaInstanceUtil;
import com.sequenceiq.it.cloudbreak.util.RecipeUtil;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

public class DistroXUpgradeTests extends AbstractE2ETest {

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private ImageCatalogTestClient imageCatalogTest;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Inject
    private ImageCatalogTestClient imageCatalogTestClient;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private RecipeTestClient recipeTestClient;

    @Inject
    private RecipeUtil recipeUtil;

    @Inject
    private FreeIpaInstanceUtil freeIpaInstanceUtil;

    @Inject
    private SdxUtil sdxUtil;

    @Inject
    private DistroxUtil distroxUtil;

    @Inject
    private EncryptedTestUtil encryptedTestUtil;

    private String resourceGroupForTest;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak, and an environment with SDX and two DistroX clusters in " +
                    "available state, one cluster created with deafult catalog and one cluster created with production catalog" +
                    "disk are encrypted",
            when = "upgrade called on both DistroX clusters",
            then = "Both DistroX upgrade should be successful," + " the clusters should be up and running" +
                    "disks are encrypted too")
    public void testDistroXUpgradesWithEncryptedDisks(TestContext testContext) {

        boolean govCloud = testContext.getCloudProvider().getGovCloud();
        String currentUpgradeRuntimeVersion = commonClusterManagerProperties.getUpgrade()
                .getDistroXUpgradeCurrentVersion(govCloud);
        String targetRuntimeVersion = commonClusterManagerProperties.getUpgrade().getDistroXUpgradeTargetVersion();
        String currentRuntimeVersion3rdParty = commonClusterManagerProperties.getUpgrade()
                .getDistroXUpgrade3rdPartyCurrentVersion(govCloud);
        String targetRuntimeVersion3rdParty = commonClusterManagerProperties.getUpgrade().getDistroXUpgrade3rdPartyTargetVersion();

        String firstDhName = resourcePropertyProvider().getName();
        String secondDhName = resourcePropertyProvider().getName();

        encryptedTestUtil.createEnvironment(testContext);
        encryptedTestUtil.createFreeipa(testContext, commonCloudProperties());
        encryptedTestUtil.doFreeipUserSync(testContext);
        encryptedTestUtil.assertEnvironmentAndFreeipa(testContext, null);
        createDatalake(testContext, currentUpgradeRuntimeVersion);
        encryptedTestUtil.assertDatalake(testContext, null);
        createDataHubs(testContext, currentUpgradeRuntimeVersion, currentRuntimeVersion3rdParty, firstDhName, secondDhName);
        encryptedTestUtil.assertDatahubWithName(testContext, null, firstDhName);
        encryptedTestUtil.assertDatahubWithName(testContext, null, secondDhName);
        upgradeAndAssertUpgrade(testContext, targetRuntimeVersion, targetRuntimeVersion3rdParty, firstDhName, secondDhName);
    }

    private void upgradeAndAssertUpgrade(TestContext testContext, String targetRuntimeVersion, String targetRuntimeVersion3rdParty,
            String firstDhName, String secondDhName) {
        testContext
                .given(DistroXUpgradeTestDto.class)
                .withRuntime(targetRuntimeVersion)
                .given(firstDhName, DistroXTestDto.class)
                .when(distroXTestClient.upgrade(), key(firstDhName))
                .given(DistroXUpgradeTestDto.class)
                .withRuntime(targetRuntimeVersion3rdParty)
                .given(secondDhName, DistroXTestDto.class)
                .when(distroXTestClient.upgrade(), key(secondDhName))
                .await(STACK_AVAILABLE, key(secondDhName))
                .awaitForHealthyInstances()
                .given(firstDhName, DistroXTestDto.class)
                .await(STACK_AVAILABLE, key(firstDhName))
                .awaitForHealthyInstances()
                .then(new AwsAvailabilityZoneAssertion(), key(firstDhName))
                .validate();
    }

    private void createDataHubs(TestContext testContext, String currentUpgradeRuntimeVersion, String currentRuntimeVersion3rdParty,
            String firstDhName, String secondDhName) {
        String thirdPartyCatalogName = resourcePropertyProvider().getName();
        String imageName = resourcePropertyProvider().getName();

        testContext
                .given(firstDhName, DistroXTestDto.class)
                .withTemplate(commonClusterManagerProperties.getDataEngDistroXBlueprintName(currentUpgradeRuntimeVersion))
                .withPreferredSubnetsForInstanceNetworkIfMultiAzEnabledOrJustFirst()
                .when(distroXTestClient.create(), key(firstDhName))
                .await(STACK_AVAILABLE, key(firstDhName))
                .awaitForHealthyInstances()
                .validate();

        testContext.given(ImageCatalogTestDto.class)
                .withUrl(commonClusterManagerProperties.getUpgrade().getImageCatalogUrl3rdParty())
                .withoutCleanup()
                .withName(thirdPartyCatalogName)
            .when(imageCatalogTestClient.createIfNotExistV4());

        testContext
                .given(imageName, DistroXImageTestDto.class)
                .withImageCatalog(thirdPartyCatalogName)
                .given(secondDhName, DistroXTestDto.class)
                .withTemplate(commonClusterManagerProperties.getDataEngDistroXBlueprintName(currentRuntimeVersion3rdParty))
                .withPreferredSubnetsForInstanceNetworkIfMultiAzEnabledOrJustFirst()
                .withImageSettings(imageName)
                .when(distroXTestClient.create(), key(secondDhName))
                .await(STACK_AVAILABLE, key(secondDhName))
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> checkImageCatalog(testDto, thirdPartyCatalogName), key(secondDhName))
                .given(secondDhName, DistroXTestDto.class)
                .await(STACK_AVAILABLE, key(secondDhName))
                .awaitForHealthyInstances()
                .then(new AwsAvailabilityZoneAssertion(), key(secondDhName))
                .validate();
    }

    private void createDatalake(TestContext testContext, String currentRuntimeVersion) {
        testContext
                .given(SdxTestDto.class)
                .withRuntimeVersion(currentRuntimeVersion)
                .withCloudStorage(getCloudStorageRequest(testContext))
                .withExternalDatabase(sdxDbRequest())
                .when(sdxTestClient.create())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .validate();
    }

    private SdxDatabaseRequest sdxDbRequest() {
        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NONE);

        return sdxDatabaseRequest;
    }

    private DistroXTestDto checkImageCatalog(DistroXTestDto testDto, String expectedImageCatalog) {
        String currentImageCatalog = testDto.getResponse().getImage().getCatalogName();
        if (!currentImageCatalog.equals(expectedImageCatalog)) {
            throw new TestFailException("The selected image catalog is: " + currentImageCatalog + " instead of: "
                    + expectedImageCatalog);
        }
        return testDto;
    }

}

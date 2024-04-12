package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
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
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
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

        String currentUpgradeRuntimeVersion = commonClusterManagerProperties.getUpgrade().getDistroXUpgradeCurrentVersion();
        String targetRuntimeVersion = commonClusterManagerProperties.getUpgrade().getDistroXUpgradeTargetVersion();
        String currentRuntimeVersion3rdParty = commonClusterManagerProperties.getUpgrade().getDistroXUpgrade3rdPartyCurrentVersion();
        String targetRuntimeVersion3rdParty = commonClusterManagerProperties.getUpgrade().getDistroXUpgrade3rdPartyTargetVersion();

        String firstDhName = resourcePropertyProvider().getName();
        String secondDhName = resourcePropertyProvider().getName();

        encryptedTestUtil.createEnvironment(testContext);
        encryptedTestUtil.createFreeipa(testContext, commonCloudProperties());
        encryptedTestUtil.doFreeipUserSync(testContext);
        encryptedTestUtil.assertEnvironmentAndFreeipa(testContext, null);
        createDatalake(testContext, currentUpgradeRuntimeVersion);
        encryptedTestUtil.assertDatalake(testContext, null);
        createDatahubs(testContext, currentUpgradeRuntimeVersion, currentRuntimeVersion3rdParty, firstDhName, secondDhName);
        encryptedTestUtil.assertDatahubWithName(testContext, null, firstDhName);
        encryptedTestUtil.assertDatahubWithName(testContext, null, secondDhName);
        upgradeAndAssertUpgrade(testContext, targetRuntimeVersion, targetRuntimeVersion3rdParty, firstDhName, secondDhName);
    }

    protected String getUuid(TestContext testContext, String prodCatalogName, String currentRuntimeVersion3rdParty) {
        testContext
                .given(ImageCatalogTestDto.class).withName(prodCatalogName)
                .when(imageCatalogTest.getV4(true));
        ImageCatalogTestDto dto = testContext.get(ImageCatalogTestDto.class);
        return dto.getResponse().getImages().getCdhImages().stream()
                .filter(img -> img.getVersion().equals(currentRuntimeVersion3rdParty) && img.getImageSetsByProvider().keySet().stream().iterator().next()
                        .equals(testContext.commonCloudProperties().getCloudProvider().toLowerCase(Locale.ROOT))).iterator().next().getUuid();
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

    private void createDatahubs(TestContext testContext, String currentUpgradeRuntimeVersion, String currentRuntimeVersion3rdParty,
            String firstDhName, String secondDhName) {
        AtomicReference<String> uuid = new AtomicReference<>();
        String thirdPartyCatalogName = resourcePropertyProvider().getName();
        String imageName = resourcePropertyProvider().getName();

        testContext
                .given(firstDhName, DistroXTestDto.class)
                .withTemplate(commonClusterManagerProperties.getInternalDistroXBlueprintName(currentUpgradeRuntimeVersion))
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

        uuid.set(getUuid(testContext, thirdPartyCatalogName, currentRuntimeVersion3rdParty));
        testContext
                .given(imageName, DistroXImageTestDto.class).withImageCatalog(thirdPartyCatalogName)
                .withImageId(uuid.get())
                .given(secondDhName, DistroXTestDto.class)
                .withTemplate(commonClusterManagerProperties.getInternalDistroXBlueprintName(currentRuntimeVersion3rdParty))
                .withPreferredSubnetsForInstanceNetworkIfMultiAzEnabledOrJustFirst()
                .withImageSettings(imageName)
                .when(distroXTestClient.create(), key(secondDhName))
                .await(STACK_AVAILABLE, key(secondDhName))
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> checkImageId(testDto, uuid.get()), key(secondDhName))
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

    private DistroXTestDto checkImageId(DistroXTestDto testDto, String expectedImageId) {
        String currentImageId = testDto.getResponse().getImage().getId();
        if (!currentImageId.equals(expectedImageId)) {
            throw new TestFailException("The selected image ID is: " + currentImageId + " instead of: "
                    + expectedImageId);
        }
        return testDto;
    }

    private EnvironmentTestDto verifyEnvironmentResponseDiskEncryptionKey(TestContext testContext, EnvironmentTestDto testDto,
            EnvironmentClient environmentClient) {
        DetailedEnvironmentResponse environment = environmentClient.getDefaultClient().environmentV1Endpoint().getByName(testDto.getName());
        testContext.getCloudProvider().verifyDiskEncryptionKey(environment, testDto.getRequest().getName());
        return testDto;
    }

    private FreeIpaTestDto verifyFreeIpaVolumeEncryptionKey(TestContext testContext, FreeIpaTestDto testDto, FreeIpaClient freeIpaClient) {
        List<String> instanceIds = freeIpaInstanceUtil.getInstanceIds(testDto, freeIpaClient, MASTER.getName());
        List<String> volumeKmsKeyIds = new ArrayList<>(testContext.getCloudProvider().getCloudFunctionality()
                .listVolumeEncryptionKeyIds(testDto.getName(), resourceGroupForTest, instanceIds));
        testContext.getCloudProvider().verifyVolumeEncryptionKey(volumeKmsKeyIds, testContext.given(EnvironmentTestDto.class).getRequest().getName());
        return testDto;
    }
}

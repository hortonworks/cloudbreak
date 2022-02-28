package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import javax.inject.Inject;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.assertion.distrox.AwsAvailabilityZoneAssertion;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXUpgradeTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXImageTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxUpgradeTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxUpgradeReplaceVms;

import java.util.concurrent.atomic.AtomicReference;

public class DistroXUpgradeTests extends AbstractE2ETest {

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private ImageCatalogTestClient imageCatalogTest;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
        createEnvironmentWithFreeIpa(testContext);
    }

    protected String getUuid(TestContext testContext, String prodCatalogName, String currentRuntimeVersion3rdParty) {
        testContext
                .given(ImageCatalogTestDto.class).withName(prodCatalogName)
                .when(imageCatalogTest.getV4(true));
        ImageCatalogTestDto dto = testContext.get(ImageCatalogTestDto.class);
        return dto.getResponse().getImages().getCdhImages().stream()
                .filter(img -> img.getVersion().equals(currentRuntimeVersion3rdParty) && img.getImageSetsByProvider().keySet().stream().iterator().next()
                        .equals(testContext.commonCloudProperties().getCloudProvider().toLowerCase())).iterator().next().getUuid();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak, and an environment with SDX and two DistroX clusters in " +
            "available state, one cluster created with deafult catalog and one cluster created with production catalog",
            when = "upgrade called on both DistroX clusters",
            then = "Both DistroX upgrade should be successful," + " the clusters should be up and running")
    public void testDistroXUpgrades(TestContext testContext) {
        String imageSettings = resourcePropertyProvider().getName();
        String currentRuntimeVersion = commonClusterManagerProperties.getUpgrade().getDistroXUpgradeCurrentVersion();
        String targetRuntimeVersion = commonClusterManagerProperties.getUpgrade().getDistroXUpgradeTargetVersion();
        String currentRuntimeVersion3rdParty = commonClusterManagerProperties.getUpgrade().getDistroXUpgrade3rdPartyCurrentVersion();
        String targetRuntimeVersion3rdParty = commonClusterManagerProperties.getUpgrade().getDistroXUpgrade3rdPartyTargetVersion();
        String sdxName = resourcePropertyProvider().getName();
        String distroXName = resourcePropertyProvider().getName();
        String distroX3rdPartyName = resourcePropertyProvider().getName();
        String thirdPartyCatalogName = resourcePropertyProvider().getName();
        AtomicReference<String> uuid = new AtomicReference<>();
        testContext
                .given(sdxName, SdxTestDto.class)
                .withRuntimeVersion(currentRuntimeVersion)
                .withCloudStorage(getCloudStorageRequest(testContext))
                .when(sdxTestClient.create(), key(sdxName))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxName))
                .awaitForHealthyInstances()
                .validate();
        testContext
                .given(distroXName, DistroXTestDto.class)
                .withTemplate(String.format(commonClusterManagerProperties.getInternalDistroXBlueprintType(), currentRuntimeVersion))
                .withPreferredSubnetsForInstanceNetworkIfMultiAzEnabledOrJustFirst()
                .when(distroXTestClient.create(), key(distroXName))
                .validate();
        createImageValidationSourceCatalog(testContext, commonClusterManagerProperties.getUpgrade()
                .getImageCatalogUrl3rdParty(), thirdPartyCatalogName);
        uuid.set(getUuid(testContext, thirdPartyCatalogName, currentRuntimeVersion3rdParty));
        testContext
                .given(imageSettings, DistroXImageTestDto.class).withImageCatalog(thirdPartyCatalogName)
                .withImageId(uuid.get())
                .given(distroX3rdPartyName, DistroXTestDto.class)
                .withTemplate(String.format(commonClusterManagerProperties.getInternalDistroXBlueprintType(), currentRuntimeVersion3rdParty))
                .withPreferredSubnetsForInstanceNetworkIfMultiAzEnabledOrJustFirst()
                .withImageSettings(imageSettings)
                .when(distroXTestClient.create(), key(distroX3rdPartyName))
                .await(STACK_AVAILABLE, key(distroX3rdPartyName))
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> checkImageId(testDto, uuid.get()))
                .given(distroXName, DistroXTestDto.class)
                .await(STACK_AVAILABLE, key(distroXName))
                .awaitForHealthyInstances()
                .then(new AwsAvailabilityZoneAssertion())
                .validate();
        testContext
                .given(distroXName, DistroXTestDto.class)
                .when(distroXTestClient.stop(), key(distroXName))
                .given(distroX3rdPartyName, DistroXTestDto.class)
                .when(distroXTestClient.stop(), key(distroX3rdPartyName))
                .await(STACK_STOPPED, key(distroX3rdPartyName))
                .given(distroXName, DistroXTestDto.class)
                .await(STACK_STOPPED, key(distroXName))
                .validate();
        testContext
                .given(SdxUpgradeTestDto.class)
                .withReplaceVms(SdxUpgradeReplaceVms.DISABLED)
                .withRuntime(targetRuntimeVersion)
                .given(sdxName, SdxTestDto.class)
                .when(sdxTestClient.upgrade(), key(sdxName))
                .await(SdxClusterStatusResponse.DATALAKE_UPGRADE_IN_PROGRESS, key(sdxName).withWaitForFlow(Boolean.FALSE))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxName))
                .awaitForHealthyInstances()
                .validate();
        testContext
                .given(distroXName, DistroXTestDto.class)
                .when(distroXTestClient.start(), key(distroXName))
                .given(distroX3rdPartyName, DistroXTestDto.class)
                .when(distroXTestClient.start(), key(distroX3rdPartyName))
                .await(STACK_AVAILABLE, key(distroX3rdPartyName))
                .awaitForHealthyInstances()
                .given(distroXName, DistroXTestDto.class)
                .await(STACK_AVAILABLE, key(distroXName))
                .awaitForHealthyInstances()
                .validate();
        testContext
                .given(DistroXUpgradeTestDto.class)
                .withRuntime(targetRuntimeVersion)
                .given(distroXName, DistroXTestDto.class)
                .when(distroXTestClient.upgrade(), key(distroXName))
                .given(DistroXUpgradeTestDto.class)
                .withRuntime(targetRuntimeVersion3rdParty)
                .given(distroX3rdPartyName, DistroXTestDto.class)
                .when(distroXTestClient.upgrade(), key(distroX3rdPartyName))
                .await(STACK_AVAILABLE, key(distroX3rdPartyName))
                .awaitForHealthyInstances()
                .given(distroXName, DistroXTestDto.class)
                .await(STACK_AVAILABLE, key(distroXName))
                .awaitForHealthyInstances()
                .then(new AwsAvailabilityZoneAssertion())
                .validate();
    }

    private DistroXTestDto checkImageId(DistroXTestDto testDto, String expectedImageId) {
        String currentImageId = testDto.getResponse().getImage().getId();
        if (!currentImageId.equals(expectedImageId)) {
            throw new TestFailException("The selected image ID is: " + currentImageId + " instead of: "
                    + expectedImageId);
        }
        return testDto;
    }
}
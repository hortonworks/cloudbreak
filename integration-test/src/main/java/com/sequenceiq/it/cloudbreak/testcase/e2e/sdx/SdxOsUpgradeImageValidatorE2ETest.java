package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.it.util.imagevalidation.ImageValidatorE2ETest;
import com.sequenceiq.it.util.imagevalidation.ImageValidatorE2ETestUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

public class SdxOsUpgradeImageValidatorE2ETest extends PreconditionSdxE2ETest implements ImageValidatorE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxOsUpgradeImageValidatorE2ETest.class);

    @Inject
    private ImageValidatorE2ETestUtil imageValidatorE2ETestUtil;

    @Inject
    private SdxUpgradeDatabaseTestUtil sdxUpgradeDatabaseTestUtil;

    private ImageV4Response latestImageWithSameRuntime;

    @Override
    protected void setupTest(TestContext testContext) {
        imageValidatorE2ETestUtil.setupTest(testContext);
        latestImageWithSameRuntime = imageValidatorE2ETestUtil.getLatestImageWithSameRuntimeAsImageUnderValidation(testContext);
        if (latestImageWithSameRuntime != null) {
            super.setupTest(testContext);
        }
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "an SDX cluster with the latest image with same runtime components as the validated image is in available state",
            and = "it uses an old DB version",
            when = "upgrade called on the SDX cluster to the validated image",
            then = "SDX OS upgrade should be successful, the cluster should be up and running and using a newer DB version"
    )
    public void testSDXOsUpgrade(TestContext testContext) {
        if (latestImageWithSameRuntime == null) {
            Log.log(LOGGER, "SKIP - There are no older images with the same runtime components, so OS upgrade testing is not possible");
            return;
        }

        String originalDatabaseMajorVersion = sdxUpgradeDatabaseTestUtil.getOriginalDatabaseMajorVersion();
        TargetMajorVersion targetDatabaseMajorVersion = sdxUpgradeDatabaseTestUtil.getTargetMajorVersion();

        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NONE);
        sdxDatabaseRequest.setDatabaseEngineVersion(originalDatabaseMajorVersion);

        testContext
                .given(SdxInternalTestDto.class)
                    .withCloudStorage()
                    .withoutDatabase()
                    .withTemplate(commonClusterManagerProperties().getInternalSdxBlueprintName())
                    .withImageCatalogNameAndImageId(commonCloudProperties().getImageValidation().getSourceCatalogName(), latestImageWithSameRuntime.getUuid())
                    .withDatabase(sdxDatabaseRequest)
                .when(sdxTestClient().createInternal())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> sdxUpgradeDatabaseTestUtil.checkCloudProviderDatabaseVersionFromMasterNode(
                        originalDatabaseMajorVersion, tc, testDto))
                .when(sdxTestClient().osUpgradeInternal(commonCloudProperties().getImageValidation().getImageUuid()))
                .await(SdxClusterStatusResponse.DATALAKE_UPGRADE_IN_PROGRESS, emptyRunningParameter().withWaitForFlow(Boolean.FALSE))
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> sdxUpgradeDatabaseTestUtil.checkCloudProviderDatabaseVersionFromMasterNode(
                        targetDatabaseMajorVersion.getMajorVersion(), tc, testDto))
                .validate();
    }
}

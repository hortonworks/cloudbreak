package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.IDBROKER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static java.lang.String.format;

import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CloudProvider;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.ImageSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXImageTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.testcase.e2e.sdx.PreconditionSdxE2ETest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

public class DistroXMarketplaceImageTests extends PreconditionSdxE2ETest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXMarketplaceImageTests.class);

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private ImageCatalogTestClient imageCatalogTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
        initalizeAzureMarketplaceTermsPolicy(testContext);
        createEnvironmentWithFreeIpa(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak",
            when = "a valid SDX then DistroX create request is sent (latest Marketplace Image)",
            then = "SDX and DistroX should be available AND deletable"
    )
    public void testSDXAndDistroXWithMarketplaceImageCanBeCreatedSuccessfully(TestContext testContext) {
        String sdxInternal = resourcePropertyProvider().getName();
        String cluster = resourcePropertyProvider().getName();
        String clouderaManager = resourcePropertyProvider().getName();
        String imageSettings = resourcePropertyProvider().getName();
        String dhImageSettings = resourcePropertyProvider().getName();
        String stack = resourcePropertyProvider().getName();
        String masterInstanceGroup = "master";
        String idbrokerInstanceGroup = "idbroker";
        String telemetry = "telemetry";
        CloudProvider cloudProvider = testContext.getCloudProvider();
        AtomicReference<String> selectedImageID = new AtomicReference<>();

        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NONE);

        String distrox = resourcePropertyProvider().getName();

        String imgCatalogKey = "mp-img-cat";
        testContext
                .given(imgCatalogKey, ImageCatalogTestDto.class)
                .withName(imgCatalogKey)
                .withUrl("https://cloudbreak-imagecatalog.s3.amazonaws.com/v3-marketplace-image-catalog.json")
                .when(imageCatalogTestClient.createIfNotExistV4())
                .when((tc, dto, client) -> {
                    selectedImageID.set(cloudProvider.getLatestPreWarmedImageIDByRuntime(tc, dto, client, "7.2.16"));
                    return dto;
                })
                .given(imageSettings, ImageSettingsTestDto.class)
                    .withImageCatalog(imgCatalogKey)
                    .withImageId(selectedImageID.get())
                .given(clouderaManager, ClouderaManagerTestDto.class)
                .given(cluster, ClusterTestDto.class)
                    .withBlueprintName(getDefaultSDXBlueprintName())
                    .withValidateBlueprint(Boolean.FALSE)
                    .withClouderaManager(clouderaManager)
                .given(masterInstanceGroup, InstanceGroupTestDto.class)
                    .withHostGroup(MASTER)
                    .withNodeCount(1)
                .given(idbrokerInstanceGroup, InstanceGroupTestDto.class)
                    .withHostGroup(IDBROKER)
                    .withNodeCount(1)
                .given(stack, StackTestDto.class)
                    .withCluster(cluster)
                    .withImageSettings(imageSettings)
                    .withInstanceGroups(masterInstanceGroup, idbrokerInstanceGroup)
                .given(telemetry, TelemetryTestDto.class)
                    .withLogging()
                    .withReportClusterLogs()
                .given(sdxInternal, SdxInternalTestDto.class)
                    .withDatabase(sdxDatabaseRequest)
                    .withCloudStorage(getCloudStorageRequest(testContext))
                    .withStackRequest(key(cluster), key(stack))
                    .withTelemetry(telemetry)
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .then((tc, dto, client) -> {
                    Log.log(LOGGER, format(" Image Catalog Name: %s ", dto.getResponse().getStackV4Response().getImage().getCatalogName()));
                    Log.log(LOGGER, format(" Image Catalog URL: %s ", dto.getResponse().getStackV4Response().getImage().getCatalogUrl()));
                    Log.log(LOGGER, format(" Image ID: %s ", dto.getResponse().getStackV4Response().getImage().getId()));

                    if (!dto.getResponse().getStackV4Response().getImage().getId().equals(selectedImageID.get())) {
                        throw new TestFailException(" The selected image ID is: " + dto.getResponse().getStackV4Response().getImage().getId() + " instead of: "
                                + selectedImageID.get());
                    }
                    return dto;
                })
                .given(dhImageSettings, DistroXImageTestDto.class)
                    .withImageCatalog(imgCatalogKey)
                    .withImageId(selectedImageID.get())
                .given(distrox, DistroXTestDto.class)
                    .withImageSettings(dhImageSettings)
                .when(distroXTestClient.create(), key(distrox))
                    .await(STACK_AVAILABLE)
                    .awaitForHealthyInstances()
                .then((tc, dto, client) -> {
                    Log.log(LOGGER, format(" Image Catalog Name: %s ", dto.getResponse().getImage().getCatalogName()));
                    Log.log(LOGGER, format(" Image Catalog URL: %s ", dto.getResponse().getImage().getCatalogUrl()));
                    Log.log(LOGGER, format(" Image ID: %s ", dto.getResponse().getImage().getId()));

                    if (!dto.getResponse().getImage().getId().equals(selectedImageID.get())) {
                        throw new TestFailException(" The selected image ID is: " + dto.getResponse().getImage().getId() + " instead of: "
                                + selectedImageID.get());
                    }
                    return dto;
                })
                .validate();
    }
}

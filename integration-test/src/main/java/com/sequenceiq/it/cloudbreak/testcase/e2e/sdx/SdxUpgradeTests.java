package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static java.lang.String.format;

import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CloudProvider;
import com.sequenceiq.it.cloudbreak.cloud.v4.aws.AwsCloudProvider;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.ImageSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.testcase.e2e.BasicSdxTests;
import com.sequenceiq.it.cloudbreak.util.wait.WaitUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxUpgradeTests extends BasicSdxTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(SdxUpgradeTests.class);

    @Inject
    private ImageCatalogTestClient imageCatalogTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private AwsCloudProvider awsCloudProvider;

    @Inject
    private WaitUtil waitUtil;

    @Value("sdx-upgrade-test-catalog")
    private String imageCatalogName;

    @Value("https://cb-group.s3.eu-central-1.amazonaws.com/test/imagecatalog/sdx-upgrade-test-catalog.json")
    private String imageCatalogUrl;

    @Value("9a72c4a6-fe05-4b41-62f3-cc0a1ed35df4")
    private String imageId;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createEnvironmentForSdx(testContext);
        initializeDefaultBlueprints(testContext);

        CloudProvider cloudProvider = testContext.getCloudProvider();
        cloudProvider.setImageCatalogName(imageCatalogName);
        cloudProvider.setImageCatalogUrl(imageCatalogUrl);
        cloudProvider.setImageId(imageId);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running Cloudbreak, and an SDX cluster in available state",
            when = "a newer AWS image is available with same package versions for SDX",
            then = "image upgrade should be successful and SDX should be in RUNNING state again"
    )
    public void testSDXCanBeUpgradedSuccessfully(TestContext testContext) {
        String sdxInternal = resourcePropertyProvider().getName();
        String cluster = resourcePropertyProvider().getName();
        String clouderaManager = resourcePropertyProvider().getName();
        String imageSettings = resourcePropertyProvider().getName();
        String stack = resourcePropertyProvider().getName();
        AtomicReference<String> selectedImageID = new AtomicReference<>();

        testContext
                .given(awsCloudProvider.getImageCatalogName(), ImageCatalogTestDto.class)
                .withName(awsCloudProvider.getImageCatalogName()).withUrl(awsCloudProvider.getImageCatalogUrl())
                .when(imageCatalogTestClient.createV4(), key(awsCloudProvider.getImageCatalogName()))
                .when((tc, dto, client) -> {
                    selectedImageID.set(testContext.getCloudProvider().getPreviousPreWarmedImageID(tc, dto, client));
                    return dto;
                })
                .given(imageSettings, ImageSettingsTestDto.class)
                .given(clouderaManager, ClouderaManagerTestDto.class)
                .given(cluster, ClusterTestDto.class).withClouderaManager(clouderaManager)
                .given(stack, StackTestDto.class).withCluster(cluster).withImageSettings(imageSettings)
                .given(sdxInternal, SdxInternalTestDto.class).withStackRequest(stack, cluster)
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxInternal))
                .then((tc, testDto, client) -> {
                    return waitUtil.waitForSdxInstancesStatus(testDto, client, getSdxInstancesHealthyState());
                })
                .when((tc, testDto, client) -> {
                    return sdxTestClient.checkForUpgrade().action(tc, testDto, client);
                })
                .when((tc, testDto, client) -> {
                    return sdxTestClient.upgrade().action(tc, testDto, client);
                })
                .await(SdxClusterStatusResponse.UPGRADE_IN_PROGRESS, key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxInternal))
                .then((tc, testDto, client) -> {
                    return waitUtil.waitForSdxInstancesStatus(testDto, client, getSdxInstancesHealthyState());
                })
                .then((tc, dto, client) -> {
                    Log.log(LOGGER, format(" Image Catalog Name: %s ", dto.getResponse().getStackV4Response().getImage().getCatalogName()));
                    Log.log(LOGGER, format(" Image Catalog URL: %s ", dto.getResponse().getStackV4Response().getImage().getCatalogUrl()));
                    Log.log(LOGGER, format(" Image ID after SDX Upgrade: %s ", dto.getResponse().getStackV4Response().getImage().getId()));

                    if (dto.getResponse().getStackV4Response().getImage().getId().equals(selectedImageID.get())) {
                        throw new TestFailException(" The selected image ID is: " + dto.getResponse().getStackV4Response().getImage().getId() + " instead of: "
                                + selectedImageID.get());
                    }
                    return dto;
                })
                .validate();
    }
}

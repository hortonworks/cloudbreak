package com.sequenceiq.it.cloudbreak.testcase.e2e.aws;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static java.lang.String.format;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.aws.AwsProperties;
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

public class AwsSdxUpgradeTests extends BasicSdxTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsSdxUpgradeTests.class);

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private AwsProperties awsProperties;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createEnvironmentForSdx(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running Cloudbreak, and an SDX cluster in available state",
            when = "a newer image is available for CentOS7 on AWS in eu-west-1",
            then = "image upgrade notification should be available"
    )
    public void testSDXWithOlderImageCanBeCreatedSuccessfully(TestContext testContext) {
        String sdxInternal = resourcePropertyProvider().getName();
        String cluster = resourcePropertyProvider().getName();
        String clouderaManager = resourcePropertyProvider().getName();
        String imageSettings = resourcePropertyProvider().getName();
        String imageCatalog = resourcePropertyProvider().getName();
        String stack = resourcePropertyProvider().getName();
        AtomicReference<String> selectedImageID = new AtomicReference<>();

        testContext
                .given(imageCatalog, ImageCatalogTestDto.class)
                .when((tc, dto, client) -> {
                    selectedImageID.set(getPreviousAWSImageID(tc, dto, client));
                    return dto;
                })
                .given(imageSettings, ImageSettingsTestDto.class)
                .given(clouderaManager, ClouderaManagerTestDto.class)
                .given(cluster, ClusterTestDto.class).withClouderaManager(clouderaManager)
                .given(stack, StackTestDto.class).withCluster(cluster).withImageSettings(imageSettings)
                .given(sdxInternal, SdxInternalTestDto.class).withStackRequest(stack, cluster)
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .await(SDX_RUNNING)
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
                .validate();
    }

    private String getPreviousAWSImageID(TestContext testContext, ImageCatalogTestDto imageCatalogTestDto, CloudbreakClient cloudbreakClient)
            throws TestFailException {

        if (awsProperties.getBaseimage().getImageId() == null || awsProperties.getBaseimage().getImageId().isEmpty()) {
            try {
                List<ImageV4Response> images = cloudbreakClient
                        .getCloudbreakClient()
                        .imageCatalogV4Endpoint()
                        .getImagesByName(cloudbreakClient.getWorkspaceId(), imageCatalogTestDto.getRequest().getName(), null,
                                CloudPlatform.AWS.name()).getCdhImages();

                ImageV4Response olderImage = images.get(images.size() - 2);
                Log.log(LOGGER, format(" Selected Image Date: %s | Image ID: %s | Stack Version: %s | Stack Description: %s ", olderImage.getDate(),
                        olderImage.getUuid(), olderImage.getStackDetails().getVersion(), olderImage.getDescription()));
                awsProperties.getBaseimage().setImageId(olderImage.getUuid());

                return olderImage.getUuid();
            } catch (Exception e) {
                LOGGER.error("Cannot fetch images of {} image catalog!", imageCatalogTestDto.getRequest().getName());
                throw new TestFailException(" Cannot fetch images of " + imageCatalogTestDto.getRequest().getName() + " image catalog!");
            }
        } else {
            return awsProperties.getBaseimage().getImageId();
        }
    }
}

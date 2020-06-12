package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.IDBROKER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static java.lang.String.format;

import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.ImageSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.it.cloudbreak.util.wait.WaitUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxImagesTests extends PreconditionSdxE2ETest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SdxImagesTests.class);

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private WaitUtil waitUtil;

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak",
            when = "a basic SDX create request is sent",
            then = "SDX should be available AND deletable"
    )
    public void testSDXWithPrewarmedImageCanBeCreatedSuccessfully(TestContext testContext) {
        String sdx = resourcePropertyProvider().getName();

        testContext
                .given(sdx, SdxTestDto.class)
                .when(sdxTestClient.create(), key(sdx))
                .awaitForFlow(key(sdx))
                .await(SdxClusterStatusResponse.RUNNING)
                .then((tc, testDto, client) -> {
                    waitUtil.waitForSdxInstanceStatus(testDto.getResponse().getName(), tc, getSdxInstancesHealthyState(), true);
                    return testDto;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak",
            when = "a valid SDX create request is sent (latest Base Image)",
            then = "SDX should be available AND deletable"
    )
    public void testSDXWithBaseImageCanBeCreatedSuccessfully(TestContext testContext) {
        String sdxInternal = resourcePropertyProvider().getName();
        String cluster = resourcePropertyProvider().getName();
        String clouderaManager = resourcePropertyProvider().getName();
        String imageSettings = resourcePropertyProvider().getName();
        String imageCatalog = resourcePropertyProvider().getName();
        String stack = resourcePropertyProvider().getName();
        String masterInstanceGroup = "master";
        String idbrokerInstanceGroup = "idbroker";
        AtomicReference<String> selectedImageID = new AtomicReference<>();

        testContext
                .given(imageCatalog, ImageCatalogTestDto.class)
                .when((tc, dto, client) -> {
                    selectedImageID.set(testContext.getCloudProvider().getLatestBaseImageID(tc, dto, client));
                    return dto;
                })
                .given(imageSettings, ImageSettingsTestDto.class)
                .given(clouderaManager, ClouderaManagerTestDto.class)
                .given(cluster, ClusterTestDto.class).withBlueprintName(getDefaultSDXBlueprintName()).withValidateBlueprint(Boolean.FALSE)
                .withClouderaManager(clouderaManager)
                .given(masterInstanceGroup, InstanceGroupTestDto.class).withHostGroup(MASTER).withNodeCount(1)
                .given(idbrokerInstanceGroup, InstanceGroupTestDto.class).withHostGroup(IDBROKER).withNodeCount(1)
                .given(stack, StackTestDto.class).withCluster(cluster).withImageSettings(imageSettings)
                .withInstanceGroups(masterInstanceGroup, idbrokerInstanceGroup)
                .given(sdxInternal, SdxInternalTestDto.class)
                .withStackRequest(key(cluster), key(stack))
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .awaitForFlow(key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING)
                .then((tc, testDto, client) -> {
                    waitUtil.waitForSdxInstanceStatus(testDto.getResponse().getName(), tc, getSdxInstancesHealthyState(), true);
                    return testDto;
                })
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
}

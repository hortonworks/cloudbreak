package com.sequenceiq.it.cloudbreak.testcase.e2e.imagevalidation;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.ImageSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import org.testng.annotations.Test;

import javax.inject.Inject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.IDBROKER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

public class BaseImageValidatorE2ETest extends AbstractImageValidatorE2ETest {

    private static final Map<String, InstanceStatus> HEALTY_STATUSES = new HashMap<>() {{
        put(HostGroupType.MASTER.getName(), InstanceStatus.SERVICES_HEALTHY);
        put(HostGroupType.IDBROKER.getName(), InstanceStatus.SERVICES_HEALTHY);
    }};

    private AtomicReference<String> selectedImageID;

    @Inject
    private SdxTestClient sdxTestClient;

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak",
            when = "a valid SDX create request is sent (latest Base Image)",
            then = "SDX should be available AND deletable")
    public void testSDXWithBaseImage(TestContext testContext) {
        String sdxInternal = resourcePropertyProvider().getName();
        String cluster = resourcePropertyProvider().getName();
        String clouderaManager = resourcePropertyProvider().getName();
        String imageSettings = resourcePropertyProvider().getName();
        String imageCatalog = resourcePropertyProvider().getName();
        String stack = resourcePropertyProvider().getName();
        String masterInstanceGroup = "master";
        String idbrokerInstanceGroup = "idbroker";
        selectedImageID = new AtomicReference<>();

        testContext
                .given(imageCatalog, ImageCatalogTestDto.class)
                .when((tc, dto, client) -> {
                    selectedImageID.set(testContext.getCloudProvider().getLatestBaseImageID(tc, dto, client));
                    return dto;
                })
                .given(imageSettings, ImageSettingsTestDto.class)
                .given(clouderaManager, ClouderaManagerTestDto.class)
                .given(cluster, ClusterTestDto.class)
                .withBlueprintName(commonClusterManagerProperties().getInternalSdxBlueprintName())
                .withValidateBlueprint(Boolean.FALSE)
                .withClouderaManager(clouderaManager)
                .given(masterInstanceGroup, InstanceGroupTestDto.class).withHostGroup(MASTER).withNodeCount(1)
                .given(idbrokerInstanceGroup, InstanceGroupTestDto.class).withHostGroup(IDBROKER).withNodeCount(1)
                .given(stack, StackTestDto.class).withCluster(cluster).withImageSettings(imageSettings)
                .withInstanceGroups(masterInstanceGroup, idbrokerInstanceGroup)
                .given(sdxInternal, SdxInternalTestDto.class)
                .withCloudStorage(getCloudStorageRequest(testContext))
                .withStackRequest(key(cluster), key(stack))
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .awaitForFlow(key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForInstance(HEALTY_STATUSES)
                .validate();
    }

    @Override
    protected String getImageId(TestContext testContext) {
        return selectedImageID.get();
    }

    @Override
    protected boolean isPrewarmedImageTest() {
        return false;
    }
}

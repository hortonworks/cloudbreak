package com.sequenceiq.it.cloudbreak.testcase.e2e.imagevalidation;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.IDBROKER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.common.model.OsType;
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
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.it.util.imagevalidation.ImageValidatorE2ETest;
import com.sequenceiq.it.util.imagevalidation.ImageValidatorE2ETestUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class BaseImageValidatorE2ETest extends AbstractE2ETest implements ImageValidatorE2ETest {

    private static final Map<String, InstanceStatus> HEALTY_STATUSES = new HashMap<>() {{
        put(HostGroupType.MASTER.getName(), InstanceStatus.SERVICES_HEALTHY);
        put(HostGroupType.IDBROKER.getName(), InstanceStatus.SERVICES_HEALTHY);
    }};

    private static final String LATEST_CENTOS7_RUNTIME_VERSION = "7.2.17";

    private String sdxInternalKey;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private ImageValidatorE2ETestUtil imageValidatorE2ETestUtil;

    @Override
    protected void setupTest(TestContext testContext) {
        imageValidatorE2ETestUtil.setupTest(testContext);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
        createEnvironmentWithFreeIpa(testContext);
        sdxInternalKey = resourcePropertyProvider().getName();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak",
            when = "a valid SDX create request is sent (latest Base Image)",
            then = "SDX should be available AND deletable")
    public void testSDXWithBaseImage(TestContext testContext) {
        String cluster = resourcePropertyProvider().getName();
        String clouderaManager = resourcePropertyProvider().getName();
        String imageSettings = resourcePropertyProvider().getName();
        String imageCatalog = resourcePropertyProvider().getName();
        String stack = resourcePropertyProvider().getName();
        String masterInstanceGroup = "master";
        String idbrokerInstanceGroup = "idbroker";

        String runtimeVersion = OsType.CENTOS7.getOs().equals(imageValidatorE2ETestUtil.getImage(testContext).getOs())
                ? LATEST_CENTOS7_RUNTIME_VERSION
                : commonClusterManagerProperties().getRuntimeVersion();

        testContext
                .given(imageCatalog, ImageCatalogTestDto.class)
                .given(imageSettings, ImageSettingsTestDto.class)
                    .withImageCatalog(commonCloudProperties().getImageValidation().getSourceCatalogName())
                    .withImageId(commonCloudProperties().getImageValidation().getImageUuid())
                .given(clouderaManager, ClouderaManagerTestDto.class)
                .given(cluster, ClusterTestDto.class)
                    .withBlueprintName(commonClusterManagerProperties().getInternalSdxBlueprintNameWithRuntimeVersion(runtimeVersion))
                    .withValidateBlueprint(Boolean.FALSE)
                    .withClouderaManager(clouderaManager)
                .given(masterInstanceGroup, InstanceGroupTestDto.class).withHostGroup(MASTER).withNodeCount(1)
                .given(idbrokerInstanceGroup, InstanceGroupTestDto.class).withHostGroup(IDBROKER).withNodeCount(1)
                .given(stack, StackTestDto.class).withCluster(cluster).withImageSettings(imageSettings)
                    .withEmptyNetwork()
                    .withInstanceGroups(masterInstanceGroup, idbrokerInstanceGroup)
                .given(sdxInternalKey, SdxInternalTestDto.class)
                    .withRuntimeVersion(runtimeVersion)
                    .withCloudStorage(getCloudStorageRequest(testContext))
                    .withStackRequest(key(cluster), key(stack))
                    .withoutDatabase()
                .when(sdxTestClient.createInternal(), key(sdxInternalKey))
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .validate();
    }

    @Override
    public String getCbImageId(TestContext testContext) {
        SdxInternalTestDto sdxInternalTestDto = testContext.get(sdxInternalKey);
        return sdxInternalTestDto.getResponse().getStackV4Response().getImage().getId();
    }
}

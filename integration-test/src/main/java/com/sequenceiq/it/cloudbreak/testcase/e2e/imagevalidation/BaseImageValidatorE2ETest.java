package com.sequenceiq.it.cloudbreak.testcase.e2e.imagevalidation;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.IDBROKER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.StackImageV4Response;
import com.sequenceiq.cloudbreak.util.VersionComparator;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.it.cloudbreak.assertion.image.ImageAssertions;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.ImageSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.clouderamanager.DistroXClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.it.util.imagevalidation.ImageValidatorE2ETest;
import com.sequenceiq.it.util.imagevalidation.ImageValidatorE2ETestUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class BaseImageValidatorE2ETest extends AbstractE2ETest implements ImageValidatorE2ETest {

    private static final Map<String, InstanceStatus> HEALTY_STATUSES = Map.of(
        MASTER.getName(), InstanceStatus.SERVICES_HEALTHY,
        IDBROKER.getName(), InstanceStatus.SERVICES_HEALTHY);

    private static final String LATEST_CENTOS7_RUNTIME_VERSION = "7.2.17";

    private static final String ARM64_MIN_RUNTIME_VERSION = "7.3.1";

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private ImageValidatorE2ETestUtil imageValidatorE2ETestUtil;

    @Inject
    private ImageAssertions imageAssertions;

    @Override
    protected void setupTest(TestContext testContext) {
        imageValidatorE2ETestUtil.setupTest(testContext);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
        createEnvironmentWithFreeIpa(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak",
            then = "based on the base image's architecture the relevant test case is selected")
    public void testProvisioningWithBaseImage(TestContext testContext) {
        ImageV4Response imageUnderValidation = imageValidatorE2ETestUtil.getImageUnderValidation(testContext).orElseThrow();
        Architecture architecture = Architecture.fromStringWithFallback(imageUnderValidation.getArchitecture());

        switch (architecture) {
            case X86_64 -> testSDXWithX86BaseImage(testContext, imageUnderValidation);
            case ARM64 -> testDistroXWithArm64BaseImage(testContext, imageUnderValidation);
            default -> throw new TestFailException("Base iamge validation is not implemented for architecture " + architecture.getName());
        }
    }

    @Description(
            given = "there is a running environment with FreeIpa",
            when = "provisioning an SDX with the x86_64 base image under validation",
            then = "SDX should be available with healthy instances")
    private void testSDXWithX86BaseImage(TestContext testContext, ImageV4Response imageUnderValidation) {
        String cluster = resourcePropertyProvider().getName();
        String clouderaManager = resourcePropertyProvider().getName();
        String imageSettings = resourcePropertyProvider().getName();
        String imageCatalog = resourcePropertyProvider().getName();
        String stack = resourcePropertyProvider().getName();
        String masterInstanceGroup = "master";
        String idbrokerInstanceGroup = "idbroker";

        String runtimeVersion = OsType.CENTOS7.getOs().equals(imageUnderValidation.getOs())
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
                .given(SdxInternalTestDto.class)
                    .withRuntimeVersion(runtimeVersion)
                    .withCloudStorage(getCloudStorageRequest(testContext))
                    .withStackRequest(key(cluster), key(stack))
                .when(sdxTestClient.createInternal())
                .then(imageAssertions.validateSdxInternalImageSetupTime())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .validate();
    }

    @Description(
            given = "there is a running environment with FreeIpa and SDX on x86_64 architecture",
            when = "provisioning an DistroX with the arm64 base image under validation",
            then = "DistroX should be available with healthy instances")
    private void testDistroXWithArm64BaseImage(TestContext testContext, ImageV4Response imageUnderValidation) {
        String runtimeVersion = new VersionComparator().compare(() -> commonClusterManagerProperties().getRuntimeVersion(), () -> ARM64_MIN_RUNTIME_VERSION) < 0
                ? ARM64_MIN_RUNTIME_VERSION
                : commonClusterManagerProperties().getRuntimeVersion();

        testContext
                .given(SdxInternalTestDto.class)
                    .withDefaultImage()
                    .withTemplate(commonClusterManagerProperties().getInternalSdxBlueprintNameWithRuntimeVersion(runtimeVersion))
                .when(sdxTestClient.createInternal())
                .await(SdxClusterStatusResponse.RUNNING)
                .when(sdxTestClient.describeInternal())
                .validate();

        String cluster = resourcePropertyProvider().getName();
        String clouderaManager = resourcePropertyProvider().getName();

        testContext
                .given(clouderaManager, DistroXClouderaManagerTestDto.class)
                .given(cluster, DistroXClusterTestDto.class)
                    .withBlueprintName(commonClusterManagerProperties().getDataEngDistroXBlueprintName(runtimeVersion))
                    .withValidateBlueprint(Boolean.FALSE)
                    .withClouderaManager(clouderaManager)
                .given(DistroXTestDto.class)
                    .withArchitecture(Architecture.ARM64)
                    .withCluster(cluster)
                .when(distroXTestClient.create())
                .await(STACK_CREATE_IN_PROGRESS, RunningParameter.emptyRunningParameter().withoutWaitForFlow())
                .then(imageAssertions.validateDistroXImageSetupTime())
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .validate();
    }

    @Override
    public String getCbImageId(TestContext testContext) {
        ImageV4Response imageUnderValidation = imageValidatorE2ETestUtil.getImageUnderValidation(testContext).orElseThrow();
        return Architecture.fromStringWithFallback(imageUnderValidation.getArchitecture()) == Architecture.ARM64
                ? getDistroXImage(testContext)
                : ImageValidatorE2ETest.super.getCbImageId(testContext);
    }

    private String getDistroXImage(TestContext testContext) {
        return Optional.ofNullable(testContext.get(DistroXTestDto.class))
                .map(AbstractTestDto::getResponse)
                .map(StackV4Response::getImage)
                .map(StackImageV4Response::getId)
                .orElse(null);
    }
}

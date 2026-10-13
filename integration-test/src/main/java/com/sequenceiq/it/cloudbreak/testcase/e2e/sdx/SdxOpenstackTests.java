package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.product.ClouderaManagerProductV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerRepositoryV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ClouderaManagerStackDescriptorV4Response;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.util.VersionComparator;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.client.UtilTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.openstack.OpenstackProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerRepositoryTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.util.StackMatrixTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.CloudbreakImageUtil;
import com.sequenceiq.it.cloudbreak.util.FreeIpaImageUtil;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxOpenstackTests extends AbstractE2ETest {

    private static final String CDH = "CDH";

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private UtilTestClient utilTestClient;

    @Inject
    private ImageCatalogTestClient imageCatalogTestClient;

    @Inject
    private OpenstackProperties openstackProperties;

    @Inject
    private CloudbreakImageUtil cloudbreakImageUtil;

    @Inject
    private FreeIpaImageUtil freeIpaImageUtil;

    private String runtimeVersion;

    private String cdhVersion;

    private String cdhParcel;

    private String cmVersion;

    private String cmBaseUrl;

    private String cmGpgKeyUrl;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createImageCatalogs(testContext);
    }

    private void createImageCatalogs(TestContext testContext) {
        OpenstackProperties.Image sdxImage = openstackProperties.getSdxImage();
        if (hasCustomImageCatalog(sdxImage)) {
            testContext.given("cbImageCatalog", ImageCatalogTestDto.class)
                    .withName(sdxImage.getCatalog())
                    .withUrl(sdxImage.getUrl())
                    .withoutCleanup()
                    .when(imageCatalogTestClient.createIfNotExistV4())
                    .validate();
        }

        OpenstackProperties.Image freeIpaImage = openstackProperties.getFreeipaImage();
        if (hasCustomImageCatalog(freeIpaImage)) {
            testContext.given("freeipaImageCatalog", ImageCatalogTestDto.class)
                    .withName(freeIpaImage.getCatalog())
                    .withUrl(freeIpaImage.getUrl())
                    .withoutCleanup()
                    .when(imageCatalogTestClient.createIfNotExistV4())
                    .validate();
        }
    }

    private boolean hasCustomImageCatalog(OpenstackProperties.Image image) {
        return StringUtils.isNoneBlank(image.getCatalog(), image.getUrl());
    }

    private void fetchCdhDetails(TestContext testContext) {
        if (StringUtils.isAnyBlank(runtimeVersion, cdhVersion, cdhParcel, cmVersion, cmBaseUrl, cmGpgKeyUrl)) {
            OsType osType = OsType.getByOsTypeString(openstackProperties.getOsType());
            testContext
                    .given(StackMatrixTestDto.class)
                        .withOs(osType.getOs())
                        .withArchitecture(Architecture.X86_64)
                    .when(utilTestClient.stackMatrixV4())
                    .then((tc, dto, client) -> {
                        runtimeVersion = latestCdhRuntimeVersion(dto.getResponse().getCdh());
                        ClouderaManagerStackDescriptorV4Response descriptor = dto.getResponse().getCdh().get(runtimeVersion);
                        cdhVersion = descriptor.getVersion();
                        cdhParcel = descriptor.getRepository().getStack().get(osType.getOsType());
                        ClouderaManagerRepositoryV4Response cmRepo = descriptor.getClouderaManager().getRepository().get(osType.getOsType());
                        cmVersion = descriptor.getClouderaManager().getVersion();
                        cmBaseUrl = cmRepo.getBaseUrl();
                        cmGpgKeyUrl = cmRepo.getGpgKeyUrl();
                        return dto;
                    })
                    .validate();
        }
    }

    private String latestCdhRuntimeVersion(Map<String, ClouderaManagerStackDescriptorV4Response> cdhVersions) {
        VersionComparator versionComparator = new VersionComparator();
        return cdhVersions.keySet().stream()
                .max((v1, v2) -> versionComparator.compare(() -> v1, () -> v2))
                .orElseThrow(() -> new TestFailException("No CDH versions found in stack matrix"));
    }

    private String configuredOrNewestFreeIpaImageId() {
        OpenstackProperties.Image freeIpaImage = openstackProperties.getFreeipaImage();
        if (shouldUseNewestImage(freeIpaImage)) {
            return freeIpaImageUtil.getNewestImageId(freeIpaImage.getUrl(), CloudPlatform.OPENSTACK.name(),
                    openstackProperties.getRegion(), OsType.getByOsTypeString(openstackProperties.getOsType()).getOs(), Architecture.X86_64);
        }
        return freeIpaImage.getId();
    }

    private String configuredOrNewestSdxBaseImageId() {
        OpenstackProperties.Image sdxImage = openstackProperties.getSdxImage();
        if (shouldUseNewestImage(sdxImage)) {
            return cloudbreakImageUtil.getNewestBaseImageId(sdxImage.getUrl(), CloudPlatform.OPENSTACK.name(),
                    openstackProperties.getRegion(), openstackProperties.getOsType(), Architecture.X86_64);
        }
        return sdxImage.getId();
    }

    private boolean shouldUseNewestImage(OpenstackProperties.Image image) {
        return StringUtils.isBlank(image.getId()) && hasCustomImageCatalog(image);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running openstack environment",
            when = "a Data Lake cluster is created, stopped, started, and master host group is repaired",
            then = "the Data Lake cluster should be available and healthy"
    )
    public void testSdxCreateStopStartRepair(TestContext testContext) {
        fetchCdhDetails(testContext);
        String freeIpaImageId = configuredOrNewestFreeIpaImageId();
        String sdxImageId = configuredOrNewestSdxBaseImageId();

        ClouderaManagerProductV4Request cdh = new ClouderaManagerProductV4Request();
        cdh.setName(CDH);
        cdh.setVersion(cdhVersion);
        cdh.setParcel(cdhParcel);

        String sdxBlueprintName = commonClusterManagerProperties()
                .getInternalSdxBlueprintNameWithRuntimeVersion(runtimeVersion);

        testContext
                .given(EnvironmentNetworkTestDto.class)
                    .valid()
                .given(EnvironmentTestDto.class)
                    .valid()
                    .withCreateFreeIpa(true)
                    .withFreeIpaImage(openstackProperties.getFreeipaImage().getCatalog(), freeIpaImageId)
                    .withFreeIpaNodes(1)
                    .withNetwork(EnvironmentNetworkTestDto.class.getSimpleName())
                .when(getEnvironmentTestClient().create())
                .awaitForFlow()
                .await(EnvironmentStatus.AVAILABLE)
                .given(ClouderaManagerRepositoryTestDto.class)
                    .withVersion(cmVersion)
                    .withBaseUrl(cmBaseUrl)
                    .withGpgKeyUrl(cmGpgKeyUrl)
                .given(ClouderaManagerTestDto.class)
                    .withProducts(List.of(cdh))
                    .withClouderaManagerRepository(ClouderaManagerRepositoryTestDto.class.getSimpleName())
                .given(ClusterTestDto.class)
                    .withBlueprintName(sdxBlueprintName)
                    .withValidateBlueprint(false)
                    .withClouderaManager(ClouderaManagerTestDto.class.getSimpleName())
                .given(SdxInternalTestDto.class)
                    .withDefaultSDXSettings()
                    .withClusterShape(SdxClusterShape.LIGHT_DUTY)
                    .withImageCatalogNameAndImageId(openstackProperties.getSdxImage().getCatalog(), sdxImageId)
                .when(sdxTestClient.createInternal())
                .awaitForFlow()
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .when(sdxTestClient.stopInternal())
                .awaitForFlow()
                .await(SdxClusterStatusResponse.STOPPED)
                .when(sdxTestClient.startInternal())
                .awaitForFlow()
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .when(sdxTestClient.repairInternalByNodeIds(MASTER.getName()))
                .awaitForFlow()
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .validate();
    }
}

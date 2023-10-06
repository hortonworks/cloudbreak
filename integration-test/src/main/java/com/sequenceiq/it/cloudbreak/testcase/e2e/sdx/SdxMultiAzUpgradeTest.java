package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.IDBROKER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.springframework.util.CollectionUtils;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.it.cloudbreak.util.VolumeUtils;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxMultiAzUpgradeTest extends PreconditionSdxE2ETest {

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Inject
    private SdxUtil sdxUtil;

    @Inject
    private ImageCatalogTestClient imageCatalogTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
        initializeAzureMarketplaceTermsPolicy(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak, and an SDX multiAz cluster in available state",
            when = "a valid upgrade stack request is sent ",
            then = "the MultiAz stack should be upgraded and the cluster should be up and running"
    )
    public void testSDXMultiAzUpgrade(TestContext testContext) {
        List<ImageV4Response> cdhImages = new ArrayList<>();
        testContext
                .given(ImageCatalogTestDto.class)
                .when(imageCatalogTestClient.getV4(true))
                // We need this logic until 7.2.18 is not in production catalog
                .then((testContext1, entity, cloudbreakClient) -> {
                    List<ImageV4Response> sortedImages = entity.getResponse().getImages().getCdhImages().stream()
                            .filter(im -> im.getVersion().equals(commonClusterManagerProperties.getMinimumRuntimeVersionForEdl()))
                            .filter(im -> im.getImageSetsByProvider().containsKey(testContext.getCloudPlatform().name().toLowerCase(Locale.ROOT)))
                            .sorted(Comparator.comparing(ImageV4Response::getCreated))
                            .collect(Collectors.toList());
                    cdhImages.addAll(sortedImages);
                    return entity;
                })
                .validate();
        if (cdhImages.size() > 1) {
            createEnvironmentWithFreeIpa(testContext);
            String sdx = resourcePropertyProvider().getName();
            List<String> actualVolumeIds = new ArrayList<>();
            List<String> expectedVolumeIds = new ArrayList<>();
            testContext
                    .given(sdx, SdxTestDto.class)
                    .withCloudStorage()
                    .withClusterShape(SdxClusterShape.ENTERPRISE)
                    // Because of an emergency fix a newer 7.2.17 was burnet then the latest one.
                    // We need to remove this once we have a new image in production
                    // The failed testcase used 5ff64ba3-4e76-4b85-8a89-86a5df9c8445 (emergency release) -> 17475332-e6fb-4bbf-9ad3-598c821ed963
                    .withImageId("93b37158-275f-45a9-8dd9-b9d089c7bb4e")
                    .withRuntimeVersion(null)
                    .withEnableMultiAz(true)
                    .when(sdxTestClient.createWithImageId(), key(sdx))
                    .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                    .awaitForHealthyInstances()
                    .then((tc, testDto, client) -> {
                        List<String> instances = sdxUtil.getInstanceIds(testDto, client, MASTER.getName());
                        instances.addAll(sdxUtil.getInstanceIds(testDto, client, IDBROKER.getName()));
                        expectedVolumeIds.addAll(getCloudFunctionality(tc).listInstancesVolumeIds(testDto.getName(), instances));
                        return testDto;
                    })
                    .withUpgradeRequestByImageId(cdhImages.get(cdhImages.size() - 1).getUuid())
                    .when(sdxTestClient.upgrade(), key(sdx))
                    .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                    .awaitForHealthyInstances()
                    .then((tc, testDto, client) -> {
                        List<String> instanceIds = sdxUtil.getInstanceIds(testDto, client, MASTER.getName());
                        instanceIds.addAll(sdxUtil.getInstanceIds(testDto, client, IDBROKER.getName()));
                        actualVolumeIds.addAll(getCloudFunctionality(tc).listInstancesVolumeIds(testDto.getName(), instanceIds));
                        return testDto;
                    })
                    .then((tc, testDto, client) -> VolumeUtils.compareVolumeIdsAfterRepair(testDto, actualVolumeIds, expectedVolumeIds))
                    .when(sdxTestClient.describe(), key(sdx))
                    .then((tc, testDto, client) -> {
                        validateMultiAz(testDto, tc);
                        return testDto;
                    })
                    .validate();
        }
    }

    private void validateMultiAz(SdxTestDto sdxTestDto, TestContext tc) {
        SdxClusterDetailResponse sdxClusterDetailResponse = sdxTestDto.getResponse();
        if (!sdxClusterDetailResponse.isEnableMultiAz()) {
            throw new TestFailException(String.format("MultiAz is not enabled for %s", sdxClusterDetailResponse.getName()));
        }
        List<InstanceMetaDataV4Response> instanceMetaDataV4Responses = sdxClusterDetailResponse.getStackV4Response().getInstanceGroups().stream()
                .map(InstanceGroupV4Response::getMetadata)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream).toList();
        List<String> instanceIds = instanceMetaDataV4Responses.stream()
                .map(InstanceMetaDataV4Response::getInstanceId)
                .collect(Collectors.toList());
        List<String> availabilityZones = instanceMetaDataV4Responses.stream()
                .map(InstanceMetaDataV4Response::getAvailabilityZone).toList();
        List<String> rackIds = instanceMetaDataV4Responses.stream()
                .map(res -> res.getRackId().substring(1)).toList();
        Assertions.assertThat(availabilityZones).withFailMessage("Rack ID and Availability zones are different")
                .containsAll(rackIds);
        String sdxName = sdxClusterDetailResponse.getStackV4Response().getName();
        Map<String, Set<String>> availabilityZoneForVms = getCloudFunctionality(tc).listAvailabilityZonesForVms(sdxName, instanceIds);
        List<String> instancesWithNoAz = instanceIds.stream().filter(instance -> CollectionUtils.isEmpty(availabilityZoneForVms.get(instance)))
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(instancesWithNoAz)) {
            throw new TestFailException(String.format("Availability Zones is missing for instances %s in %s",
                    String.join(",", instancesWithNoAz), sdxName));
        }
    }

    protected CloudFunctionality getCloudFunctionality(TestContext testContext) {
        return testContext.getCloudProvider().getCloudFunctionality();
    }
}

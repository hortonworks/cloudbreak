package com.sequenceiq.datalake.service.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageComponentVersions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeShowAvailableImages;

@ExtendWith(MockitoExtension.class)
class SdxUpgradeFilterTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String ACCOUNT_ID = Crn.fromString(USER_CRN).getAccountId();

    private static final String IMAGE_ID = "image-id-first";

    private static final String IMAGE_ID_LAST = "image-id-last";

    private static final String MATCHING_TARGET_RUNTIME = "7.0.2";

    private static final String V_7_0_3 = "7.0.3";

    private static final String V_7_0_2 = "7.0.2";

    @InjectMocks
    private SdxUpgradeFilter underTest;

    private SdxUpgradeRequest sdxUpgradeRequest;

    private SdxCluster sdxCluster;

    @BeforeEach
    public void setUp() {
        sdxUpgradeRequest = getFullSdxUpgradeRequest();
        sdxCluster = getValidSdxCluster();
    }

    @Test
    public void testDryRunShouldReturnOneUpgradeCandidate() {
        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        imageInfo.setCreated(1L);
        imageInfo.setComponentVersions(crateExpectedPackageVersions());
        ImageInfoV4Response lastImageInfo = new ImageInfoV4Response();
        lastImageInfo.setImageId(IMAGE_ID_LAST);
        lastImageInfo.setCreated(2L);
        lastImageInfo.setComponentVersions(crateExpectedPackageVersions());
        UpgradeV4Response upgradeV4Response = new UpgradeV4Response();
        upgradeV4Response.setUpgradeCandidates(List.of(imageInfo, lastImageInfo));
        sdxUpgradeRequest.setDryRun(true);

        UpgradeV4Response actualUpgradeResponse = underTest.filterSdxUpgradeResponse(ACCOUNT_ID, sdxCluster.getClusterName(),
                sdxUpgradeRequest, upgradeV4Response);

        assertEquals(1, actualUpgradeResponse.getUpgradeCandidates().size());
        assertEquals(IMAGE_ID_LAST, actualUpgradeResponse.getUpgradeCandidates().get(0).getImageId());
    }

    @Test
    public void testShowLatestOnlyShouldReturnLatestUpgradeCandidatesPerRuntime() {
        ImageComponentVersions imageComponentVersionsFor702 = new ImageComponentVersions();
        imageComponentVersionsFor702.setCm(V_7_0_2);
        imageComponentVersionsFor702.setCdp(V_7_0_2);

        ImageComponentVersions imageComponentVersionsFor703 = new ImageComponentVersions();
        imageComponentVersionsFor703.setCm(V_7_0_3);
        imageComponentVersionsFor703.setCdp(V_7_0_3);

        ImageInfoV4Response imageInfo1 = new ImageInfoV4Response();
        imageInfo1.setImageId(IMAGE_ID + 1);
        imageInfo1.setCreated(1L);
        imageInfo1.setComponentVersions(imageComponentVersionsFor702);

        ImageInfoV4Response imageInfo2 = new ImageInfoV4Response();
        imageInfo2.setImageId(IMAGE_ID + 2);
        imageInfo2.setCreated(2L);
        imageInfo2.setComponentVersions(imageComponentVersionsFor702);

        ImageInfoV4Response imageInfo3 = new ImageInfoV4Response();
        imageInfo3.setImageId(IMAGE_ID + 3);
        imageInfo3.setCreated(3L);
        imageInfo3.setComponentVersions(imageComponentVersionsFor703);

        UpgradeV4Response upgradeV4Response = new UpgradeV4Response();
        upgradeV4Response.setUpgradeCandidates(List.of(imageInfo1, imageInfo2, imageInfo3));
        sdxUpgradeRequest.setShowAvailableImages(SdxUpgradeShowAvailableImages.LATEST_ONLY);

        UpgradeV4Response actualResponse = underTest.filterSdxUpgradeResponse(ACCOUNT_ID, sdxCluster.getClusterName(), sdxUpgradeRequest, upgradeV4Response);

        assertEquals(2, actualResponse.getUpgradeCandidates().size());
        assertFalse(actualResponse.getUpgradeCandidates().stream().anyMatch(imageInfoV4Response -> imageInfoV4Response.getImageId().equals(IMAGE_ID + 1)));
        assertTrue(actualResponse.getUpgradeCandidates().stream().anyMatch(imageInfoV4Response -> imageInfoV4Response.getImageId().equals(IMAGE_ID + 2)));
        assertTrue(actualResponse.getUpgradeCandidates().stream().anyMatch(imageInfoV4Response -> imageInfoV4Response.getImageId().equals(IMAGE_ID + 3)));
    }

    @Test
    public void testShowAvailableImagesShouldReturnAllUpgradeCandidates() {

        ImageComponentVersions imageComponentVersionsFor702 = new ImageComponentVersions();
        imageComponentVersionsFor702.setCm(V_7_0_2);
        imageComponentVersionsFor702.setCdp(V_7_0_2);

        ImageComponentVersions imageComponentVersionsFor703 = new ImageComponentVersions();
        imageComponentVersionsFor703.setCm(V_7_0_3);
        imageComponentVersionsFor703.setCdp(V_7_0_3);

        ImageInfoV4Response imageInfo1 = new ImageInfoV4Response();
        imageInfo1.setImageId(IMAGE_ID + 1);
        imageInfo1.setCreated(1L);
        imageInfo1.setComponentVersions(imageComponentVersionsFor702);

        ImageInfoV4Response imageInfo2 = new ImageInfoV4Response();
        imageInfo2.setImageId(IMAGE_ID + 2);
        imageInfo2.setCreated(2L);
        imageInfo2.setComponentVersions(imageComponentVersionsFor702);

        ImageInfoV4Response imageInfo3 = new ImageInfoV4Response();
        imageInfo3.setImageId(IMAGE_ID + 3);
        imageInfo3.setCreated(3L);
        imageInfo3.setComponentVersions(imageComponentVersionsFor703);

        UpgradeV4Response upgradeV4Response = new UpgradeV4Response();
        upgradeV4Response.setUpgradeCandidates(List.of(imageInfo1, imageInfo2, imageInfo3));
        sdxUpgradeRequest.setShowAvailableImages(SdxUpgradeShowAvailableImages.SHOW);

        underTest.filterSdxUpgradeResponse(ACCOUNT_ID, sdxCluster.getClusterName(), sdxUpgradeRequest, upgradeV4Response);

        assertEquals(3, upgradeV4Response.getUpgradeCandidates().size());
        assertTrue(upgradeV4Response.getUpgradeCandidates().stream().anyMatch(imageInfoV4Response -> imageInfoV4Response.getImageId().equals(IMAGE_ID + 1)));
        assertTrue(upgradeV4Response.getUpgradeCandidates().stream().anyMatch(imageInfoV4Response -> imageInfoV4Response.getImageId().equals(IMAGE_ID + 2)));
        assertTrue(upgradeV4Response.getUpgradeCandidates().stream().anyMatch(imageInfoV4Response -> imageInfoV4Response.getImageId().equals(IMAGE_ID + 3)));
    }

    private SdxUpgradeRequest getFullSdxUpgradeRequest() {
        SdxUpgradeRequest sdxUpgradeRequest = new SdxUpgradeRequest();
        sdxUpgradeRequest.setImageId(IMAGE_ID);
        sdxUpgradeRequest.setRuntime(MATCHING_TARGET_RUNTIME);
        return sdxUpgradeRequest;
    }

    private ImageComponentVersions crateExpectedPackageVersions() {
        ImageComponentVersions imageComponentVersions = new ImageComponentVersions();
        imageComponentVersions.setCm(V_7_0_3);
        imageComponentVersions.setCdp(V_7_0_2);
        return imageComponentVersions;
    }

    private SdxCluster getValidSdxCluster() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("test-sdx-cluster");
        sdxCluster.setClusterShape(SdxClusterShape.MEDIUM_DUTY_HA);
        sdxCluster.setEnvName("test-env");
        sdxCluster.setId(1L);
        sdxCluster.setAccountId("accountid");
        return sdxCluster;
    }

}
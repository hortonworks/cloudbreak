package com.sequenceiq.datalake.service.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageComponentVersions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.util.TestConstants;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeShowAvailableImages;

@ExtendWith(MockitoExtension.class)
class SdxUpgradeFilterTest {

    private static final String IMAGE_ID = "image-id-first";

    private static final String IMAGE_ID_LAST = "image-id-last";

    private static final String MATCHING_TARGET_RUNTIME = "7.0.2";

    private static final String V_7_0_3 = "7.0.3";

    private static final String V_7_0_2 = "7.0.2";

    @InjectMocks
    private SdxUpgradeFilter underTest;

    private SdxUpgradeRequest sdxUpgradeRequest;

    @Mock
    private EntitlementService entitlementService;

    @BeforeEach
    public void setUp() {
        sdxUpgradeRequest = getFullSdxUpgradeRequest();
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

        UpgradeV4Response actualUpgradeResponse = underTest.filterSdxUpgradeResponse(sdxUpgradeRequest, upgradeV4Response,
            SdxClusterShape.ENTERPRISE);

        assertEquals(1, actualUpgradeResponse.getUpgradeCandidates().size());
        assertEquals(IMAGE_ID_LAST, actualUpgradeResponse.getUpgradeCandidates().get(0).getImageId());
    }

    @Test
    void testListingUpgradeCandidatesOnLightDuty() {
        ImageInfoV4Response imageInfoV4Response1 = new ImageInfoV4Response();
        imageInfoV4Response1.setComponentVersions(new ImageComponentVersions("",  "",  "7.2.17", "",  "", "", List.of()));
        ImageInfoV4Response imageInfoV4Response2 = new ImageInfoV4Response();
        imageInfoV4Response1.setComponentVersions(new ImageComponentVersions("",  "",  "7.2.18", "",  "", "", List.of()));
        ImageInfoV4Response imageInfoV4Response3 = new ImageInfoV4Response();
        imageInfoV4Response1.setComponentVersions(new ImageComponentVersions("",  "",  "7.3.0", "",  "", "", List.of()));
        UpgradeV4Response upgradeV4Response = new UpgradeV4Response();
        upgradeV4Response.setUpgradeCandidates(List.of(imageInfoV4Response1, imageInfoV4Response2, imageInfoV4Response3));
        UpgradeV4Response response = underTest.filterSdxUpgradeResponse(sdxUpgradeRequest, upgradeV4Response, SdxClusterShape.LIGHT_DUTY);
        assertEquals(3, response.getUpgradeCandidates().size());
    }

    @Test
    void testListingUpgradeCandidatesOnMediumDuty() {
        ImageInfoV4Response imageInfoV4Response1 = new ImageInfoV4Response();
        imageInfoV4Response1.setComponentVersions(new ImageComponentVersions("",  "",  "7.2.17", "",  "", "", List.of()));
        ImageInfoV4Response imageInfoV4Response2 = new ImageInfoV4Response();
        imageInfoV4Response2.setComponentVersions(new ImageComponentVersions("",  "",  "7.2.18", "",  "", "", List.of()));
        ImageInfoV4Response imageInfoV4Response3 = new ImageInfoV4Response();
        imageInfoV4Response3.setComponentVersions(new ImageComponentVersions("",  "",  "7.3.0", "",  "", "", List.of()));
        UpgradeV4Response upgradeV4Response = new UpgradeV4Response();
        upgradeV4Response.setUpgradeCandidates(List.of(imageInfoV4Response1, imageInfoV4Response2, imageInfoV4Response3));
        UpgradeV4Response response = ThreadBasedUserCrnProvider.doAs(TestConstants.CRN,
                () -> underTest.filterSdxUpgradeResponse(sdxUpgradeRequest, upgradeV4Response, SdxClusterShape.MEDIUM_DUTY_HA));
        assertEquals(1, response.getUpgradeCandidates().size());
        List<String> candidates = response.getUpgradeCandidates().stream().map(candidate -> candidate.getComponentVersions().getCdp()).toList();
        assertTrue(candidates.contains("7.2.17"));
    }

    @Test
    void testListingUpgradeCandidatesOnEnterprise() {
        ImageInfoV4Response imageInfoV4Response1 = new ImageInfoV4Response();
        imageInfoV4Response1.setComponentVersions(new ImageComponentVersions("",  "",  "7.2.17", "",  "", "", List.of()));
        ImageInfoV4Response imageInfoV4Response2 = new ImageInfoV4Response();
        imageInfoV4Response1.setComponentVersions(new ImageComponentVersions("",  "",  "7.2.18", "",  "", "", List.of()));
        ImageInfoV4Response imageInfoV4Response3 = new ImageInfoV4Response();
        imageInfoV4Response1.setComponentVersions(new ImageComponentVersions("",  "",  "7.3.0", "",  "", "", List.of()));
        UpgradeV4Response upgradeV4Response = new UpgradeV4Response();
        upgradeV4Response.setUpgradeCandidates(List.of(imageInfoV4Response1, imageInfoV4Response2, imageInfoV4Response3));
        UpgradeV4Response response = underTest.filterSdxUpgradeResponse(sdxUpgradeRequest, upgradeV4Response, SdxClusterShape.ENTERPRISE);
        assertEquals(3, response.getUpgradeCandidates().size());
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

        underTest.filterSdxUpgradeResponse(sdxUpgradeRequest, upgradeV4Response, SdxClusterShape.ENTERPRISE);

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

}
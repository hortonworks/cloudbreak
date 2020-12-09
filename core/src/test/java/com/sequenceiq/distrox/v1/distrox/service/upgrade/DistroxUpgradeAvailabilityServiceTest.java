package com.sequenceiq.distrox.v1.distrox.service.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageComponentVersions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.common.model.UpgradeShowAvailableImages;
import com.sequenceiq.distrox.v1.distrox.StackOperations;

@ExtendWith(MockitoExtension.class)
class DistroxUpgradeAvailabilityServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:f3b8ed82-e712-4f89-bda7-be07183720d3";

    private static final NameOrCrn CLUSTER = NameOrCrn.ofName("asdf");

    private static final Long WORKSPACE_ID = 1L;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private StackOperations stackOperations;

    @InjectMocks
    private DistroxUpgradeAvailabilityService underTest;

    @BeforeEach
    public void init() {
        lenient().when(entitlementService.datahubRuntimeUpgradeEnabled("9d74eee4-1cad-45d7-b645-7ccf9edbb73d")).thenReturn(Boolean.TRUE);
    }

    @Test
    public void testCrnParseException() {
        Assertions.assertThrows(BadRequestException.class, () -> underTest.isRuntimeUpgradeEnabled("asdf"));
    }

    @Test
    public void testNullPointerException() {
        Assertions.assertThrows(BadRequestException.class, () -> underTest.isRuntimeUpgradeEnabled(null));
    }

    @Test
    public void testEntitlementServiceCalled() {
        boolean result = underTest.isRuntimeUpgradeEnabled(USER_CRN);

        assertTrue(result);
        verify(entitlementService).datahubRuntimeUpgradeEnabled("9d74eee4-1cad-45d7-b645-7ccf9edbb73d");
    }

    @Test
    public void testVerifyRuntimeUpgradeEntitlement() {
        when(entitlementService.datahubRuntimeUpgradeEnabled("9d74eee4-1cad-45d7-b645-7ccf9edbb73d")).thenReturn(Boolean.FALSE);
        UpgradeV4Request request = new UpgradeV4Request();

        assertThrows(BadRequestException.class, () -> underTest.checkForUpgrade(CLUSTER, WORKSPACE_ID, request, USER_CRN),
                "Runtime upgrade feature is not enabled");
    }

    @Test
    public void testReturnAllCandidates() {
        UpgradeV4Request request = new UpgradeV4Request();
        UpgradeV4Response response = new UpgradeV4Response();
        response.setUpgradeCandidates(List.of(mock(ImageInfoV4Response.class), mock(ImageInfoV4Response.class)));
        when(stackOperations.checkForClusterUpgrade(CLUSTER, WORKSPACE_ID, request)).thenReturn(response);

        UpgradeV4Response result = underTest.checkForUpgrade(CLUSTER, WORKSPACE_ID, request, USER_CRN);

        assertEquals(response.getUpgradeCandidates(), result.getUpgradeCandidates());
    }

    @Test
    public void testReturnLatestOnlyForDryRun() {
        UpgradeV4Request request = new UpgradeV4Request();
        request.setDryRun(Boolean.TRUE);
        UpgradeV4Response response = new UpgradeV4Response();
        ImageInfoV4Response image1 = new ImageInfoV4Response();
        image1.setCreated(1L);
        ImageInfoV4Response image2 = new ImageInfoV4Response();
        image2.setCreated(8L);
        ImageInfoV4Response image3 = new ImageInfoV4Response();
        image3.setCreated(5L);
        response.setUpgradeCandidates(List.of(image1, image2, image3));
        when(stackOperations.checkForClusterUpgrade(CLUSTER, WORKSPACE_ID, request)).thenReturn(response);

        UpgradeV4Response result = underTest.checkForUpgrade(CLUSTER, WORKSPACE_ID, request, USER_CRN);

        assertEquals(1, result.getUpgradeCandidates().size());
        assertEquals(8L, result.getUpgradeCandidates().get(0).getCreated());
    }

    @Test
    public void testLatestByRuntime() {
        UpgradeV4Request request = new UpgradeV4Request();
        request.setShowAvailableImages(UpgradeShowAvailableImages.LATEST_ONLY);
        UpgradeV4Response response = new UpgradeV4Response();
        ImageInfoV4Response image1 = createImageResponse(2L, "A");
        ImageInfoV4Response image2 = createImageResponse(8L, "A");
        ImageInfoV4Response image3 = createImageResponse(6L, "A");
        ImageInfoV4Response image4 = createImageResponse(1L, "B");
        ImageInfoV4Response image5 = createImageResponse(4L, "B");
        ImageInfoV4Response image6 = createImageResponse(3L, "B");
        ImageInfoV4Response image7 = createImageResponse(9L, "C");
        ImageInfoV4Response image8 = createImageResponse(8L, "C");
        ImageInfoV4Response image9 = createImageResponse(6L, "C");
        response.setUpgradeCandidates(List.of(image1, image2, image3, image4, image5, image6, image7, image8, image9));
        when(stackOperations.checkForClusterUpgrade(CLUSTER, WORKSPACE_ID, request)).thenReturn(response);

        UpgradeV4Response result = underTest.checkForUpgrade(CLUSTER, WORKSPACE_ID, request, USER_CRN);

        assertEquals(3, result.getUpgradeCandidates().size());
        assertTrue(result.getUpgradeCandidates().stream().anyMatch(img -> img.getCreated() == 8L && "A".equals(img.getComponentVersions().getCdp())));
        assertTrue(result.getUpgradeCandidates().stream().anyMatch(img -> img.getCreated() == 4L && "B".equals(img.getComponentVersions().getCdp())));
        assertTrue(result.getUpgradeCandidates().stream().anyMatch(img -> img.getCreated() == 9L && "C".equals(img.getComponentVersions().getCdp())));
    }

    private ImageInfoV4Response createImageResponse(long creation, String cdp) {
        ImageInfoV4Response image = new ImageInfoV4Response();
        image.setCreated(creation);
        image.setComponentVersions(new ImageComponentVersions("dontcare", "dontcare", cdp, "dontcare", "dontcare", "dontcare"));
        return image;
    }
}
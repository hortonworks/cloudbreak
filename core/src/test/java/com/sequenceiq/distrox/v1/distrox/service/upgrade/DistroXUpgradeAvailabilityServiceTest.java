package com.sequenceiq.distrox.v1.distrox.service.upgrade;

import static com.sequenceiq.common.model.OsType.CENTOS7;
import static com.sequenceiq.common.model.UpgradeShowAvailableImages.LATEST_ONLY;
import static com.sequenceiq.common.model.UpgradeShowAvailableImages.SHOW;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageComponentVersions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.BlueprintUpgradeOption;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.service.image.CurrentImageUsageCondition;
import com.sequenceiq.cloudbreak.service.stack.RuntimeVersionService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.model.UpgradeShowAvailableImages;
import com.sequenceiq.distrox.v1.distrox.StackUpgradeOperations;

@ExtendWith(MockitoExtension.class)
public class DistroXUpgradeAvailabilityServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:f3b8ed82-e712-4f89-bda7-be07183720d3";

    private static final String ACCOUNT_ID = Crn.fromString(USER_CRN).getAccountId();

    private static final NameOrCrn CLUSTER = NameOrCrn.ofName("asdf");

    private static final Long WORKSPACE_ID = 1L;

    private static final long STACK_ID = 2L;

    private static final String ENVIRONMENT_CRN = "envcrn";

    @Mock
    private DistroXUpgradeResponseFilterService distroXUpgradeResponseFilterService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private StackUpgradeOperations stackUpgradeOperations;

    @Mock
    private StackService stackService;

    @Mock
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    @Mock
    private RuntimeVersionService runtimeVersionService;

    @Mock
    private CurrentImageUsageCondition currentImageUsageCondition;

    @InjectMocks
    private DistroXUpgradeAvailabilityService underTest;

    private final Stack stack = new Stack();

    @BeforeEach
    public void init() {
        Blueprint blueprint = TestUtil.blueprint();
        blueprint.setBlueprintUpgradeOption(BlueprintUpgradeOption.ENABLED);
        Cluster cluster = TestUtil.cluster();
        cluster.setBlueprint(blueprint);
        stack.setCluster(cluster);
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        stack.setId(STACK_ID);

        lenient().when(platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(eq(ENVIRONMENT_CRN))).thenReturn(Optional.of(
                new SdxBasicView(null, null, null, false, 1L, null)));
    }

    @Test
    public void testReturnAllCandidates() {
        UpgradeV4Request request = createRequest(LATEST_ONLY, true, true);
        UpgradeV4Response response = new UpgradeV4Response(new ImageInfoV4Response(), List.of(mock(ImageInfoV4Response.class), mock(ImageInfoV4Response.class)),
                null);
        when(stackService.getByNameOrCrnInWorkspace(CLUSTER, WORKSPACE_ID)).thenReturn(stack);
        when(stackUpgradeOperations.checkForClusterUpgrade(ACCOUNT_ID, stack, request)).thenReturn(response);

        UpgradeV4Response result = underTest.checkForUpgrade(CLUSTER, WORKSPACE_ID, request, USER_CRN);

        assertEquals(response.getUpgradeCandidates(), result.getUpgradeCandidates());
    }

    @Test
    public void testWhenRangerRazEnabledAndEntitlementNotGrantedThenUpgradeNotAllowed() {
        UpgradeV4Request request = createRequest(LATEST_ONLY, true, false);
        ImageInfoV4Response currentImage = createImageInfoResponse("currentImage", STACK_ID, "7.2.0");
        ImageInfoV4Response candidateImage1 = createImageInfoResponse("image1", 3L, "7.2.1");
        ImageInfoV4Response candidateImage2 = createImageInfoResponse("image2", 4L, "7.2.2");
        UpgradeV4Response response = new UpgradeV4Response(currentImage, List.of(candidateImage1, candidateImage2), null);
        when(stackService.getByNameOrCrnInWorkspace(CLUSTER, WORKSPACE_ID)).thenReturn(stack);
        when(stackUpgradeOperations.checkForClusterUpgrade(ACCOUNT_ID, stack, request)).thenReturn(response);
        when(runtimeVersionService.getRuntimeVersion(any())).thenReturn(Optional.of("7.2.0"));
        when(platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(eq(ENVIRONMENT_CRN))).thenReturn(Optional.of(
                new SdxBasicView("dummyCluster", null, null, true, 1L, null)));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.checkForUpgrade(CLUSTER, WORKSPACE_ID, request, USER_CRN));

        assertEquals("Data Hub Upgrade is not allowed as Ranger RAZ is enabled for [dummyCluster] cluster, because runtime version is [7.2.0].",
                exception.getMessage());
    }

    private UpgradeV4Request createRequest(UpgradeShowAvailableImages showAvailableImages, boolean dryRun, boolean dataHubRuntimeUpgradeEntitled) {
        UpgradeV4Request request = new UpgradeV4Request();
        request.setShowAvailableImages(showAvailableImages);
        request.setDryRun(dryRun);
        request.setInternalUpgradeSettings(new InternalUpgradeSettings(false, dataHubRuntimeUpgradeEntitled, false));
        return request;
    }

    @Test
    public void testWhenRangerRazDisabledAndEntitlementNotGrantedThenUpgradeAllowed() {
        UpgradeV4Request request = createRequest(LATEST_ONLY, true, false);
        ImageInfoV4Response currentImage = createImageInfoResponse("currentImage", STACK_ID, "7.2.0");
        ImageInfoV4Response candidateImage1 = createImageInfoResponse("image1", 3L, "7.2.1");
        ImageInfoV4Response candidateImage2 = createImageInfoResponse("image2", 4L, "7.2.2");
        UpgradeV4Response response = new UpgradeV4Response(currentImage, List.of(candidateImage1, candidateImage2), null);
        when(stackService.getByNameOrCrnInWorkspace(CLUSTER, WORKSPACE_ID)).thenReturn(stack);
        when(stackUpgradeOperations.checkForClusterUpgrade(ACCOUNT_ID, stack, request)).thenReturn(response);

        assertDoesNotThrow(() -> underTest.checkForUpgrade(CLUSTER, WORKSPACE_ID, request, USER_CRN));

        verify(platformAwareSdxConnector, times(1)).getSdxBasicViewByEnvironmentCrn(ENVIRONMENT_CRN);
    }

    @Test
    public void testReturnLatestOnlyForDryRun() {
        UpgradeV4Request request = createRequest(LATEST_ONLY, true, true);
        ImageInfoV4Response image1 = createImageInfoResponse("image1", 1L, "A");
        ImageInfoV4Response image2 = createImageInfoResponse("image2", 8L, "A");
        ImageInfoV4Response image3 = createImageInfoResponse("image3", 5L, "A");
        ImageInfoV4Response currentImage = createImageInfoResponse("currentImage", STACK_ID, "7.2.0");
        UpgradeV4Response response = new UpgradeV4Response(currentImage, List.of(image1, image2, image3), null);
        when(stackService.getByNameOrCrnInWorkspace(CLUSTER, WORKSPACE_ID)).thenReturn(stack);
        when(entitlementService.isDifferentDataHubAndDataLakeVersionAllowed(anyString())).thenReturn(true);
        when(stackUpgradeOperations.checkForClusterUpgrade(ACCOUNT_ID, stack, request)).thenReturn(response);
        when(currentImageUsageCondition.currentImageUsedOnInstances(any(), any())).thenReturn(true);
        when(distroXUpgradeResponseFilterService.filterForLatestImagePerRuntimeAndOs(response.getUpgradeCandidates(), response.getCurrent()))
                .thenReturn(List.of(image2));

        UpgradeV4Response result = underTest.checkForUpgrade(CLUSTER, WORKSPACE_ID, request, USER_CRN);

        assertEquals(1, result.getUpgradeCandidates().size());
        assertEquals(8L, result.getUpgradeCandidates().getFirst().getCreated());
    }

    @Test
    public void testLatestByRuntime() {
        UpgradeV4Request request = createRequest(LATEST_ONLY, false, true);
        ImageInfoV4Response image1 = createImageInfoResponse("image1", STACK_ID, "A");
        ImageInfoV4Response image2 = createImageInfoResponse("image2", 8L, "A");
        ImageInfoV4Response image3 = createImageInfoResponse("image3", 6L, "A");
        ImageInfoV4Response image4 = createImageInfoResponse("image4", 1L, "B");
        ImageInfoV4Response image5 = createImageInfoResponse("image5", 4L, "B");
        ImageInfoV4Response image6 = createImageInfoResponse("image6", 3L, "B");
        ImageInfoV4Response image7 = createImageInfoResponse("image7", 9L, "C");
        ImageInfoV4Response image8 = createImageInfoResponse("image8", 8L, "C");
        ImageInfoV4Response image9 = createImageInfoResponse("image9", 6L, "C");
        ImageInfoV4Response currentImage = createImageInfoResponse("currentImage", STACK_ID, "7.2.0");
        UpgradeV4Response response = new UpgradeV4Response(currentImage, List.of(image1, image2, image3, image4, image5, image6, image7, image8, image9), null);
        when(stackService.getByNameOrCrnInWorkspace(CLUSTER, WORKSPACE_ID)).thenReturn(stack);
        when(stackUpgradeOperations.checkForClusterUpgrade(ACCOUNT_ID, stack, request)).thenReturn(response);
        when(currentImageUsageCondition.currentImageUsedOnInstances(any(), any())).thenReturn(true);
        when(entitlementService.isDifferentDataHubAndDataLakeVersionAllowed(anyString())).thenReturn(true);
        when(distroXUpgradeResponseFilterService.filterForLatestImagePerRuntimeAndOs(response.getUpgradeCandidates(), response.getCurrent()))
                .thenReturn(List.of(image2, image5, image7));

        UpgradeV4Response result = underTest.checkForUpgrade(CLUSTER, WORKSPACE_ID, request, USER_CRN);

        assertEquals(3, result.getUpgradeCandidates().size());
        assertTrue(result.getUpgradeCandidates().stream().anyMatch(img -> img.getCreated() == 8L && "A".equals(img.getComponentVersions().getCdp())));
        assertTrue(result.getUpgradeCandidates().stream().anyMatch(img -> img.getCreated() == 4L && "B".equals(img.getComponentVersions().getCdp())));
        assertTrue(result.getUpgradeCandidates().stream().anyMatch(img -> img.getCreated() == 9L && "C".equals(img.getComponentVersions().getCdp())));
    }

    @Test
    public void testOnlyReturnCandidatesWithDatalakeVersion() {
        UpgradeV4Request request = createRequest(SHOW, false, true);
        ImageInfoV4Response image1 = createImageInfoResponse("image1", STACK_ID, "A");
        ImageInfoV4Response image2 = createImageInfoResponse("image2", 8L, "A");
        ImageInfoV4Response image3 = createImageInfoResponse("image3", 6L, "A");
        ImageInfoV4Response image4 = createImageInfoResponse("image4", 1L, "B");
        ImageInfoV4Response image5 = createImageInfoResponse("image5", 4L, "B");
        ImageInfoV4Response image6 = createImageInfoResponse("image6", 3L, "B");
        ImageInfoV4Response image7 = createImageInfoResponse("image7", 9L, "C");
        ImageInfoV4Response image8 = createImageInfoResponse("image8", 8L, "C");
        ImageInfoV4Response image9 = createImageInfoResponse("image9", 6L, "C");
        ImageInfoV4Response currentImage = createImageInfoResponse("currentImage", 6L, "C");
        UpgradeV4Response response = new UpgradeV4Response(currentImage, List.of(image1, image2, image3, image4, image5, image6, image7, image8, image9), null);
        when(stackService.getByNameOrCrnInWorkspace(CLUSTER, WORKSPACE_ID)).thenReturn(stack);
        when(stackUpgradeOperations.checkForClusterUpgrade(ACCOUNT_ID, stack, request)).thenReturn(response);
        when(currentImageUsageCondition.currentImageUsedOnInstances(any(), any())).thenReturn(true);
        when(distroXUpgradeResponseFilterService.filterForDatalakeVersion(ENVIRONMENT_CRN, response)).thenReturn(List.of(image7));

        UpgradeV4Response result = underTest.checkForUpgrade(CLUSTER, WORKSPACE_ID, request, USER_CRN);

        assertEquals(1, result.getUpgradeCandidates().size());
        assertTrue(result.getUpgradeCandidates().stream().anyMatch(img -> img.getCreated() == 9L && "C".equals(img.getComponentVersions().getCdp())));
    }

    @Test
    @DisplayName("this test simulates when the Data Lake and Data Hub version is the same (7.1.0)"
            + "and no upgrade options are available with this version.")
    public void testCheckForUpgradeWhenDataLakeAndDataHubIsOnTheSameVersionAndPatchUpgradeIsAvailable() {
        UpgradeV4Request request = createRequest(LATEST_ONLY, false, true);
        UpgradeV4Response response = new UpgradeV4Response();
        response.setUpgradeCandidates(List.of());
        response.setCurrent(new ImageInfoV4Response());
        when(stackService.getByNameOrCrnInWorkspace(CLUSTER, WORKSPACE_ID)).thenReturn(stack);
        when(stackUpgradeOperations.checkForClusterUpgrade(ACCOUNT_ID, stack, request)).thenReturn(response);
        when(currentImageUsageCondition.currentImageUsedOnInstances(any(), any())).thenReturn(true);

        UpgradeV4Response result = underTest.checkForUpgrade(CLUSTER, WORKSPACE_ID, request, USER_CRN);

        assertEquals(0, result.getUpgradeCandidates().size());
        assertNull(result.getReason());
    }

    @Test
    @DisplayName("this test simulates when the Data Lake and Data Hub version is the same (7.1.0)"
            + "and the only upgrade options for Data Hub is newer than the DL (7.2.0,7.3.0,7.4.0). this will be filtered"
            + "since the upgrade options are newer than the Data Lake. Data Lake upgrade shall be suggested")
    public void testCheckForUpgradeWhenDataLakeAndDataHubIsOnTheSameVersionAndNoUpgradeIsAvailable() {
        UpgradeV4Request request = createRequest(SHOW, false, true);
        ImageInfoV4Response image1 = createImageInfoResponse("image1", STACK_ID, "7.2.0");
        ImageInfoV4Response image2 = createImageInfoResponse("image2", 8L, "7.3.0");
        ImageInfoV4Response image3 = createImageInfoResponse("image3", 6L, "7.4.0");
        UpgradeV4Response response = new UpgradeV4Response(createImageInfoResponse("currentImage", 6L, "7.1.0"), List.of(image1, image2, image3), null);
        when(stackService.getByNameOrCrnInWorkspace(CLUSTER, WORKSPACE_ID)).thenReturn(stack);
        when(stackUpgradeOperations.checkForClusterUpgrade(ACCOUNT_ID, stack, request)).thenReturn(response);
        when(entitlementService.isDifferentDataHubAndDataLakeVersionAllowed(anyString())).thenReturn(false);
        when(currentImageUsageCondition.currentImageUsedOnInstances(any(), any())).thenReturn(true);
        String expectedMessage = "Data Hub can only be upgraded to the same version as the Data Lake. "
                + "To upgrade your Data Hub, please upgrade your Data Lake first.";
        when(distroXUpgradeResponseFilterService.filterForDatalakeVersion(ENVIRONMENT_CRN, response)).thenReturn(Collections.emptyList());

        UpgradeV4Response result = underTest.checkForUpgrade(CLUSTER, WORKSPACE_ID, request, USER_CRN);

        assertEquals(0, result.getUpgradeCandidates().size());
        assertEquals(expectedMessage, result.getReason());
    }

    @Test
    @DisplayName("this test simulates when the Data Lake and Data Hub version is the same (7.1.0)"
            + "and the only upgrade options for Data Hub is newer than the DL (7.2.0,7.3.0,7.4.0). this will NOT be filtered"
            + "since the different Data Hub version entitlement is enabled")
    public void testCheckForUpgradeWhenDataLakeAndDataHubIsOnTheSameVersionAndUpgradeIsAvailable() {
        UpgradeV4Request request = createRequest(SHOW, false, true);
        ImageInfoV4Response image1 = createImageInfoResponse("image1", STACK_ID, "7.2.0");
        ImageInfoV4Response image2 = createImageInfoResponse("image2", 8L, "7.3.0");
        ImageInfoV4Response image3 = createImageInfoResponse("image3", 6L, "7.4.0");
        UpgradeV4Response response = new UpgradeV4Response(new ImageInfoV4Response(), List.of(image1, image2, image3), null);
        when(stackService.getByNameOrCrnInWorkspace(CLUSTER, WORKSPACE_ID)).thenReturn(stack);
        when(stackUpgradeOperations.checkForClusterUpgrade(ACCOUNT_ID, stack, request)).thenReturn(response);
        when(entitlementService.isDifferentDataHubAndDataLakeVersionAllowed(anyString())).thenReturn(true);
        when(currentImageUsageCondition.currentImageUsedOnInstances(any(), any())).thenReturn(true);

        UpgradeV4Response result = underTest.checkForUpgrade(CLUSTER, WORKSPACE_ID, request, USER_CRN);

        assertEquals(3, result.getUpgradeCandidates().size());
    }

    @Test
    @DisplayName("this test simulates that a Data Hub runtime upgrade entitlement is disabled"
            + " and there are 2 image candidates for maintenance upgrade and the latest should be returned with dry-run")
    public void testCheckForUpgradeWhenDataHubUpgradeIsDisabledAnMultipleMaintenanceUpgradeCandidatesAreAvailable() {
        UpgradeV4Request request = createRequest(SHOW, true, false);
        ImageInfoV4Response current = createImageInfoResponse("currentImage", 1L, "7.1.0");
        ImageInfoV4Response image1 = createImageInfoResponse("image1", STACK_ID, "7.1.0");
        ImageInfoV4Response image2 = createImageInfoResponse("image2", 3L, "7.1.0");
        ImageInfoV4Response image3 = createImageInfoResponse("image3", 6L, "7.3.0");
        UpgradeV4Response response = new UpgradeV4Response(current, List.of(image1, image2, image3), null);
        when(stackService.getByNameOrCrnInWorkspace(CLUSTER, WORKSPACE_ID)).thenReturn(stack);
        when(stackUpgradeOperations.checkForClusterUpgrade(ACCOUNT_ID, stack, request)).thenReturn(response);
        when(entitlementService.isDifferentDataHubAndDataLakeVersionAllowed(anyString())).thenReturn(false);
        when(currentImageUsageCondition.currentImageUsedOnInstances(any(), any())).thenReturn(true);
        when(distroXUpgradeResponseFilterService.filterForDatalakeVersion(ENVIRONMENT_CRN, response)).thenReturn(List.of(image2));

        UpgradeV4Response result = underTest.checkForUpgrade(CLUSTER, WORKSPACE_ID, request, USER_CRN);

        assertEquals(1, result.getUpgradeCandidates().size());
        assertTrue(result.getUpgradeCandidates().stream().anyMatch(img -> img.getCreated() == 3L && "7.1.0".equals(img.getComponentVersions().getCdp())));
    }

    @Test
    @DisplayName("this test simulates that a Data Hub blueprint is GA for upgrade "
            + " and there 3 image candidates for upgrade")
    public void testCheckForUpgradeWhenDataHubUpgradeIsGaAndOneMaintenanceUpgradeCandidateIsAvailable() {
        UpgradeV4Request request = createRequest(SHOW, false, false);
        ImageInfoV4Response current = createImageInfoResponse("currentImage", 1L, "7.1.0");
        ImageInfoV4Response image1 = createImageInfoResponse("image1", STACK_ID, "7.1.0");
        ImageInfoV4Response image2 = createImageInfoResponse("image2", 8L, "7.2.0");
        ImageInfoV4Response image3 = createImageInfoResponse("image3", 6L, "7.3.0");
        UpgradeV4Response response = new UpgradeV4Response(current, List.of(image1, image2, image3), null);
        stack.getCluster().getBlueprint().setBlueprintUpgradeOption(BlueprintUpgradeOption.GA);
        when(stackService.getByNameOrCrnInWorkspace(CLUSTER, WORKSPACE_ID)).thenReturn(stack);
        when(stackUpgradeOperations.checkForClusterUpgrade(ACCOUNT_ID, stack, request)).thenReturn(response);
        when(currentImageUsageCondition.currentImageUsedOnInstances(any(), any())).thenReturn(true);
        when(entitlementService.isDifferentDataHubAndDataLakeVersionAllowed(anyString())).thenReturn(true);

        UpgradeV4Response result = underTest.checkForUpgrade(CLUSTER, WORKSPACE_ID, request, USER_CRN);

        assertEquals(3, result.getUpgradeCandidates().size());
        assertTrue(result.getUpgradeCandidates().stream().anyMatch(img -> img.getCreated() == STACK_ID && "7.1.0".equals(img.getComponentVersions().getCdp())));
    }

    @Test
    @DisplayName("this test simulates that a Data Hub blueprint upgrade option is not checked for custom "
            + " blueprints and there 3 image candidates for upgrade")
    public void testCheckForUpgradeWhenDataHubUpgradeIsNotGaAndOneMaintenanceUpgradeCandidateIsAvailable() {
        UpgradeV4Request request = createRequest(SHOW, false, false);
        ImageInfoV4Response current = createImageInfoResponse("currentImage", 1L, "7.1.0");
        ImageInfoV4Response image1 = createImageInfoResponse("image1", STACK_ID, "7.1.0");
        ImageInfoV4Response image2 = createImageInfoResponse("image2", 8L, "7.2.0");
        ImageInfoV4Response image3 = createImageInfoResponse("image3", 6L, "7.3.0");
        UpgradeV4Response response = new UpgradeV4Response(current, List.of(image1, image2, image3), null);
        stack.getCluster().getBlueprint().setStatus(ResourceStatus.USER_MANAGED);
        stack.getCluster().getBlueprint().setBlueprintUpgradeOption(BlueprintUpgradeOption.OS_UPGRADE_DISABLED);
        when(stackService.getByNameOrCrnInWorkspace(CLUSTER, WORKSPACE_ID)).thenReturn(stack);
        when(stackUpgradeOperations.checkForClusterUpgrade(ACCOUNT_ID, stack, request)).thenReturn(response);
        when(currentImageUsageCondition.currentImageUsedOnInstances(any(), any())).thenReturn(true);
        when(entitlementService.isDifferentDataHubAndDataLakeVersionAllowed(anyString())).thenReturn(true);

        UpgradeV4Response result = underTest.checkForUpgrade(CLUSTER, WORKSPACE_ID, request, USER_CRN);

        assertEquals(3, result.getUpgradeCandidates().size());
        assertTrue(result.getUpgradeCandidates().stream().anyMatch(img -> img.getCreated() == STACK_ID && "7.1.0".equals(img.getComponentVersions().getCdp())));
    }

    private ImageInfoV4Response createImageInfoResponse(String imageId, long creation, String cdp) {
        ImageInfoV4Response image = new ImageInfoV4Response();
        image.setImageId(imageId);
        image.setCreated(creation);
        image.setComponentVersions(new ImageComponentVersions("dontcare", "dontcare", cdp, "dontcare", CENTOS7.getOs(), "dontcare", List.of()));
        return image;
    }
}
package com.sequenceiq.distrox.v1.distrox.service.upgrade;

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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageComponentVersions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.ClusterView;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.RuntimeVersionService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.StackViewService;
import com.sequenceiq.common.model.UpgradeShowAvailableImages;
import com.sequenceiq.distrox.v1.distrox.StackUpgradeOperations;

@ExtendWith(MockitoExtension.class)
public class DistroXUpgradeAvailabilityServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:f3b8ed82-e712-4f89-bda7-be07183720d3";

    private static final String ACCOUNT_ID = Crn.fromString(USER_CRN).getAccountId();

    private static final NameOrCrn CLUSTER = NameOrCrn.ofName("asdf");

    private static final Long WORKSPACE_ID = 1L;

    private static final Stack STACK = new Stack();

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:default:datalake:d3b8df82-878d-4395-94b1-2e355217446d";

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private StackUpgradeOperations stackUpgradeOperations;

    @Mock
    private StackService stackService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private StackViewService stackViewService;

    @Mock
    private RuntimeVersionService runtimeVersionService;

    @InjectMocks
    private DistroXUpgradeAvailabilityService underTest;

    @BeforeEach
    public void init() {
        lenient().when(entitlementService.datahubRuntimeUpgradeEnabled(ACCOUNT_ID)).thenReturn(Boolean.TRUE);
    }

    @Test
    public void testCrnParseException() {
        Assertions.assertThrows(BadRequestException.class, () -> underTest.isRuntimeUpgradeEnabledByUserCrn("asdf"));
    }

    @Test
    public void testNullPointerException() {
        Assertions.assertThrows(BadRequestException.class, () -> underTest.isRuntimeUpgradeEnabledByUserCrn(null));
    }

    @Test
    public void testEntitlementServiceCalled() {
        boolean result = underTest.isRuntimeUpgradeEnabledByUserCrn(USER_CRN);

        assertTrue(result);
        verify(entitlementService).datahubRuntimeUpgradeEnabled(ACCOUNT_ID);
    }

    @Test
    public void testReturnAllCandidates() {
        UpgradeV4Request request = new UpgradeV4Request();
        UpgradeV4Response response = new UpgradeV4Response();
        response.setUpgradeCandidates(List.of(mock(ImageInfoV4Response.class), mock(ImageInfoV4Response.class)));
        when(stackService.getByNameOrCrnInWorkspace(CLUSTER, WORKSPACE_ID)).thenReturn(STACK);
        when(stackUpgradeOperations.checkForClusterUpgrade(ACCOUNT_ID, STACK, WORKSPACE_ID, request)).thenReturn(response);

        UpgradeV4Response result = underTest.checkForUpgrade(CLUSTER, WORKSPACE_ID, request, USER_CRN);

        assertEquals(response.getUpgradeCandidates(), result.getUpgradeCandidates());
    }

    @Test
    public void testWhenRangerRazEnabledAndEntitlementNotGrantedThenUpgradeNotAllowed() {
        Cluster datalakeCluster = TestUtil.cluster();
        datalakeCluster.setRangerRazEnabled(true);
        Stack datalake = TestUtil.stack();
        STACK.setDatalakeCrn(DATALAKE_CRN);
        UpgradeV4Request request = new UpgradeV4Request();
        UpgradeV4Response response = new UpgradeV4Response();
        ImageInfoV4Response currentImage = createImageResponse(2L, "7.2.0");
        ImageInfoV4Response candidateImage1 = createImageResponse(3L, "7.2.1");
        ImageInfoV4Response candidateImage2 = createImageResponse(4L, "7.2.2");

        response.setUpgradeCandidates(List.of(candidateImage1, candidateImage2));
        response.setCurrent(currentImage);
        when(entitlementService.datahubRuntimeUpgradeEnabled(ACCOUNT_ID)).thenReturn(false);
        when(stackService.getByCrn(DATALAKE_CRN)).thenReturn(datalake);
        when(stackService.getByNameOrCrnInWorkspace(CLUSTER, WORKSPACE_ID)).thenReturn(STACK);
        when(stackUpgradeOperations.checkForClusterUpgrade(ACCOUNT_ID, STACK, WORKSPACE_ID, request)).thenReturn(response);
        when(clusterService.getCluster(datalake)).thenReturn(datalakeCluster);
        when(runtimeVersionService.getRuntimeVersion(any())).thenReturn(Optional.of("7.2.0"));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.checkForUpgrade(CLUSTER, WORKSPACE_ID, request, USER_CRN));

        assertEquals("Data Hub Upgrade is not allowed as Ranger RAZ is enabled for [dummyCluster] cluster, because runtime version is [7.2.0].",
                exception.getMessage());
    }

    @Test
    public void testWhenRangerRazDisabledAndEntitlementNotGrantedThenUpgradeAllowed() {
        Cluster datalakeCluster = TestUtil.cluster();
        datalakeCluster.setRangerRazEnabled(false);
        Stack datalake = TestUtil.stack();
        STACK.setDatalakeCrn(DATALAKE_CRN);
        STACK.setCluster(TestUtil.cluster());
        UpgradeV4Request request = new UpgradeV4Request();
        UpgradeV4Response response = new UpgradeV4Response();
        ImageInfoV4Response currentImage = createImageResponse(2L, "7.2.0");
        ImageInfoV4Response candidateImage1 = createImageResponse(3L, "7.2.1");
        ImageInfoV4Response candidateImage2 = createImageResponse(4L, "7.2.2");

        response.setUpgradeCandidates(List.of(candidateImage1, candidateImage2));
        response.setCurrent(currentImage);
        when(entitlementService.datahubRuntimeUpgradeEnabled(ACCOUNT_ID)).thenReturn(false);
        when(stackService.getByNameOrCrnInWorkspace(CLUSTER, WORKSPACE_ID)).thenReturn(STACK);
        when(stackUpgradeOperations.checkForClusterUpgrade(ACCOUNT_ID, STACK, WORKSPACE_ID, request)).thenReturn(response);
        when(stackService.getByCrn(DATALAKE_CRN)).thenReturn(datalake);
        when(clusterService.getCluster(any())).thenReturn(datalakeCluster);

        assertDoesNotThrow(() -> underTest.checkForUpgrade(CLUSTER, WORKSPACE_ID, request, USER_CRN));

        verify(clusterService, never()).getCluster(STACK);
    }

    @Test
    public void testWhenRangerRazDisabledAndEntitlementGrantedThenUpgradeAllowed() {
        UpgradeV4Request request = new UpgradeV4Request();
        UpgradeV4Response response = new UpgradeV4Response();
        ImageInfoV4Response currentImage = createImageResponse(2L, "7.2.0");
        ImageInfoV4Response candidateImage1 = createImageResponse(3L, "7.2.1");
        ImageInfoV4Response candidateImage2 = createImageResponse(4L, "7.2.2");

        response.setUpgradeCandidates(List.of(candidateImage1, candidateImage2));
        response.setCurrent(currentImage);
        when(entitlementService.datahubRuntimeUpgradeEnabled(ACCOUNT_ID)).thenReturn(true);
        when(stackService.getByNameOrCrnInWorkspace(CLUSTER, WORKSPACE_ID)).thenReturn(STACK);
        when(stackUpgradeOperations.checkForClusterUpgrade(ACCOUNT_ID, STACK, WORKSPACE_ID, request)).thenReturn(response);

        assertDoesNotThrow(() -> underTest.checkForUpgrade(CLUSTER, WORKSPACE_ID, request, USER_CRN));

        verify(clusterService, never()).getCluster(STACK);
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
        when(stackService.getByNameOrCrnInWorkspace(CLUSTER, WORKSPACE_ID)).thenReturn(STACK);
        when(stackUpgradeOperations.checkForClusterUpgrade(ACCOUNT_ID, STACK, WORKSPACE_ID, request)).thenReturn(response);

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
        when(stackService.getByNameOrCrnInWorkspace(CLUSTER, WORKSPACE_ID)).thenReturn(STACK);
        when(stackUpgradeOperations.checkForClusterUpgrade(ACCOUNT_ID, STACK, WORKSPACE_ID, request)).thenReturn(response);

        UpgradeV4Response result = underTest.checkForUpgrade(CLUSTER, WORKSPACE_ID, request, USER_CRN);

        assertEquals(3, result.getUpgradeCandidates().size());
        assertTrue(result.getUpgradeCandidates().stream().anyMatch(img -> img.getCreated() == 8L && "A".equals(img.getComponentVersions().getCdp())));
        assertTrue(result.getUpgradeCandidates().stream().anyMatch(img -> img.getCreated() == 4L && "B".equals(img.getComponentVersions().getCdp())));
        assertTrue(result.getUpgradeCandidates().stream().anyMatch(img -> img.getCreated() == 9L && "C".equals(img.getComponentVersions().getCdp())));
    }

    @Test
    public void testOnlyReturnCandidatesWithDatalakeVersion() {
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
        Stack stackWithEnv = new Stack();
        stackWithEnv.setEnvironmentCrn("envcrn");
        StackView stackView = new StackView();
        ClusterView clusterView = new ClusterView();
        clusterView.setId(1L);
        ReflectionTestUtils.setField(stackView, "cluster", clusterView);
        when(stackService.getByNameOrCrnInWorkspace(CLUSTER, WORKSPACE_ID)).thenReturn(stackWithEnv);
        when(stackUpgradeOperations.checkForClusterUpgrade(ACCOUNT_ID, stackWithEnv, WORKSPACE_ID, request)).thenReturn(response);
        when(stackViewService.findDatalakeViewByEnvironmentCrn(stackWithEnv.getEnvironmentCrn())).thenReturn(Optional.of(stackView));
        when(runtimeVersionService.getRuntimeVersion(eq(clusterView.getId()))).thenReturn(Optional.of("C"));
        when(entitlementService.datahubRuntimeUpgradeEnabled(ACCOUNT_ID)).thenReturn(true);

        UpgradeV4Response result = underTest.checkForUpgrade(CLUSTER, WORKSPACE_ID, request, USER_CRN);

        assertEquals(1, result.getUpgradeCandidates().size());
        assertTrue(result.getUpgradeCandidates().stream().anyMatch(img -> img.getCreated() == 9L && "C".equals(img.getComponentVersions().getCdp())));
    }

    @Test
    @DisplayName("this test simulates when the Data Lake and Data Hub version is the same (7.1.0)"
            + "and no upgrade options are available with this version.")
    public void testCheckForUpgradeWhenDataLakeAndDataHubIsOnTheSameVersionAndPatchUpgradeIsAvailable() {
        UpgradeV4Request request = new UpgradeV4Request();
        UpgradeV4Response response = new UpgradeV4Response();
        response.setUpgradeCandidates(List.of());
        Stack stackWithEnv = new Stack();
        stackWithEnv.setEnvironmentCrn("envcrn");
        when(stackService.getByNameOrCrnInWorkspace(CLUSTER, WORKSPACE_ID)).thenReturn(stackWithEnv);
        when(stackUpgradeOperations.checkForClusterUpgrade(ACCOUNT_ID, stackWithEnv, WORKSPACE_ID, request)).thenReturn(response);

        UpgradeV4Response result = underTest.checkForUpgrade(CLUSTER, WORKSPACE_ID, request, USER_CRN);

        assertEquals(0, result.getUpgradeCandidates().size());
        assertNull(result.getReason());
    }

    @Test
    @DisplayName("this test simulates when the Data Lake and Data Hub version is the same (7.1.0)"
            + "and the only upgrade options for Data Hub is newer than the DL (7.2.0,7.3.0,7.4.0). this will be filtered"
            + "since the upgrade options are newer than the Data Lake. Data Lake upgrade shall be suggested")
    public void testCheckForUpgradeWhenDataLakeAndDataHubIsOnTheSameVersionAndNoUpgradeIsAvailable() {
        UpgradeV4Request request = new UpgradeV4Request();
        UpgradeV4Response response = new UpgradeV4Response();
        ImageInfoV4Response image1 = createImageResponse(2L, "7.2.0");
        ImageInfoV4Response image2 = createImageResponse(8L, "7.3.0");
        ImageInfoV4Response image3 = createImageResponse(6L, "7.4.0");
        response.setUpgradeCandidates(List.of(image1, image2, image3));
        Stack stackWithEnv = new Stack();
        stackWithEnv.setEnvironmentCrn("envcrn");
        StackView stackView = new StackView();
        ClusterView clusterView = new ClusterView();
        clusterView.setId(1L);
        ReflectionTestUtils.setField(stackView, "cluster", clusterView);
        when(stackService.getByNameOrCrnInWorkspace(CLUSTER, WORKSPACE_ID)).thenReturn(stackWithEnv);
        when(stackUpgradeOperations.checkForClusterUpgrade(ACCOUNT_ID, stackWithEnv, WORKSPACE_ID, request)).thenReturn(response);
        when(stackViewService.findDatalakeViewByEnvironmentCrn(stackWithEnv.getEnvironmentCrn())).thenReturn(Optional.of(stackView));
        when(runtimeVersionService.getRuntimeVersion(eq(clusterView.getId()))).thenReturn(Optional.of("7.1.0"));
        when(entitlementService.isDifferentDataHubAndDataLakeVersionAllowed(anyString())).thenReturn(false);
        when(entitlementService.datahubRuntimeUpgradeEnabled(ACCOUNT_ID)).thenReturn(true);

        UpgradeV4Response result = underTest.checkForUpgrade(CLUSTER, WORKSPACE_ID, request, USER_CRN);

        String expectedMessage = "Data Hub can only be upgraded to the same version as the Data Lake (7.1.0). "
                + "To upgrade your Data Hub, please upgrade your Data Lake first.";
        assertEquals(0, result.getUpgradeCandidates().size());
        assertEquals(expectedMessage, result.getReason());
    }

    @Test
    @DisplayName("this test simulates when the Data Lake and Data Hub version is the same (7.1.0)"
            + "and the only upgrade options for Data Hub is newer than the DL (7.2.0,7.3.0,7.4.0). this will NOT be filtered"
            + "since the different Data Hub version entitlement is enabled")
    public void testCheckForUpgradeWhenDataLakeAndDataHubIsOnTheSameVersionAndUpgradeIsAvailable() {
        UpgradeV4Request request = new UpgradeV4Request();
        UpgradeV4Response response = new UpgradeV4Response();
        ImageInfoV4Response image1 = createImageResponse(2L, "7.2.0");
        ImageInfoV4Response image2 = createImageResponse(8L, "7.3.0");
        ImageInfoV4Response image3 = createImageResponse(6L, "7.4.0");
        response.setUpgradeCandidates(List.of(image1, image2, image3));
        Stack stackWithEnv = new Stack();
        stackWithEnv.setEnvironmentCrn("envcrn");
        StackView stackView = new StackView();
        ClusterView clusterView = new ClusterView();
        clusterView.setId(1L);
        ReflectionTestUtils.setField(stackView, "cluster", clusterView);
        when(stackService.getByNameOrCrnInWorkspace(CLUSTER, WORKSPACE_ID)).thenReturn(stackWithEnv);
        when(stackUpgradeOperations.checkForClusterUpgrade(ACCOUNT_ID, stackWithEnv, WORKSPACE_ID, request)).thenReturn(response);
        when(entitlementService.isDifferentDataHubAndDataLakeVersionAllowed(anyString())).thenReturn(true);

        UpgradeV4Response result = underTest.checkForUpgrade(CLUSTER, WORKSPACE_ID, request, USER_CRN);

        assertEquals(3, result.getUpgradeCandidates().size());
    }

    @Test
    @DisplayName("this test simulates that a Data Hub runtime upgrade entitlement is disabled"
            + " and all the image candidates are filtered for maintenance upgrade so empty response should be returned")
    public void testCheckForUpgradeWhenDataHubUpgradeIsDisabledAnNoMaintenanceUpgradeCandidatesAreAvaiable() {
        Cluster datalakeCluster = TestUtil.cluster();
        datalakeCluster.setRangerRazEnabled(false);
        UpgradeV4Request request = new UpgradeV4Request();
        UpgradeV4Response response = new UpgradeV4Response();
        ImageInfoV4Response current = createImageResponse(1L, "7.1.0");
        ImageInfoV4Response image1 = createImageResponse(2L, "7.2.0");
        ImageInfoV4Response image2 = createImageResponse(8L, "7.3.0");
        ImageInfoV4Response image3 = createImageResponse(6L, "7.4.0");
        response.setUpgradeCandidates(List.of(image1, image2, image3));
        response.setCurrent(current);
        Stack stackWithEnv = new Stack();
        stackWithEnv.setName("stack");
        stackWithEnv.setEnvironmentCrn("envcrn");
        stackWithEnv.setCluster(TestUtil.cluster());
        when(clusterService.getCluster(any())).thenReturn(datalakeCluster);
        when(stackService.getByNameOrCrnInWorkspace(CLUSTER, WORKSPACE_ID)).thenReturn(stackWithEnv);
        when(stackUpgradeOperations.checkForClusterUpgrade(ACCOUNT_ID, stackWithEnv, WORKSPACE_ID, request)).thenReturn(response);
        when(entitlementService.datahubRuntimeUpgradeEnabled(ACCOUNT_ID)).thenReturn(false);

        UpgradeV4Response result = underTest.checkForUpgrade(CLUSTER, WORKSPACE_ID, request, USER_CRN);

        String expectedMessage = " No image is available for maintenance upgrade, CDP version: 7.1.0";
        assertEquals(0, result.getUpgradeCandidates().size());
        assertEquals(expectedMessage, result.getReason());
    }

    @Test
    @DisplayName("this test simulates that a Data Hub runtime upgrade entitlement is disabled"
            + " and there is 1 image candidate for maintenance upgrade")
    public void testCheckForUpgradeWhenDataHubUpgradeIsDisabledAnOneMaintenanceUpgradeCandidateIsAvaiable() {
        Cluster datalakeCluster = TestUtil.cluster();
        datalakeCluster.setRangerRazEnabled(false);
        UpgradeV4Request request = new UpgradeV4Request();
        UpgradeV4Response response = new UpgradeV4Response();
        ImageInfoV4Response current = createImageResponse(1L, "7.1.0");
        ImageInfoV4Response image1 = createImageResponse(2L, "7.1.0");
        ImageInfoV4Response image2 = createImageResponse(8L, "7.2.0");
        ImageInfoV4Response image3 = createImageResponse(6L, "7.3.0");
        response.setUpgradeCandidates(List.of(image1, image2, image3));
        response.setCurrent(current);
        Stack stackWithEnv = new Stack();
        stackWithEnv.setName("stack");
        stackWithEnv.setEnvironmentCrn("envcrn");
        stackWithEnv.setCluster(TestUtil.cluster());
        when(clusterService.getCluster(any())).thenReturn(datalakeCluster);
        when(stackService.getByNameOrCrnInWorkspace(CLUSTER, WORKSPACE_ID)).thenReturn(stackWithEnv);
        when(stackUpgradeOperations.checkForClusterUpgrade(ACCOUNT_ID, stackWithEnv, WORKSPACE_ID, request)).thenReturn(response);
        when(entitlementService.datahubRuntimeUpgradeEnabled(ACCOUNT_ID)).thenReturn(false);

        UpgradeV4Response result = underTest.checkForUpgrade(CLUSTER, WORKSPACE_ID, request, USER_CRN);

        assertEquals(1, result.getUpgradeCandidates().size());
        assertTrue(result.getUpgradeCandidates().stream().anyMatch(img -> img.getCreated() == 2L && "7.1.0".equals(img.getComponentVersions().getCdp())));
    }

    @Test
    @DisplayName("this test simulates that a Data Hub runtime upgrade entitlement is disabled"
            + " and there are 2 image candidates for maintenance upgrade and the latest should be returned with dry-run")
    public void testCheckForUpgradeWhenDataHubUpgradeIsDisabledAnMultipleMaintenanceUpgradeCandidatesAreAvaiable() {
        Cluster datalakeCluster = TestUtil.cluster();
        datalakeCluster.setRangerRazEnabled(false);
        UpgradeV4Request request = new UpgradeV4Request();
        request.setDryRun(true);
        UpgradeV4Response response = new UpgradeV4Response();
        ImageInfoV4Response current = createImageResponse(1L, "7.1.0");
        ImageInfoV4Response image1 = createImageResponse(2L, "7.1.0");
        ImageInfoV4Response image2 = createImageResponse(3L, "7.1.0");
        ImageInfoV4Response image3 = createImageResponse(6L, "7.3.0");
        response.setUpgradeCandidates(List.of(image1, image2, image3));
        response.setCurrent(current);
        Stack stackWithEnv = new Stack();
        stackWithEnv.setName("stack");
        stackWithEnv.setEnvironmentCrn("envcrn");
        StackView stackView = new StackView();
        ClusterView clusterView = new ClusterView();
        clusterView.setId(1L);
        ReflectionTestUtils.setField(stackView, "cluster", clusterView);
        stackWithEnv.setCluster(TestUtil.cluster());
        when(runtimeVersionService.getRuntimeVersion(any())).thenReturn(Optional.of("C"));
        when(clusterService.getCluster(any())).thenReturn(datalakeCluster);
        when(stackService.getByNameOrCrnInWorkspace(CLUSTER, WORKSPACE_ID)).thenReturn(stackWithEnv);
        when(stackUpgradeOperations.checkForClusterUpgrade(ACCOUNT_ID, stackWithEnv, WORKSPACE_ID, request)).thenReturn(response);
        when(entitlementService.datahubRuntimeUpgradeEnabled(ACCOUNT_ID)).thenReturn(false);
        when(entitlementService.isDifferentDataHubAndDataLakeVersionAllowed(anyString())).thenReturn(false);
        when(stackViewService.findDatalakeViewByEnvironmentCrn(stackWithEnv.getEnvironmentCrn())).thenReturn(Optional.of(stackView));
        when(runtimeVersionService.getRuntimeVersion(eq(clusterView.getId()))).thenReturn(Optional.of("7.1.0"));

        UpgradeV4Response result = underTest.checkForUpgrade(CLUSTER, WORKSPACE_ID, request, USER_CRN);

        assertEquals(1, result.getUpgradeCandidates().size());
        assertTrue(result.getUpgradeCandidates().stream().anyMatch(img -> img.getCreated() == 3L && "7.1.0".equals(img.getComponentVersions().getCdp())));
    }

    private ImageInfoV4Response createImageResponse(long creation, String cdp) {
        ImageInfoV4Response image = new ImageInfoV4Response();
        image.setCreated(creation);
        image.setComponentVersions(new ImageComponentVersions("dontcare", "dontcare", cdp, "dontcare", "dontcare", "dontcare", List.of()));
        return image;
    }
}
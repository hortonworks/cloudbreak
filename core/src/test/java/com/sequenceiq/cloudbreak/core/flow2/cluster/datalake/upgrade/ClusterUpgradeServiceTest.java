package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade;

import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CDH_BUILD_NUMBER;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CFM;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CM;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CM_BUILD_NUMBER;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.SALT;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.STACK;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MANAGER_UPGRADE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MANAGER_UPGRADE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MANAGER_UPGRADE_NOT_NEEDED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_UPGRADE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_UPGRADE_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_UPGRADE_FINISHED_NOVERSION;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_ROLLING_UPGRADE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_UPGRADE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.util.NullUtil;

@ExtendWith(MockitoExtension.class)
public class ClusterUpgradeServiceTest {

    private static final String ERROR_MESSAGE = "error message";

    private static final String SUCCESS_MESSAGE = "Cluster was successfully upgraded.";

    private static final long STACK_ID = 1L;

    private static final String CLOUD_PLATFORM = "aws";

    private static final String OS_TYPE = "redhat7";

    private static final String OS = "centos7";

    private static final String V_7_0_2 = "7.0.2";

    private static final String CMF_VERSION = "2.0.0.0-121";

    private static final String CSP_VERSION = "3.0.0.0-103";

    private static final String SALT_VERSION = "2017.7.5";

    private static final String CURRENT_BUILD_NUMBER = "1";

    private static final String TARGET_BUILD_NUMBER = "2";

    private static final String CURRENT_IMAGE_ID = "f58c5f97-4609-4b47-6498-1c1bc6a4501c";

    private static final String IMAGE_ID = "image-id";

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private ClusterService clusterService;

    @InjectMocks
    private ClusterUpgradeService underTest;

    private static Image createImage(String cmBuildNumber, String stackBuildNumber) {
        return Image.builder()
                .withOs(OS)
                .withUuid(CURRENT_IMAGE_ID)
                .withVersion(V_7_0_2)
                .withImageSetsByProvider(Map.of(CLOUD_PLATFORM, Collections.emptyMap()))
                .withStackDetails(new ImageStackDetails(V_7_0_2, null, stackBuildNumber))
                .withOsType(OS_TYPE)
                .withPackageVersions(createPackageVersions(cmBuildNumber, stackBuildNumber))
                .withCmBuildNumber(cmBuildNumber)
                .withAdvertised(true)
                .build();
    }

    private static Image createImage(String cmBuildNumber) {
        return createImage(cmBuildNumber, "");
    }

    private static Map<String, String> createPackageVersions(String cmBuildNumber, String stackBuildNumber) {
        Map<String, String> packageVersions = new HashMap<>();
        packageVersions.put(CM.getKey(), V_7_0_2);
        packageVersions.put(STACK.getKey(), V_7_0_2);
        packageVersions.put(CFM.getKey(), CMF_VERSION);
        packageVersions.put("csp", CSP_VERSION);
        packageVersions.put(SALT.getKey(), SALT_VERSION);
        Optional.ofNullable(cmBuildNumber).ifPresent(buildNumber -> packageVersions.put(CM_BUILD_NUMBER.getKey(), cmBuildNumber));
        Optional.ofNullable(stackBuildNumber).ifPresent(buildNumber -> packageVersions.put(CDH_BUILD_NUMBER.getKey(), stackBuildNumber));
        return packageVersions;
    }

    private static Stream<Arguments> upgradeNeededVersions() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of("", null),
                Arguments.of(null, ""),
                Arguments.of("", ""),
                Arguments.of("version", ""),
                Arguments.of("version", null),
                Arguments.of("", "version"),
                Arguments.of(null, "version"),
                Arguments.of(CURRENT_BUILD_NUMBER, TARGET_BUILD_NUMBER)
        );
    }

    private static Stream<Arguments> upgradeNeededImages() {
        return Stream.of(
                Arguments.of(Map.of(CM_BUILD_NUMBER.getKey(), CURRENT_BUILD_NUMBER), createImage(CURRENT_BUILD_NUMBER)),
                Arguments.of(Map.of(CM_BUILD_NUMBER.getKey(), CURRENT_BUILD_NUMBER, CDH_BUILD_NUMBER.getKey(), CURRENT_BUILD_NUMBER),
                        createImage(CURRENT_BUILD_NUMBER)),
                Arguments.of(Map.of(CM_BUILD_NUMBER.getKey(), CURRENT_BUILD_NUMBER), createImage(CURRENT_BUILD_NUMBER, CURRENT_BUILD_NUMBER))
        );
    }

    private static Map<String, String> createPackageVersionMap(String cdhVersion, String cdhBuildNumber) {
        return Map.of(STACK.getKey(), cdhVersion, CDH_BUILD_NUMBER.getKey(), cdhBuildNumber);
    }

    @Test
    public void testClusterManagerUpgrade() {
        // GIVEN
        // WHEN
        underTest.upgradeClusterManager(STACK_ID);
        // THEN
        verify(flowMessageService, times(0))
                .fireEventAndLog(STACK_ID, Status.UPDATE_IN_PROGRESS.name(), CLUSTER_MANAGER_UPGRADE_NOT_NEEDED, CURRENT_BUILD_NUMBER);
        verify(flowMessageService).fireEventAndLog(STACK_ID, Status.UPDATE_IN_PROGRESS.name(), CLUSTER_MANAGER_UPGRADE);
    }

    @ParameterizedTest
    @MethodSource("upgradeNeededVersions")
    public void testClusterUpgradeFinishedWhenNeeded(String currentStackVersion, String targetStackVersion) {
        // GIVEN
        Map<String, String> currentImagePackages = createPackageVersions(CURRENT_BUILD_NUMBER,
                currentStackVersion);
        StatedImage targetImage = StatedImage.statedImage(createImage(CURRENT_BUILD_NUMBER, targetStackVersion), null, null);
        // WHEN
        underTest.clusterUpgradeFinished(STACK_ID, currentImagePackages, targetImage, false);
        // THEN
        verify(stackUpdater).updateStackStatus(STACK_ID, DetailedStackStatus.CLUSTER_UPGRADE_FINISHED, SUCCESS_MESSAGE);
        verify(flowMessageService).fireEventAndLog(STACK_ID, Status.AVAILABLE.name(), CLUSTER_UPGRADE_FINISHED, V_7_0_2);
    }

    @ParameterizedTest
    @MethodSource("upgradeNeededImages")
    public void testClusterUpgradeFinishedWhenNeededNoStackDetails(Map<String, String> currentImagePackages, Image targetIm) {
        // GIVEN
        StatedImage targetImage = StatedImage.statedImage(targetIm, null, null);
        // WHEN
        underTest.clusterUpgradeFinished(STACK_ID, currentImagePackages, targetImage, false);
        // THEN
        verify(stackUpdater).updateStackStatus(STACK_ID, DetailedStackStatus.CLUSTER_UPGRADE_FINISHED, SUCCESS_MESSAGE);
        verify(flowMessageService).fireEventAndLog(STACK_ID, Status.AVAILABLE.name(), CLUSTER_UPGRADE_FINISHED,
                NullUtil.getIfNotNull(targetIm.getStackDetails(), ImageStackDetails::getVersion));
    }

    @Test
    public void testClusterUpgradeFinishedWhenNotNeededAndRollingUpgradeIsEnabled() {
        // GIVEN
        Map<String, String> currentImagePackages = createPackageVersions(CURRENT_BUILD_NUMBER,
                CURRENT_BUILD_NUMBER);
        StatedImage targetImage = StatedImage.statedImage(createImage(CURRENT_BUILD_NUMBER, CURRENT_BUILD_NUMBER), null, null);
        // WHEN
        underTest.clusterUpgradeFinished(STACK_ID, currentImagePackages, targetImage, true);
        // THEN
        verify(stackUpdater).updateStackStatus(STACK_ID, DetailedStackStatus.CLUSTER_ROLLING_UPGRADE_FINISHED, SUCCESS_MESSAGE);
        verify(flowMessageService).fireEventAndLog(STACK_ID, Status.AVAILABLE.name(), CLUSTER_UPGRADE_FINISHED_NOVERSION);
    }

    @Test
    public void testClusterManagerUpgradeFailureWhenRollingUpgradeIsNotEnabled() {
        underTest.handleUpgradeClusterFailure(STACK_ID, ERROR_MESSAGE, DetailedStackStatus.CLUSTER_MANAGER_UPGRADE_FAILED, false);

        verify(stackUpdater).updateStackStatus(STACK_ID, DetailedStackStatus.CLUSTER_MANAGER_UPGRADE_FAILED, ERROR_MESSAGE);
        verify(flowMessageService).fireEventAndLog(STACK_ID, Status.UPDATE_FAILED.name(), CLUSTER_MANAGER_UPGRADE_FAILED, ERROR_MESSAGE);
    }

    @Test
    public void testClusterManagerUpgradeFailureWhenRollingUpgradeIsEnabled() {
        underTest.handleUpgradeClusterFailure(STACK_ID, ERROR_MESSAGE, DetailedStackStatus.CLUSTER_MANAGER_UPGRADE_FAILED, true);

        verify(stackUpdater).updateStackStatus(STACK_ID, DetailedStackStatus.CLUSTER_MANAGER_ROLLING_UPGRADE_FAILED, ERROR_MESSAGE);
        verify(flowMessageService).fireEventAndLog(STACK_ID, Status.UPDATE_FAILED.name(), CLUSTER_MANAGER_UPGRADE_FAILED, ERROR_MESSAGE);
    }

    @Test
    public void testClusterUpgradeFailureWhenRollingUpgradeIsNotEnabled() {
        underTest.handleUpgradeClusterFailure(STACK_ID, ERROR_MESSAGE, DetailedStackStatus.CLUSTER_UPGRADE_FAILED, false);

        verify(stackUpdater).updateStackStatus(STACK_ID, DetailedStackStatus.CLUSTER_UPGRADE_FAILED, ERROR_MESSAGE);
        verify(flowMessageService).fireEventAndLog(STACK_ID, Status.UPDATE_FAILED.name(), CLUSTER_UPGRADE_FAILED, ERROR_MESSAGE);
    }

    @Test
    public void testClusterUpgradeFailureWhenRollingUpgradeIsEnabled() {
        underTest.handleUpgradeClusterFailure(STACK_ID, ERROR_MESSAGE, DetailedStackStatus.CLUSTER_UPGRADE_FAILED, true);

        verify(stackUpdater).updateStackStatus(STACK_ID, DetailedStackStatus.CLUSTER_ROLLING_UPGRADE_FAILED, ERROR_MESSAGE);
        verify(flowMessageService).fireEventAndLog(STACK_ID, Status.UPDATE_FAILED.name(), CLUSTER_UPGRADE_FAILED, ERROR_MESSAGE);
    }

    @Test
    void testInitUpgradeClusterWhenRollingUpgradeIsEnabled() {
        Image image = Image.builder().withStackDetails(new ImageStackDetails(V_7_0_2, null, null)).withUuid(IMAGE_ID).build();
        underTest.initUpgradeCluster(STACK_ID, StatedImage.statedImage(image, null, null), true);
        verify(flowMessageService).fireEventAndLog(STACK_ID, Status.UPDATE_IN_PROGRESS.name(), DATALAKE_ROLLING_UPGRADE, V_7_0_2, IMAGE_ID);
        verify(clusterService).updateClusterStatusByStackId(STACK_ID, DetailedStackStatus.CLUSTER_ROLLING_UPGRADE_STARTED, "Cluster upgrade has been started.");
    }

    @Test
    void testInitUpgradeClusterWhenRollingUpgradeIsDisabled() {
        Image image = Image.builder().withStackDetails(new ImageStackDetails(V_7_0_2, null, null)).withUuid(IMAGE_ID).build();
        underTest.initUpgradeCluster(STACK_ID, StatedImage.statedImage(image, null, null), false);
        verify(flowMessageService).fireEventAndLog(STACK_ID, Status.UPDATE_IN_PROGRESS.name(), DATALAKE_UPGRADE, V_7_0_2, IMAGE_ID);
        verify(clusterService).updateClusterStatusByStackId(STACK_ID, DetailedStackStatus.CLUSTER_UPGRADE_STARTED, "Cluster upgrade has been started.");
    }

    @Test
    void testIsRuntimeUpgradeNecessaryShouldReturnTrueWhenThereAreUpgradeCandidates() {
        assertTrue(underTest.isRuntimeUpgradeNecessary(Set.of(new ClouderaManagerProduct())));
    }

    @Test
    void testIsRuntimeUpgradeNecessaryShouldReturnFalseWhenThereAreNoUpgradeCandidates() {
        assertFalse(underTest.isRuntimeUpgradeNecessary(Collections.emptySet()));
    }
}

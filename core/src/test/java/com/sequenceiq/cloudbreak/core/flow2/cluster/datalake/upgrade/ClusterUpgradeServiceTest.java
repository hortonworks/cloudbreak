package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MANAGER_UPGRADE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MANAGER_UPGRADE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MANAGER_UPGRADE_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MANAGER_UPGRADE_NOT_NEEDED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_UPGRADE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_UPGRADE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_UPGRADE_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_UPGRADE_FINISHED_NOVERSION;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_UPGRADE_NOT_NEEDED;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackDetails;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.util.NullUtil;

public class ClusterUpgradeServiceTest {
    private static final String ERROR_MESSAGE = "error message";

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

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private StackUpdater stackUpdater;

    @InjectMocks
    private ClusterUpgradeService underTest;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
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
    public void testClusterUpgradeNeededButCMNot(String currentStackVersion, String targetStackVersion) {
        // GIVEN
        Image currentImage = createImage(CURRENT_BUILD_NUMBER, currentStackVersion);
        Image targetImage = createImage(CURRENT_BUILD_NUMBER, targetStackVersion);
        // WHEN
        boolean actualResult = underTest.upgradeCluster(STACK_ID, currentImage, targetImage);
        // THEN
        Assertions.assertTrue(actualResult);
        verify(flowMessageService, times(0))
                .fireEventAndLog(eq(STACK_ID), eq(Status.UPDATE_IN_PROGRESS.name()), eq(CLUSTER_MANAGER_UPGRADE_FINISHED), anyString());
        verify(flowMessageService).fireEventAndLog(STACK_ID, Status.UPDATE_IN_PROGRESS.name(), CLUSTER_UPGRADE);
    }

    @ParameterizedTest
    @MethodSource("upgradeNeededImages")
    public void testClusterUpgradeNeededButCMNotNoStackDetails(Image currentImage, Image targetImage) {
        // WHEN
        boolean actualResult = underTest.upgradeCluster(STACK_ID, currentImage, targetImage);
        // THEN
        Assertions.assertTrue(actualResult);
        verify(flowMessageService, times(0))
                .fireEventAndLog(eq(STACK_ID), eq(Status.UPDATE_IN_PROGRESS.name()), eq(CLUSTER_MANAGER_UPGRADE_FINISHED), anyString());
        verify(flowMessageService).fireEventAndLog(STACK_ID, Status.UPDATE_IN_PROGRESS.name(), CLUSTER_UPGRADE);
    }

    @ParameterizedTest
    @MethodSource("upgradeNeededVersions")
    public void testClusterAndCMUpgradeNeeded(String currentStackVersion, String targetStackVersion) {
        // GIVEN
        Image currentImage = createImage(currentStackVersion, currentStackVersion);
        Image targetImage = createImage(targetStackVersion, targetStackVersion);
        // WHEN
        boolean actualResult = underTest.upgradeCluster(STACK_ID, currentImage, targetImage);
        // THEN
        Assertions.assertTrue(actualResult);
        verify(flowMessageService).fireEventAndLog(STACK_ID, Status.UPDATE_IN_PROGRESS.name(), CLUSTER_MANAGER_UPGRADE_FINISHED, V_7_0_2);
        verify(flowMessageService).fireEventAndLog(STACK_ID, Status.UPDATE_IN_PROGRESS.name(), CLUSTER_UPGRADE);
    }

    @ParameterizedTest
    @MethodSource("upgradeNeededVersions")
    public void testClusterUpgradeNotNeededButCMNeeded(String currentStackVersion, String targetStackVersion) {
        // GIVEN
        Image currentImage = createImage(currentStackVersion, CURRENT_BUILD_NUMBER);
        Image targetImage = createImage(targetStackVersion, CURRENT_BUILD_NUMBER);
        // WHEN
        boolean actualResult = underTest.upgradeCluster(STACK_ID, currentImage, targetImage);
        // THEN
        Assertions.assertFalse(actualResult);
        verify(flowMessageService).fireEventAndLog(STACK_ID, Status.UPDATE_IN_PROGRESS.name(), CLUSTER_MANAGER_UPGRADE_FINISHED, V_7_0_2);
        verify(flowMessageService).fireEventAndLog(STACK_ID, Status.UPDATE_IN_PROGRESS.name(), CLUSTER_UPGRADE_NOT_NEEDED, CURRENT_BUILD_NUMBER);
    }

    @Test
    public void testNeitherClusterNorCMUpgradeNeeded() {
        // GIVEN
        Image currentImage = createImage(CURRENT_BUILD_NUMBER, CURRENT_BUILD_NUMBER);
        Image targetImage = createImage(CURRENT_BUILD_NUMBER, CURRENT_BUILD_NUMBER);
        // WHEN
        boolean actualResult = underTest.upgradeCluster(STACK_ID, currentImage, targetImage);
        // THEN
        Assertions.assertFalse(actualResult);
        verify(flowMessageService, times(0))
                .fireEventAndLog(eq(STACK_ID), eq(Status.UPDATE_IN_PROGRESS.name()), eq(CLUSTER_MANAGER_UPGRADE_FINISHED), anyString());
        verify(flowMessageService).fireEventAndLog(STACK_ID, Status.UPDATE_IN_PROGRESS.name(), CLUSTER_UPGRADE_NOT_NEEDED, CURRENT_BUILD_NUMBER);
    }

    @ParameterizedTest
    @MethodSource("upgradeNeededVersions")
    public void testClusterUpgradeFinishedWhenNeeded(String currentStackVersion, String targetStackVersion) {
        // GIVEN
        Image currentImmage = createImage(CURRENT_BUILD_NUMBER, currentStackVersion);
        StatedImage currentImage = StatedImage.statedImage(currentImmage, null, null);
        Image targetImmage = createImage(CURRENT_BUILD_NUMBER, targetStackVersion);
        StatedImage targetImage = StatedImage.statedImage(targetImmage, null, null);
        // WHEN
        underTest.clusterUpgradeFinished(STACK_ID, currentImage, targetImage);
        // THEN
        verify(flowMessageService).fireEventAndLog(STACK_ID, Status.AVAILABLE.name(), CLUSTER_UPGRADE_FINISHED, V_7_0_2);
    }

    @ParameterizedTest
    @MethodSource("upgradeNeededImages")
    public void testClusterUpgradeFinishedWhenNeededNoStackDetails(Image currentIm, Image targetIm) {
        // GIVEN
        StatedImage currentImage = StatedImage.statedImage(currentIm, null, null);
        StatedImage targetImage = StatedImage.statedImage(targetIm, null, null);
        // WHEN
        underTest.clusterUpgradeFinished(STACK_ID, currentImage, targetImage);
        // THEN
        verify(flowMessageService).fireEventAndLog(STACK_ID, Status.AVAILABLE.name(), CLUSTER_UPGRADE_FINISHED,
                NullUtil.getIfNotNull(targetIm.getStackDetails(), StackDetails::getVersion));
    }

    @Test
    public void testClusterUpgradeFinishedWhenNotNeeded() {
        // GIVEN
        Image currentImmage = createImage(CURRENT_BUILD_NUMBER, CURRENT_BUILD_NUMBER);
        StatedImage currentImage = StatedImage.statedImage(currentImmage, null, null);
        Image targetImmage = createImage(CURRENT_BUILD_NUMBER, CURRENT_BUILD_NUMBER);
        StatedImage targetImage = StatedImage.statedImage(targetImmage, null, null);
        // WHEN
        underTest.clusterUpgradeFinished(STACK_ID, currentImage, targetImage);
        // THEN
        verify(flowMessageService).fireEventAndLog(STACK_ID, Status.AVAILABLE.name(), CLUSTER_UPGRADE_FINISHED_NOVERSION);
    }

    @Test
    public void testClusterManagerUpgradeFailure() {
        ArgumentCaptor<ResourceEvent> captor = ArgumentCaptor.forClass(ResourceEvent.class);
        underTest.handleUpgradeClusterFailure(STACK_ID, ERROR_MESSAGE, DetailedStackStatus.CLUSTER_MANAGER_UPGRADE_FAILED);
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(Status.UPDATE_FAILED.name()), captor.capture(), eq(ERROR_MESSAGE));
        assertEquals(CLUSTER_MANAGER_UPGRADE_FAILED, captor.getValue());
    }

    @Test
    public void testClusterUpgradeFailure() {
        ArgumentCaptor<ResourceEvent> captor = ArgumentCaptor.forClass(ResourceEvent.class);
        underTest.handleUpgradeClusterFailure(STACK_ID, ERROR_MESSAGE, DetailedStackStatus.CLUSTER_UPGRADE_FAILED);
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(Status.UPDATE_FAILED.name()), captor.capture(), eq(ERROR_MESSAGE));
        assertEquals(CLUSTER_UPGRADE_FAILED, captor.getValue());
    }

    private static Image createImage(String cmBuildNumber, String stackBuildNumber) {
        return new Image(null, null, null, OS, CURRENT_IMAGE_ID, V_7_0_2, null,
                Map.of(CLOUD_PLATFORM, Collections.emptyMap()), new StackDetails(V_7_0_2, null, stackBuildNumber), OS_TYPE,
                createPackageVersions(V_7_0_2, V_7_0_2, CMF_VERSION, CSP_VERSION, SALT_VERSION),
                null, null, cmBuildNumber, true, null, null);
    }

    private static Image createImage(String cmBuildNumber) {
        return new Image(null, null, null, OS, CURRENT_IMAGE_ID, V_7_0_2, null,
                Map.of(CLOUD_PLATFORM, Collections.emptyMap()), null, OS_TYPE,
                createPackageVersions(V_7_0_2, V_7_0_2, CMF_VERSION, CSP_VERSION, SALT_VERSION),
                null, null, cmBuildNumber, true, null, null);
    }

    private static Map<String, String> createPackageVersions(String cmVersion, String cdhVersion, String cfmVersion, String cspVersion, String saltVersion) {
        return Map.of(
                "cm", cmVersion,
                "stack", cdhVersion,
                "cfm", cfmVersion,
                "csp", cspVersion,
                "salt", saltVersion);
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
                Arguments.of(createImage(CURRENT_BUILD_NUMBER), createImage(CURRENT_BUILD_NUMBER)),
                Arguments.of(createImage(CURRENT_BUILD_NUMBER, CURRENT_BUILD_NUMBER), createImage(CURRENT_BUILD_NUMBER)),
                Arguments.of(createImage(CURRENT_BUILD_NUMBER), createImage(CURRENT_BUILD_NUMBER, CURRENT_BUILD_NUMBER))
        );
    }
}

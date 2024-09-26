package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationHandlerSelectors.DOWNLOAD_CM_PACKAGES_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationStateSelectors.START_CLUSTER_UPGRADE_PARCEL_DOWNLOAD_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradePreparationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradePreparationFailureEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.upgrade.preparation.ClusterUpgradeCmPackageDownloaderService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class ClusterUpgradeCmPackageDownloadHandlerTest {

    private static final Long STACK_ID = 1L;

    private static final String IMAGE_ID = "image-id";

    @InjectMocks
    private ClusterUpgradeCmPackageDownloadHandler underTest;

    @Mock
    private ClusterUpgradeCmPackageDownloaderService clusterUpgradeCmPackageDownloaderService;

    @Test
    void testDoAcceptSuccess() throws Exception {
        ClusterUpgradePreparationEvent request = new ClusterUpgradePreparationEvent(DOWNLOAD_CM_PACKAGES_EVENT.selector(), STACK_ID, null, IMAGE_ID);
        HandlerEvent<ClusterUpgradePreparationEvent> event = new HandlerEvent<>(new Event<>(request));

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(ClusterUpgradePreparationEvent.class, result);
        assertEquals(START_CLUSTER_UPGRADE_PARCEL_DOWNLOAD_EVENT.name(), result.getSelector());
        verify(clusterUpgradeCmPackageDownloaderService).downloadCmPackages(STACK_ID, IMAGE_ID);
    }

    @Test
    void testDoAcceptFailure() throws Exception {
        ClusterUpgradePreparationEvent request = new ClusterUpgradePreparationEvent(DOWNLOAD_CM_PACKAGES_EVENT.selector(), STACK_ID, null, IMAGE_ID);
        HandlerEvent<ClusterUpgradePreparationEvent> event = new HandlerEvent<>(new Event<>(request));

        doThrow(new RuntimeException("error")).when(clusterUpgradeCmPackageDownloaderService).downloadCmPackages(STACK_ID, IMAGE_ID);

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(ClusterUpgradePreparationFailureEvent.class, result);
        verify(clusterUpgradeCmPackageDownloaderService).downloadCmPackages(STACK_ID, IMAGE_ID);
    }

}
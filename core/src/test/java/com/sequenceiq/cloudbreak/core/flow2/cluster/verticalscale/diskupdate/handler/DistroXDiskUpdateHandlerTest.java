package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateStateSelectors.DATAHUB_DISK_UPDATE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateStateSelectors.DATAHUB_DISK_UPDATE_FINISH_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateStateSelectors.FAILED_DATAHUB_DISK_UPDATE_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event.DistroXDiskUpdateEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.diskupdate.DiskUpdateService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class DistroXDiskUpdateHandlerTest {

    private static final String TEST_CLUSTER = "TEST_CLUSTER";

    private static final String ACCOUNT_ID = "ACCOUNT_ID";

    @Mock
    private DiskUpdateService diskUpdateService;

    @InjectMocks
    private DistroXDiskUpdateHandler underTest;

    @Test
    void testDiskUpdateAction() throws Exception {
        String selector = DATAHUB_DISK_UPDATE_EVENT.event();
        DiskUpdateRequest diskUpdateRequest = new DiskUpdateRequest();
        diskUpdateRequest.setGroup("compute");
        diskUpdateRequest.setSize(100);
        diskUpdateRequest.setVolumeType("gp2");
        DistroXDiskUpdateEvent event = DistroXDiskUpdateEvent.builder()
                .withClusterName(TEST_CLUSTER)
                .withAccountId(ACCOUNT_ID)
                .withGroup("compute")
                .withSize(100)
                .withVolumeType("gp2")
                .withSelector(selector)
                .withVolumesToBeUpdated(List.of(mock(Volume.class)))
                .withCloudPlatform("AWS")
                .withStackId(1L)
                .build();
        Selectable selectable = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));
        verify(diskUpdateService, times(1)).updateDiskTypeAndSize(
                eq(event.getGroup()),
                eq(event.getVolumeType()),
                eq(event.getSize()),
                eq(event.getVolumesToBeUpdated()),
                eq(1L)
        );
        assertEquals(DATAHUB_DISK_UPDATE_FINISH_EVENT.selector(), selectable.getSelector());
    }

    @Test
    void testDiskUpdateFailureAction() throws Exception {
        String selector = DATAHUB_DISK_UPDATE_EVENT.event();
        DistroXDiskUpdateEvent event = DistroXDiskUpdateEvent.builder()
                .withClusterName(TEST_CLUSTER)
                .withAccountId(ACCOUNT_ID)
                .withGroup("compute")
                .withVolumeType("gp2")
                .withSize(100)
                .withSelector(selector)
                .withVolumesToBeUpdated(List.of(mock(Volume.class)))
                .withCloudPlatform("AWS")
                .build();
        doThrow(new CloudbreakException("Test")).when(diskUpdateService).updateDiskTypeAndSize(
                eq(event.getGroup()),
                eq(event.getVolumeType()),
                eq(event.getSize()),
                eq(event.getVolumesToBeUpdated()),
                eq(1L));
        Selectable selectable = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));
        assertEquals(FAILED_DATAHUB_DISK_UPDATE_EVENT.selector(), selectable.getSelector());
    }
}
package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateHandlerSelectors.DATAHUB_DISK_UPDATE_VALIDATION_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateStateSelectors.DATAHUB_DISK_UPDATE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateStateSelectors.FAILED_DATAHUB_DISK_UPDATE_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event.DistroXDiskUpdateEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.diskupdate.DiskUpdateService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class DistroXDiskUpdateValidationHandlerTest {

    private static final Long STACK_ID = 1L;

    private static final String TEST_CLUSTER = "TEST_CLUSTER";

    private static final String ACCOUNT_ID = "ACCOUNT_ID";

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private DiskUpdateService diskUpdateService;

    @InjectMocks
    private DistroXDiskUpdateValidationHandler underTest;

    private Volume volume(String id, CloudVolumeUsageType usageType) {
        Volume volume = new Volume("", VolumeParameterType.ST1.name().toLowerCase(Locale.ROOT), 50, usageType);
        volume.setId(id);
        return volume;
    }

    @Test
    void testDiskUpdateValidationAction() {
        String selector = DATAHUB_DISK_UPDATE_VALIDATION_HANDLER_EVENT.event();
        DistroXDiskUpdateEvent event = DistroXDiskUpdateEvent.builder()
                .withClusterName(TEST_CLUSTER)
                .withAccountId(ACCOUNT_ID)
                .withSize(100)
                .withVolumeType("gp2")
                .withGroup("compute")
                .withSelector(selector)
                .withStackId(STACK_ID)
                .withResourceId(STACK_ID)
                .build();
        StackDto stackDto = mock(StackDto.class);
        doReturn(CloudPlatform.AWS.toString()).when(stackDto).getCloudPlatform();
        when(diskUpdateService.getVolumesToUpdate(eq(stackDto), eq("compute"), anyInt(), any(), any()))
                .thenReturn(List.of(volume("vol-1", CloudVolumeUsageType.GENERAL)));
        doReturn(true).when(diskUpdateService).isDiskTypeChangeSupported(CloudPlatform.AWS.toString());
        doReturn(stackDto).when(stackDtoService).getById(anyLong());

        Selectable selectable = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        DistroXDiskUpdateEvent eventCaptured = (DistroXDiskUpdateEvent) selectable;
        assertEquals(DATAHUB_DISK_UPDATE_EVENT.selector(), eventCaptured.getSelector());
        assertEquals(1, eventCaptured.getVolumesToBeUpdated().size());
        assertEquals("vol-1", eventCaptured.getVolumesToBeUpdated().get(0).getId());
    }

    @Test
    void testFailureDiskUpdateValidationAction() {
        String selector = DATAHUB_DISK_UPDATE_VALIDATION_HANDLER_EVENT.event();
        DistroXDiskUpdateEvent event = DistroXDiskUpdateEvent.builder()
                .withClusterName(TEST_CLUSTER)
                .withAccountId(ACCOUNT_ID)
                .withGroup("compute")
                .withVolumeType("gp2")
                .withSize(100)
                .withSelector(selector)
                .withStackId(STACK_ID)
                .withResourceId(STACK_ID)
                .build();
        StackDto stackDto = mock(StackDto.class);
        when(diskUpdateService.getVolumesToUpdate(eq(stackDto), eq("compute"), anyInt(), any(), any()))
                .thenReturn(List.of());
        doReturn(true).when(diskUpdateService).isDiskTypeChangeSupported(any());
        doReturn(stackDto).when(stackDtoService).getById(anyLong());
        Selectable selectable = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));
        assertEquals(FAILED_DATAHUB_DISK_UPDATE_EVENT.selector(), selectable.getSelector());
    }

    @Test
    void testDiskUpdateValidationActionWithDatabaseDiskType() {
        String selector = DATAHUB_DISK_UPDATE_VALIDATION_HANDLER_EVENT.event();
        DistroXDiskUpdateEvent event = DistroXDiskUpdateEvent.builder()
                .withClusterName(TEST_CLUSTER)
                .withAccountId(ACCOUNT_ID)
                .withSize(100)
                .withVolumeType("gp2")
                .withGroup("compute")
                .withDiskType(DiskType.DATABASE_DISK.name())
                .withSelector(selector)
                .withStackId(STACK_ID)
                .withResourceId(STACK_ID)
                .build();
        StackDto stackDto = mock(StackDto.class);
        doReturn(CloudPlatform.AWS.toString()).when(stackDto).getCloudPlatform();
        when(diskUpdateService.getVolumesToUpdate(eq(stackDto), eq("compute"), anyInt(), any(), eq(DiskType.DATABASE_DISK.name())))
                .thenReturn(List.of(volume("db-vol-1", CloudVolumeUsageType.DATABASE)));
        doReturn(true).when(diskUpdateService).isDiskTypeChangeSupported(CloudPlatform.AWS.toString());
        doReturn(stackDto).when(stackDtoService).getById(anyLong());

        Selectable selectable = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        DistroXDiskUpdateEvent eventCaptured = (DistroXDiskUpdateEvent) selectable;
        assertEquals(DATAHUB_DISK_UPDATE_EVENT.selector(), eventCaptured.getSelector());
        assertEquals(1, eventCaptured.getVolumesToBeUpdated().size());
        assertEquals("db-vol-1", eventCaptured.getVolumesToBeUpdated().get(0).getId());
        assertEquals(CloudVolumeUsageType.DATABASE, eventCaptured.getVolumesToBeUpdated().get(0).getVolumeUsageType());
    }

}

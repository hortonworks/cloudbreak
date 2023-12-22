package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateHandlerSelectors.DATAHUB_DISK_UPDATE_VALIDATION_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateStateSelectors.DATAHUB_DISK_UPDATE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateStateSelectors.FAILED_DATAHUB_DISK_UPDATE_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event.DistroXDiskUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event.DistroXDiskUpdateFailedEvent;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.datalake.DiskUpdateService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.reactor.api.event.EventSender;

@ExtendWith(MockitoExtension.class)
class DistroXDiskUpdateValidationHandlerTest {

    private static final Long STACK_ID = 1L;

    private static final String TEST_CLUSTER = "TEST_CLUSTER";

    private static final String ACCOUNT_ID = "ACCOUNT_ID";

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private DiskUpdateService diskUpdateService;

    private DistroXDiskUpdateValidationHandler underTest;

    @Mock
    private EventSender eventSender;

    @Captor
    private ArgumentCaptor<DistroXDiskUpdateFailedEvent> failureCaptor;

    private Json json;

    private Json json2;

    @BeforeEach
    void setUp() {
        underTest = new DistroXDiskUpdateValidationHandler(eventSender);
        ReflectionTestUtils.setField(underTest, null, stackDtoService, StackDtoService.class);
        ReflectionTestUtils.setField(underTest, null, diskUpdateService, DiskUpdateService.class);
        VolumeSetAttributes.Volume volume = new VolumeSetAttributes.Volume("vol-1", "/dev/xvdb", 50, "standard",
                CloudVolumeUsageType.GENERAL);
        VolumeSetAttributes.Volume volume2 = new VolumeSetAttributes.Volume("vol-2", "/dev/xvdb", 50, "standard",
                CloudVolumeUsageType.GENERAL);
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes("us-west-2a", true, "", List.of(volume),
                512, "standard");
        VolumeSetAttributes volumeSetAttributes2 = new VolumeSetAttributes("us-west-2a", true, "", List.of(volume2),
                512, "standard");
        json = new Json(volumeSetAttributes);
        json2 = new Json(volumeSetAttributes2);
    }

    @Test
    void testDiskUpdateValidationAction() {
        String selector = DATAHUB_DISK_UPDATE_VALIDATION_HANDLER_EVENT.event();
        DiskUpdateRequest diskUpdateRequest = new DiskUpdateRequest();
        diskUpdateRequest.setGroup("compute");
        diskUpdateRequest.setSize(100);
        diskUpdateRequest.setVolumeType("gp2");
        DistroXDiskUpdateEvent event = DistroXDiskUpdateEvent.builder()
                .withClusterName(TEST_CLUSTER)
                .withAccountId(ACCOUNT_ID)
                .withDiskUpdateRequest(diskUpdateRequest)
                .withSelector(selector)
                .withStackId(STACK_ID)
                .withResourceId(STACK_ID)
                .build();
        StackDto stack = mock(StackDto.class);
        Resource resource = mock(Resource.class);
        doReturn("A1234").when(resource).getInstanceId();
        doReturn("compute").when(resource).getInstanceGroup();
        doReturn(json).when(resource).getAttributes();
        doReturn(ResourceType.AWS_VOLUMESET).when(resource).getResourceType();

        Resource resource2 = mock(Resource.class);
        lenient().when(resource2.getInstanceId()).thenReturn("B1234");
        lenient().when(resource2.getResourceType()).thenReturn(ResourceType.AWS_VOLUMESET);
        lenient().when(resource2.getAttributes()).thenReturn(json2);

        doReturn(Set.of(resource)).when(stack).getResources();

        doReturn(CloudPlatform.AWS.toString()).when(stack).getCloudPlatform();
        doReturn(true).when(diskUpdateService).isDiskTypeChangeSupported(CloudPlatform.AWS.toString());
        doReturn(stack).when(stackDtoService).getById(anyLong());
        ArgumentCaptor<DistroXDiskUpdateEvent> captor = ArgumentCaptor.forClass(DistroXDiskUpdateEvent.class);
        underTest.accept(new Event<>(event));
        verify(eventSender, times(1)).sendEvent(captor.capture(), any());
        DistroXDiskUpdateEvent eventCaptured = captor.getValue();
        assertEquals(DATAHUB_DISK_UPDATE_EVENT.selector(), captor.getValue().getSelector());
        assertEquals(1, eventCaptured.getVolumesToBeUpdated().size());
        assertEquals("vol-1", eventCaptured.getVolumesToBeUpdated().get(0).getId());
    }

    @Test
    void testFailureDiskUpdateValidationAction() {
        String selector = DATAHUB_DISK_UPDATE_VALIDATION_HANDLER_EVENT.event();
        DiskUpdateRequest diskUpdateRequest = new DiskUpdateRequest();
        diskUpdateRequest.setGroup("compute");
        diskUpdateRequest.setSize(100);
        diskUpdateRequest.setVolumeType("gp2");
        DistroXDiskUpdateEvent event = DistroXDiskUpdateEvent.builder()
                .withClusterName(TEST_CLUSTER)
                .withAccountId(ACCOUNT_ID)
                .withDiskUpdateRequest(diskUpdateRequest)
                .withSelector(selector)
                .withStackId(STACK_ID)
                .withResourceId(STACK_ID)
                .build();
        StackDto stack = mock(StackDto.class);
        Resource resource = mock(Resource.class);
        doReturn("A1234").when(resource).getInstanceId();
        doReturn("compute").when(resource).getInstanceGroup();
        doReturn(Set.of(resource)).when(stack).getResources();
        doReturn(stack).when(stackDtoService).getById(anyLong());
        underTest.accept(new Event<>(event));
        verify(eventSender, times(1)).sendEvent(failureCaptor.capture(), any());
        assertEquals(FAILED_DATAHUB_DISK_UPDATE_EVENT.selector(), failureCaptor.getValue().getSelector());
    }
}

package com.sequenceiq.datalake.flow.datalake.verticalscale.diskupdate.handler;

import static com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateStateSelectors.DATALAKE_DISK_UPDATE_VALIDATION_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.resource.ResourceV4Response;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateEvent;
import com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateFailedEvent;
import com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateStateSelectors;
import com.sequenceiq.datalake.flow.verticalscale.diskupdate.handler.DatalakeDiskUpdateValidationHandler;
import com.sequenceiq.datalake.service.sdx.VerticalScaleService;
import com.sequenceiq.flow.reactor.api.event.EventSender;

@ExtendWith(MockitoExtension.class)
public class DatalakeDiskUpdateValidationHandlerTest {

    private static final Long STACK_ID = 1L;

    private static final String TEST_CLUSTER = "TEST_CLUSTER";

    private static final String ACCOUNT_ID = "ACCOUNT_ID";

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private VerticalScaleService verticalScaleService;

    private DatalakeDiskUpdateValidationHandler underTest;

    @Mock
    private EventSender eventSender;

    private Json json;

    private Json json2;

    @BeforeEach
    void setUp() {
        underTest = new DatalakeDiskUpdateValidationHandler(eventSender);
        ReflectionTestUtils.setField(underTest, null, stackV4Endpoint, StackV4Endpoint.class);
        ReflectionTestUtils.setField(underTest, null, verticalScaleService, VerticalScaleService.class);
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
    public void testDiskUpdateValidationAction() {
        String selector = DATALAKE_DISK_UPDATE_VALIDATION_EVENT.event();
        DiskUpdateRequest diskUpdateRequest = new DiskUpdateRequest();
        diskUpdateRequest.setGroup("compute");
        diskUpdateRequest.setSize(100);
        diskUpdateRequest.setVolumeType("gp2");
        DatalakeDiskUpdateEvent event = DatalakeDiskUpdateEvent.builder()
                .withAccepted(new Promise<>())
                .withClusterName(TEST_CLUSTER)
                .withAccountId(ACCOUNT_ID)
                .withDatalakeDiskUpdateRequest(diskUpdateRequest)
                .withSelector(selector)
                .withStackId(STACK_ID)
                .build();
        StackV4Response stackV4Response = mock(StackV4Response.class);

        ResourceV4Response resourceV4Response1 = mock(ResourceV4Response.class);
        doReturn("A1234").when(resourceV4Response1).getInstanceId();
        doReturn("compute").when(resourceV4Response1).getInstanceGroup();
        doReturn(ResourceType.AWS_VOLUMESET).when(resourceV4Response1).getResourceType();

        ResourceV4Response resourceV4Response2 = mock(ResourceV4Response.class);
        lenient().when(resourceV4Response2.getInstanceId()).thenReturn("B1234");
        lenient().when(resourceV4Response2.getResourceType()).thenReturn(ResourceType.AWS_VOLUMESET);

        doReturn(List.of(resourceV4Response1, resourceV4Response2)).when(stackV4Response).getResources();
        doReturn(CloudPlatform.AWS).when(stackV4Response).getCloudPlatform();
        doReturn(json.getValue()).when(resourceV4Response1).getAttributes();
        lenient().when(resourceV4Response2.getAttributes()).thenReturn(json2.getValue());
        doReturn(true).when(verticalScaleService).getDiskTypeChangeSupported(CloudPlatform.AWS.toString());
        doReturn(stackV4Response).when(stackV4Endpoint).getWithResources(anyLong(), anyString(), anySet(), anyString());
        underTest.accept(new Event<>(event));
        ArgumentCaptor<DatalakeDiskUpdateEvent> captor = ArgumentCaptor.forClass(DatalakeDiskUpdateEvent.class);
        verify(eventSender, times(1)).sendEvent(captor.capture(), any());
        DatalakeDiskUpdateEvent eventCaptured = captor.getValue();
        assertEquals(DatalakeDiskUpdateStateSelectors.DATALAKE_DISK_UPDATE_EVENT.selector(), eventCaptured.getSelector());
        assertEquals(1, eventCaptured.getVolumesToBeUpdated().size());
        assertEquals("vol-1", eventCaptured.getVolumesToBeUpdated().get(0).getId());
    }

    @Test
    public void testFailureDiskUpdateValidationAction() {
        String selector = DATALAKE_DISK_UPDATE_VALIDATION_EVENT.event();
        DiskUpdateRequest diskUpdateRequest = new DiskUpdateRequest();
        diskUpdateRequest.setGroup("compute");
        diskUpdateRequest.setSize(100);
        diskUpdateRequest.setVolumeType("gp2");
        DatalakeDiskUpdateEvent event = DatalakeDiskUpdateEvent.builder()
                .withAccepted(new Promise<>())
                .withClusterName(TEST_CLUSTER)
                .withAccountId(ACCOUNT_ID)
                .withDatalakeDiskUpdateRequest(diskUpdateRequest)
                .withSelector(selector)
                .withStackId(STACK_ID)
                .build();
        StackV4Response stackV4Response = mock(StackV4Response.class);
        ResourceV4Response resourceV4Response = mock(ResourceV4Response.class);
        doReturn("A1234").when(resourceV4Response).getInstanceId();
        doReturn("compute").when(resourceV4Response).getInstanceGroup();
        doReturn(List.of(resourceV4Response)).when(stackV4Response).getResources();
        doReturn(stackV4Response).when(stackV4Endpoint).getWithResources(anyLong(), anyString(), anySet(), anyString());
        ArgumentCaptor<DatalakeDiskUpdateFailedEvent> captor = ArgumentCaptor.forClass(DatalakeDiskUpdateFailedEvent.class);
        underTest.accept(new Event<>(event));
        verify(eventSender, times(1)).sendEvent(captor.capture(), any());
        assertEquals(DatalakeDiskUpdateStateSelectors.FAILED_DATALAKE_DISK_UPDATE_EVENT.selector(), captor.getValue().getSelector());
    }
}

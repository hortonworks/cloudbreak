package com.sequenceiq.datalake.flow.datalake.verticalscale.diskupdate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.flow.verticalscale.diskupdate.AbstractDatalakeDiskUpdateAction;
import com.sequenceiq.datalake.flow.verticalscale.diskupdate.DatalakeDiskUpdateActions;
import com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateEvent;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(MockitoExtension.class)
public class DatalakeDiskUpdateActionsTest {

    private Map<Object, Object> variables = new HashMap<>();

    @Mock
    private SdxStatusService sdxStatusService;

    @InjectMocks
    @Spy
    private DatalakeDiskUpdateActions underTest;

    @Captor
    private ArgumentCaptor<DatalakeStatusEnum> captor;

    @Captor
    private ArgumentCaptor<Event<DatalakeDiskUpdateEvent>> eventCaptor;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    private CommonContext context;

    @Mock
    private FlowParameters flowParameters;

    private Json json;

    @BeforeEach
    void setUp() {
        context = new CommonContext(flowParameters);
        VolumeSetAttributes.Volume volume = new VolumeSetAttributes.Volume("vol-07d2212c81d1b8b00", "/dev/xvdb", 50, "standard",
                CloudVolumeUsageType.GENERAL);
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes("us-west-2a", true, "", List.of(volume),
                512, "standard");
        json = new Json(volumeSetAttributes);
    }

    @Test
    public void testDiskUpdateValidationAction() throws Exception {
        DatalakeDiskUpdateEvent event = mock(DatalakeDiskUpdateEvent.class);
        doReturn(1L).when(event).getResourceId();
        DiskUpdateRequest request = mock(DiskUpdateRequest.class);
        doReturn(request).when(event).getDatalakeDiskUpdateRequest();
        doReturn("test").when(request).getGroup();
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), eq(event));
        AbstractDatalakeDiskUpdateAction<DatalakeDiskUpdateEvent> action =
                (AbstractDatalakeDiskUpdateAction<DatalakeDiskUpdateEvent>) underTest.diskUpdateValidationAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);
        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotifyWithStatusReason(captor.capture(), anyString(), any());
        assertEquals(DatalakeStatusEnum.DATALAKE_DISK_UPDATE_VALIDATION_IN_PROGRESS, captor.getValue());
        verify(eventBus, times(1)).notify(anyString(), eventCaptor.capture());
        assertEquals(event, eventCaptor.getValue().getData());
    }

    @Test
    public void testDiskUpdateAction() throws Exception {
        DatalakeDiskUpdateEvent event = mock(DatalakeDiskUpdateEvent.class);
        doReturn(1L).when(event).getResourceId();
        DiskUpdateRequest request = mock(DiskUpdateRequest.class);
        doReturn(request).when(event).getDatalakeDiskUpdateRequest();
        doReturn("test").when(request).getGroup();
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), eq(event));
        AbstractDatalakeDiskUpdateAction<DatalakeDiskUpdateEvent> action =
                (AbstractDatalakeDiskUpdateAction<DatalakeDiskUpdateEvent>) underTest.diskUpdateInDatalakeAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);
        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotifyWithStatusReason(captor.capture(), anyString(), any());
        assertEquals(DatalakeStatusEnum.DATALAKE_DISK_UPDATE_IN_PROGRESS, captor.getValue());
        verify(eventBus, times(1)).notify(anyString(), eventCaptor.capture());
        assertEquals(event, eventCaptor.getValue().getData());
    }

    @Test
    public void testFinishedAction() throws Exception {
        DatalakeDiskUpdateEvent event = mock(DatalakeDiskUpdateEvent.class);
        doReturn(1L).when(event).getResourceId();
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), eq(event));
        AbstractDatalakeDiskUpdateAction<DatalakeDiskUpdateEvent> action =
                (AbstractDatalakeDiskUpdateAction<DatalakeDiskUpdateEvent>) underTest.finishedAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);
        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotifyWithStatusReason(captor.capture(), anyString(), any());
        assertEquals(DatalakeStatusEnum.RUNNING, captor.getValue());
        verify(eventBus, times(1)).notify(anyString(), eventCaptor.capture());
        assertEquals(event, eventCaptor.getValue().getData());
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }
}

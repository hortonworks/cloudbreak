package com.sequenceiq.datalake.flow.datalake.verticalscale.diskupdate.handler;

import static com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateStateSelectors.DATALAKE_DISK_UPDATE_VALIDATION_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateEvent;
import com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateStateSelectors;
import com.sequenceiq.datalake.flow.verticalscale.diskupdate.handler.DatalakeDiskUpdateValidationHandler;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;
import com.sequenceiq.flow.reactor.api.event.EventSender;

@ExtendWith(MockitoExtension.class)
public class DatalakeDiskUpdateValidationHandlerTest {

    private static final Long STACK_ID = 1L;

    private static final String TEST_CLUSTER = "TEST_CLUSTER";

    private static final String ACCOUNT_ID = "ACCOUNT_ID";

    private static final String CLOUD_PLATFORM = "AWS";

    @Mock
    private EventSender eventSender;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    private DatalakeDiskUpdateValidationHandler underTest;

    @BeforeEach
    void setUp() {
        underTest = new DatalakeDiskUpdateValidationHandler(eventSender);
        ReflectionTestUtils.setField(underTest, null, stackV4Endpoint, StackV4Endpoint.class);
    }

    private DatalakeDiskUpdateEvent triggerEvent() {
        DiskUpdateRequest diskUpdateRequest = new DiskUpdateRequest();
        diskUpdateRequest.setGroup("compute");
        diskUpdateRequest.setSize(100);
        diskUpdateRequest.setVolumeType("gp2");
        return DatalakeDiskUpdateEvent.builder()
                .withAccepted(new Promise<>())
                .withClusterName(TEST_CLUSTER)
                .withAccountId(ACCOUNT_ID)
                .withDatalakeDiskUpdateRequest(diskUpdateRequest)
                .withSelector(DATALAKE_DISK_UPDATE_VALIDATION_EVENT.event())
                .withStackId(STACK_ID)
                .withCloudPlatform(CLOUD_PLATFORM)
                .build();
    }

    @Test
    public void testHandlerForwardsRequestUnchangedAsDiskUpdateEvent() {
        DatalakeDiskUpdateEvent event = triggerEvent();
        StackV4Response stackV4Response = mock(StackV4Response.class);
        doReturn(CloudPlatform.AWS).when(stackV4Response).getCloudPlatform();
        doReturn(STACK_ID).when(stackV4Response).getId();
        doReturn(stackV4Response).when(stackV4Endpoint).getWithResources(anyLong(), anyString(), anySet(), anyString());
        underTest.accept(new Event<>(event));

        ArgumentCaptor<DatalakeDiskUpdateEvent> captor = ArgumentCaptor.forClass(DatalakeDiskUpdateEvent.class);
        verify(eventSender, times(1)).sendEvent(captor.capture(), any());
        DatalakeDiskUpdateEvent forwarded = captor.getValue();
        // Validation now runs synchronously in the core API path; this handler is a thin pass-through that only forwards.
        assertEquals(DatalakeDiskUpdateStateSelectors.DATALAKE_DISK_UPDATE_EVENT.selector(), forwarded.getSelector());
        assertEquals(STACK_ID, forwarded.getStackId());
        assertEquals(CLOUD_PLATFORM, forwarded.getCloudPlatform());
        assertSame(event.getDatalakeDiskUpdateRequest(), forwarded.getDatalakeDiskUpdateRequest());
        // volumesToUpdate is @NotNull on DiskModificationRequest and recomputed from the stack in core; forward an empty, never-null list.
        assertNotNull(forwarded.getVolumesToBeUpdated());
        assertTrue(forwarded.getVolumesToBeUpdated().isEmpty());
    }

    @Test
    public void testHandlerSendsFailedEventWhenForwardingThrows() {
        DatalakeDiskUpdateEvent event = triggerEvent();
        doThrow(new RuntimeException("bus down")).doReturn(null).when(eventSender).sendEvent(any(), any());
        StackV4Response stackV4Response = mock(StackV4Response.class);
        doReturn(CloudPlatform.AWS).when(stackV4Response).getCloudPlatform();
        doReturn(stackV4Response).when(stackV4Endpoint).getWithResources(anyLong(), anyString(), anySet(), anyString());
        underTest.accept(new Event<>(event));

        // Both the pass-through event and the failed event are BaseNamedFlowEvent, so both go through the same
        // sendEvent overload; the second captured value is the failed event.
        ArgumentCaptor<BaseNamedFlowEvent> captor = ArgumentCaptor.forClass(BaseNamedFlowEvent.class);
        verify(eventSender, times(2)).sendEvent(captor.capture(), any());
        assertEquals(DatalakeDiskUpdateStateSelectors.FAILED_DATALAKE_DISK_UPDATE_EVENT.selector(),
                ((Selectable) captor.getAllValues().get(1)).getSelector());
    }
}

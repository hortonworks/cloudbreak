package com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.ADD_VOLUMES_VALIDATE_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.ADD_VOLUMES_VALIDATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.FAILURE_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesValidateEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesValidationFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.service.AddVolumesService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;

@ExtendWith(MockitoExtension.class)
class AddVolumesValidateHandlerTest {

    @Mock
    private EventBus eventBus;

    @Mock
    private AddVolumesService addVolumesService;

    @InjectMocks
    private AddVolumesValidateHandler underTest;

    @Captor
    private ArgumentCaptor<Event<AddVolumesValidationFinishedEvent>> successEventCaptor;

    @Captor
    private ArgumentCaptor<Event<AddVolumesFailedEvent>> failureEventCaptor;

    @Test
    void testSelector() {
        assertEquals(ADD_VOLUMES_VALIDATE_HANDLER_EVENT.event(), underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Exception e = mock(Exception.class);

        AddVolumesFailedEvent result = (AddVolumesFailedEvent) underTest.defaultFailureEvent(1L, e, null);

        assertEquals(1L, result.getResourceId());
        assertEquals(e, result.getException());
    }

    @Test
    void testAccept() {
        underTest.accept(new Event<>(new AddVolumesValidateEvent(1L, 2L, "gp2", 200L, CloudVolumeUsageType.GENERAL, "test")));

        verify(eventBus).notify(eq(ADD_VOLUMES_VALIDATION_FINISHED_EVENT.event()), successEventCaptor.capture());
        AddVolumesValidationFinishedEvent successEvent = successEventCaptor.getValue().getData();
        assertEquals(1L, successEvent.getResourceId());
        assertEquals(2L, successEvent.getNumberOfDisks());
        assertEquals("gp2", successEvent.getType());
        assertEquals(200L, successEvent.getSize());
        assertEquals(CloudVolumeUsageType.GENERAL, successEvent.getCloudVolumeUsageType());
        assertEquals("test", successEvent.getInstanceGroup());
    }

    @Test
    void testAcceptFailure() {
        AddVolumesValidateEvent addVolumesValidateEvent = new AddVolumesValidateEvent(1L, 2L, "gp2", 200L, CloudVolumeUsageType.GENERAL, "test");
        doThrow(CloudbreakServiceException.class).when(addVolumesService).validateVolumeAddition(1L, "test",
                addVolumesValidateEvent);

        underTest.accept(new Event<>(addVolumesValidateEvent));

        verify(eventBus).notify(eq(FAILURE_EVENT.event()), failureEventCaptor.capture());
        AddVolumesFailedEvent failureEvent = failureEventCaptor.getValue().getData();
        assertEquals(1L, failureEvent.getResourceId());
        assertEquals(CloudbreakServiceException.class, failureEvent.getException().getClass());
    }
}

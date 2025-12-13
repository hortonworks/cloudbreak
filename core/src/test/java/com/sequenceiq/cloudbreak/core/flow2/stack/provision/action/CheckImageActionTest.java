package com.sequenceiq.cloudbreak.core.flow2.stack.provision.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.event.setup.CheckImageResult;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.service.StackCreationService;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackCreationContext;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.common.api.type.ImageStatus;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(MockitoExtension.class)
public class CheckImageActionTest {

    public static final long STACK_ID = 2L;

    private static final String IMAGE_COPY_FAULT_NUM = "IMAGE_COPY_FAULT_NUM";

    private static final int ANY_STATUS_PROGRESS_VALUE = 10;

    private static final long REPEAT_WAIT_TIME = 30000L;

    private static final int FAULT_TOLERANCE = 5;

    @Mock
    private StackCreationService stackCreationService;

    @Mock
    private Timer timer;

    @Mock
    private EventBus eventBus;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @InjectMocks
    private CheckImageAction underTest;

    @Mock
    private StackCreationContext stackContext;

    @Mock
    private StackEvent stackEvent;

    private Map<Object, Object> variables = new HashMap<>();

    @Test
    void testWhenInProgressThenRepeat() {
        CheckImageResult checkImageResult = new CheckImageResult(1L, ImageStatus.IN_PROGRESS, ANY_STATUS_PROGRESS_VALUE);
        when(stackCreationService.checkImage(stackContext)).thenReturn(checkImageResult);

        underTest.doExecute(stackContext, stackEvent, variables);

        assertEquals(0, variables.get(IMAGE_COPY_FAULT_NUM));
        verify(timer).schedule(any(), eq(REPEAT_WAIT_TIME));
        verify(eventBus, never()).notify(any(), any(Event.class));
    }

    @Test
    void testWhenCreateFinishedThenSendEvent() {
        FlowParameters flowParameters = new FlowParameters("flowId", "flowTriggerUserCrn");
        when(stackContext.getStackId()).thenReturn(STACK_ID);
        when(stackContext.getFlowParameters()).thenReturn(flowParameters);
        when(reactorEventFactory.createEvent(any(Map.class), any())).thenReturn(mock(Event.class));
        CheckImageResult checkImageResult = new CheckImageResult(1L, ImageStatus.CREATE_FINISHED, ANY_STATUS_PROGRESS_VALUE);
        when(stackCreationService.checkImage(stackContext)).thenReturn(checkImageResult);

        underTest.doExecute(stackContext, stackEvent, variables);

        verify(eventBus).notify(any(), any(Event.class));
    }

    @Test
    void testWhenCreateFailedThenIncreaseFaultNumAndRepeat() {
        variables.put(IMAGE_COPY_FAULT_NUM, 0);
        CheckImageResult checkImageResult = new CheckImageResult(1L, ImageStatus.CREATE_FAILED, ANY_STATUS_PROGRESS_VALUE);
        when(stackCreationService.checkImage(stackContext)).thenReturn(checkImageResult);

        underTest.doExecute(stackContext, stackEvent, variables);

        assertEquals(1, variables.get(IMAGE_COPY_FAULT_NUM));
        verify(timer).schedule(any(), anyLong());
        verify(eventBus, never()).notify(any(), any(Event.class));
    }

    @Test
    void testWhenCreateFailedFiveTimesInARowThenThrow() {
        variables.put(IMAGE_COPY_FAULT_NUM, FAULT_TOLERANCE - 1);
        CheckImageResult checkImageResult = new CheckImageResult(1L, ImageStatus.CREATE_FAILED, ANY_STATUS_PROGRESS_VALUE);
        when(stackCreationService.checkImage(stackContext)).thenReturn(checkImageResult);

        assertThrows(CloudbreakServiceException.class, () ->
                underTest.doExecute(stackContext, stackEvent, variables),
                "Image copy failed."
        );

        assertNull(variables.get(IMAGE_COPY_FAULT_NUM));
        verify(timer, never()).schedule(any(), anyLong());
        verify(eventBus, never()).notify(any(), any(Event.class));
    }

    @Test
    void testWhenInProgressAfterCreateFailedThenResetFaultNumAndRepeat() {
        variables.put(IMAGE_COPY_FAULT_NUM, 1);
        CheckImageResult checkImageResult = new CheckImageResult(1L, ImageStatus.IN_PROGRESS, ANY_STATUS_PROGRESS_VALUE);
        when(stackCreationService.checkImage(stackContext)).thenReturn(checkImageResult);

        underTest.doExecute(stackContext, stackEvent, variables);

        assertEquals(0, variables.get(IMAGE_COPY_FAULT_NUM));
        verify(timer).schedule(any(), eq(REPEAT_WAIT_TIME));
        verify(eventBus, never()).notify(any(), any(Event.class));
    }

}

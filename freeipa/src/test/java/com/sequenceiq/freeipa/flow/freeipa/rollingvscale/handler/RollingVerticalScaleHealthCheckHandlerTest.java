package com.sequenceiq.freeipa.flow.freeipa.rollingvscale.handler;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.FreeIpaRollingVerticalScaleFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.RollingVerticalScaleHealthCheckRequest;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.RollingVerticalScaleHealthCheckResult;
import com.sequenceiq.freeipa.flow.stack.start.AttemptMakerFactory;
import com.sequenceiq.freeipa.flow.stack.start.OneFreeIpaReachableAttempt;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@ExtendWith(MockitoExtension.class)
class RollingVerticalScaleHealthCheckHandlerTest {

    private static final long STACK_ID = 1L;

    private static final String INSTANCE_ID = "i-abc123";

    @Mock
    private StackService stackService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private AttemptMakerFactory attemptMakerFactory;

    @Mock
    private OneFreeIpaReachableAttempt reachableAttempt;

    @Mock
    private EventBus eventBus;

    @Mock
    private Stack stack;

    @Mock
    private InstanceMetaData instance;

    @InjectMocks
    private RollingVerticalScaleHealthCheckHandler underTest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "maxAttempts", 3);
        ReflectionTestUtils.setField(underTest, "sleepingTimeSec", 1);
        ReflectionTestUtils.setField(underTest, "consecutiveSuccessRequired", 1);
        when(attemptMakerFactory.create(any(), any(), anyInt())).thenReturn(reachableAttempt);
    }

    @Test
    void testHealthyReturnsHealthCheckResult() throws Exception {
        RollingVerticalScaleHealthCheckRequest request = new RollingVerticalScaleHealthCheckRequest(STACK_ID, INSTANCE_ID);

        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(instanceMetaDataService.getByInstanceIds(STACK_ID, List.of(INSTANCE_ID))).thenReturn(Set.of(instance));
        when(instance.getInstanceId()).thenReturn(INSTANCE_ID);
        doReturn(AttemptResults.justFinish()).when(reachableAttempt).process();

        underTest.accept(new Event<>(request));

        ArgumentCaptor<Event<?>> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(any(String.class), eventCaptor.capture());

        Object result = eventCaptor.getValue().getData();
        assertInstanceOf(RollingVerticalScaleHealthCheckResult.class, result);
        assertTrue(((RollingVerticalScaleHealthCheckResult) result).isHealthy());
    }

    @Test
    void testPollingExhaustedReturnsFailureEvent() throws Exception {
        RollingVerticalScaleHealthCheckRequest request = new RollingVerticalScaleHealthCheckRequest(STACK_ID, INSTANCE_ID);

        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(instanceMetaDataService.getByInstanceIds(STACK_ID, List.of(INSTANCE_ID))).thenReturn(Set.of(instance));
        when(instance.getInstanceId()).thenReturn(INSTANCE_ID);
        doReturn(AttemptResults.justContinue()).when(reachableAttempt).process();

        underTest.accept(new Event<>(request));

        ArgumentCaptor<Event<?>> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(any(String.class), eventCaptor.capture());

        assertInstanceOf(FreeIpaRollingVerticalScaleFailureEvent.class, eventCaptor.getValue().getData());
    }

    @Test
    void testPollingAbortedReturnsFailureEvent() throws Exception {
        RollingVerticalScaleHealthCheckRequest request = new RollingVerticalScaleHealthCheckRequest(STACK_ID, INSTANCE_ID);

        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(instanceMetaDataService.getByInstanceIds(STACK_ID, List.of(INSTANCE_ID))).thenReturn(Set.of(instance));
        when(instance.getInstanceId()).thenReturn(INSTANCE_ID);
        doReturn(AttemptResults.breakFor("polling cancelled")).when(reachableAttempt).process();

        underTest.accept(new Event<>(request));

        ArgumentCaptor<Event<?>> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(any(String.class), eventCaptor.capture());

        assertInstanceOf(FreeIpaRollingVerticalScaleFailureEvent.class, eventCaptor.getValue().getData());
    }
}

package com.sequenceiq.redbeams.flow;

import static com.sequenceiq.redbeams.rotation.RedbeamsSecretType.REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.rotation.flow.chain.SecretRotationFlowChainTriggerEvent;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.core.model.FlowAcceptResult;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.flow.service.FlowNameFormatService;

@ExtendWith(MockitoExtension.class)
class RedbeamsFlowManagerTest {

    private static final String DATABASE_CRN = "databaseCrn";

    private static final Long DATABASE_ID = 1L;

    private static final String FLOW_CHAIN_ID = "flowChainId";

    @Mock
    private EventBus reactor;

    @Mock
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Mock
    private FlowNameFormatService flowNameFormatService;

    @InjectMocks
    private RedbeamsFlowManager underTest;

    @Test
    void triggerSecretRotationShouldSucceed() throws InterruptedException {
        Acceptable data = mock(Acceptable.class);
        Promise<AcceptResult> accepted = (Promise<AcceptResult>) mock(Promise.class);
        when(data.accepted()).thenReturn(accepted);
        Event<Acceptable> event = new Event<>(data);
        when(eventFactory.createEventWithErrHandler(anyMap(), any(Acceptable.class))).thenReturn(event);
        when(accepted.await(10L, TimeUnit.SECONDS)).thenReturn(FlowAcceptResult.runningInFlowChain(FLOW_CHAIN_ID));
        FlowIdentifier flowIdentifier = underTest.triggerSecretRotation(DATABASE_ID, DATABASE_CRN, List.of(REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD),
                null, null);
        assertEquals(FlowType.FLOW_CHAIN, flowIdentifier.getType());
        assertEquals(FLOW_CHAIN_ID, flowIdentifier.getPollableId());
        verify(reactor, times(1)).notify(eq(EventSelectorUtil.selector(SecretRotationFlowChainTriggerEvent.class)), any(Event.class));
    }
}
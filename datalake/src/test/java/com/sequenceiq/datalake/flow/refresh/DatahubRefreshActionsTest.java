package com.sequenceiq.datalake.flow.refresh;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.refresh.event.DatahubRefreshStartEvent;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.refresh.SdxRefreshService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(MockitoExtension.class)

public class DatahubRefreshActionsTest {

    private static final Long SDX_ID = 1L;

    private static final String FLOW_ID = "flow_id";

    private static final String SDX_NAME = "sdx_name";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    @Mock
    private SdxRefreshService sdxRefreshService;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private SdxService sdxService;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @InjectMocks
    private final DatahubRefreshActions underTest = new DatahubRefreshActions();

    @Test
    public void testStartTriggersCall() throws Exception {

        DatahubRefreshStartEvent event = new DatahubRefreshStartEvent(SDX_ID, SDX_NAME, USER_CRN);
        AbstractAction action = (AbstractAction) underTest.startDatahubRefreshAction();
        initActionPrivateFields(action);
        AbstractActionTestSupport testSupport = new AbstractActionTestSupport(action);
        SdxContext context = SdxContext.from(new FlowParameters(FLOW_ID, USER_CRN), event);
        testSupport.doExecute(context, event, new HashMap());

        ArgumentCaptor<DatahubRefreshStartEvent> captor = ArgumentCaptor.forClass(DatahubRefreshStartEvent.class);
        verify(reactorEventFactory, times(1)).createEvent(any(), captor.capture());
        DatahubRefreshStartEvent captorValue = captor.getValue();
        assertEquals(SDX_NAME, captorValue.getSdxName());
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }
}

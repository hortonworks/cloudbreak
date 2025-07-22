package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateHandlerSelectors.DATAHUB_DISK_RESIZE_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateHandlerSelectors.DATAHUB_DISK_UPDATE_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateHandlerSelectors.DATAHUB_DISK_UPDATE_VALIDATION_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateStateSelectors.DATAHUB_DISK_UPDATE_FINALIZE_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.core.flow2.cluster.AbstractClusterAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event.DistroXDiskResizeFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event.DistroXDiskUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(MockitoExtension.class)
class DistroXDiskUpdateActionsTest {

    private Map<Object, Object> variables = new HashMap<>();

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private StackUpdater stackUpdater;

    @InjectMocks
    private DistroXDiskUpdateActions underTest;

    @Captor
    private ArgumentCaptor<String> captor;

    @Captor
    private ArgumentCaptor<String> selectorCaptor;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    private ClusterViewContext context;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private StackView stackView;

    @Mock
    private ClusterView clusterView;

    @BeforeEach
    void setUp() {
        context = new ClusterViewContext(flowParameters, stackView, clusterView);
    }

    @Test
    void testDiskUpdateValidationAction() throws Exception {
        DistroXDiskUpdateEvent event = mock(DistroXDiskUpdateEvent.class);
        doReturn(1L).when(event).getResourceId();
        doReturn("test").when(event).getGroup();
        doReturn("gp2").when(event).getVolumeType();
        AbstractClusterAction<DistroXDiskUpdateEvent> action =
                (AbstractClusterAction<DistroXDiskUpdateEvent>) underTest.datahubDiskUpdateValidationAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);
        verify(flowMessageService, times(1)).fireEventAndLog(anyLong(), captor.capture(), any(), anyString(), anyString(), any());
        assertEquals(Status.DATAHUB_DISK_UPDATE_VALIDATION_IN_PROGRESS.name(), captor.getValue());
        verify(eventBus, times(1)).notify(selectorCaptor.capture(), any());
        assertEquals(DATAHUB_DISK_UPDATE_VALIDATION_HANDLER_EVENT.selector(), selectorCaptor.getValue());
    }

    @Test
    void testDiskUpdateAction() throws Exception {
        DistroXDiskUpdateEvent event = mock(DistroXDiskUpdateEvent.class);
        doReturn(1L).when(event).getResourceId();
        doReturn("test").when(event).getGroup();
        doReturn("gp2").when(event).getVolumeType();
        AbstractClusterAction<DistroXDiskUpdateEvent> action =
                (AbstractClusterAction<DistroXDiskUpdateEvent>) underTest.diskUpdateInDatahubAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);
        verify(flowMessageService, times(1)).fireEventAndLog(anyLong(), captor.capture(), any(), anyString(), anyString(), any());
        assertEquals(Status.UPDATE_IN_PROGRESS.name(), captor.getValue());
        verify(eventBus, times(1)).notify(selectorCaptor.capture(), any());
        assertEquals(DATAHUB_DISK_UPDATE_HANDLER_EVENT.selector(), selectorCaptor.getValue());
    }

    @Test
    void testDiskResizeInDatahubAction() throws Exception {
        DistroXDiskUpdateEvent event = mock(DistroXDiskUpdateEvent.class);
        doReturn(1L).when(event).getResourceId();
        AbstractClusterAction<DistroXDiskUpdateEvent> action =
                (AbstractClusterAction<DistroXDiskUpdateEvent>) underTest.diskResizeInDatahubAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);
        verify(flowMessageService, times(1)).fireEventAndLog(anyLong(), captor.capture(), any(), any());
        assertEquals(Status.UPDATE_IN_PROGRESS.name(), captor.getValue());
        verify(eventBus, times(1)).notify(selectorCaptor.capture(), any());
        assertEquals(DATAHUB_DISK_RESIZE_HANDLER_EVENT.event(), selectorCaptor.getValue());
    }

    @Test
    void testFinishedAction() throws Exception {
        DistroXDiskResizeFinishedEvent event = mock(DistroXDiskResizeFinishedEvent.class);
        doReturn(1L).when(event).getResourceId();
        AbstractClusterAction<DistroXDiskResizeFinishedEvent> action =
                (AbstractClusterAction<DistroXDiskResizeFinishedEvent>) underTest.finishedAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);
        verify(flowMessageService, times(1)).fireEventAndLog(anyLong(), captor.capture(), any());
        assertEquals(Status.AVAILABLE.name(), captor.getValue());
        verify(eventBus, times(1)).notify(selectorCaptor.capture(), any());
        assertEquals(DATAHUB_DISK_UPDATE_FINALIZE_EVENT.selector(), selectorCaptor.getValue());
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }

}

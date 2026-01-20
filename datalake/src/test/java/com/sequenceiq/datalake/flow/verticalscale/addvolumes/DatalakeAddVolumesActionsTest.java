package com.sequenceiq.datalake.flow.verticalscale.addvolumes;

import static com.sequenceiq.datalake.flow.verticalscale.addvolumes.event.DatalakeAddVolumesStateSelectors.DATALAKE_ADD_VOLUMES_FINALIZE_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.addvolumes.event.DatalakeAddVolumesStateSelectors.DATALAKE_ADD_VOLUMES_FINISH_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.addvolumes.event.DatalakeAddVolumesStateSelectors.DATALAKE_ADD_VOLUMES_HANDLER_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.addvolumes.event.DatalakeAddVolumesStateSelectors.DATALAKE_ADD_VOLUMES_TRIGGER_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.addvolumes.event.DatalakeAddVolumesStateSelectors.HANDLED_FAILED_DATALAKE_ADD_VOLUMES_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
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

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackAddVolumesRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.verticalscale.addvolumes.event.DatalakeAddVolumesEvent;
import com.sequenceiq.datalake.flow.verticalscale.addvolumes.event.DatalakeAddVolumesFailedEvent;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(MockitoExtension.class)
class DatalakeAddVolumesActionsTest {

    private static final Long SDX_ID = 1L;

    private static final String USER_ID = "TEST_USER";

    private static final String SDX_NAME = "TEST_SDX";

    @Mock
    private SdxStatusService sdxStatusService;

    @InjectMocks
    private DatalakeAddVolumesActions underTest;

    private SdxContext context;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Captor
    private ArgumentCaptor<String> captor;

    @Captor
    private ArgumentCaptor<DatalakeStatusEnum> statusCaptor;

    private StackAddVolumesRequest stackAddVolumesRequest;

    @BeforeEach
    void setUp() {
        context = new SdxContext(flowParameters, SDX_ID, USER_ID);
        stackAddVolumesRequest = new StackAddVolumesRequest();
        stackAddVolumesRequest.setInstanceGroup("test");
        stackAddVolumesRequest.setNumberOfDisks(2L);
        stackAddVolumesRequest.setType("gp2");
        stackAddVolumesRequest.setSize(200L);
        stackAddVolumesRequest.setCloudVolumeUsageType(CloudVolumeUsageType.GENERAL.name());
    }

    @Test
    void testAddVolumesAction() throws Exception {
        String selector = DATALAKE_ADD_VOLUMES_TRIGGER_EVENT.event();
        DatalakeAddVolumesEvent event = new DatalakeAddVolumesEvent(selector, SDX_ID, USER_ID, stackAddVolumesRequest, SDX_NAME);
        Map<Object, Object> variables = new HashMap<>();
        AbstractSdxAction<DatalakeAddVolumesEvent> action = (AbstractSdxAction<DatalakeAddVolumesEvent>) underTest.addVolumesAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);
        verify(eventBus).notify(captor.capture(), any());
        assertEquals(DATALAKE_ADD_VOLUMES_HANDLER_EVENT.selector(), captor.getValue());
        String statusMessage = "Calling core services to trigger add volumes flow for group of test on the Data Lake.";
        verify(sdxStatusService).setStatusForDatalakeAndNotifyWithStatusReason(eq(DatalakeStatusEnum.DATALAKE_ADD_VOLUMES_IN_PROGRESS),
                eq(statusMessage), eq(SDX_ID));
    }

    @Test
    void testAddVolumesFinishedAction() throws Exception {
        String selector = DATALAKE_ADD_VOLUMES_FINISH_EVENT.event();
        DatalakeAddVolumesEvent event = new DatalakeAddVolumesEvent(selector, SDX_ID, USER_ID, stackAddVolumesRequest, SDX_NAME);
        Map<Object, Object> variables = new HashMap<>();
        AbstractSdxAction<DatalakeAddVolumesEvent> action = (AbstractSdxAction<DatalakeAddVolumesEvent>) underTest.addVolumesFinishedAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);
        verify(eventBus).notify(captor.capture(), any());
        assertEquals(DATALAKE_ADD_VOLUMES_FINALIZE_EVENT.selector(), captor.getValue());
        String statusMessage = "Adding volumes has finished on the Data Lake.";
        verify(sdxStatusService).setStatusForDatalakeAndNotifyWithStatusReason(eq(DatalakeStatusEnum.RUNNING), eq(statusMessage), eq(SDX_ID));
    }

    @Test
    void testAddDisksFailedAction() throws Exception {
        doReturn("flow-id").when(flowParameters).getFlowId();
        DatalakeAddVolumesFailedEvent event = new DatalakeAddVolumesFailedEvent(SDX_ID, USER_ID, new CloudbreakServiceException("TEST"));
        Map<Object, Object> variables = new HashMap<>();
        AbstractSdxAction<DatalakeAddVolumesFailedEvent> action = (AbstractSdxAction<DatalakeAddVolumesFailedEvent>) underTest.addVolumesFailedAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);
        verify(eventBus).notify(captor.capture(), any());
        assertEquals(HANDLED_FAILED_DATALAKE_ADD_VOLUMES_EVENT.selector(), captor.getValue());
        verify(sdxStatusService).setStatusForDatalakeAndNotify(eq(DatalakeStatusEnum.RUNNING),
                eq(DatalakeStatusEnum.DATALAKE_ADD_VOLUMES_FAILED.getDefaultResourceEvent()), eq("TEST"), eq(SDX_ID));
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }
}

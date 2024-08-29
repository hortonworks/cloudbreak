package com.sequenceiq.datalake.flow.verticalscale.rootvolume;

import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_DISK_UPDATE_FAILED;
import static com.sequenceiq.datalake.flow.verticalscale.rootvolume.event.DatalakeRootVolumeUpdateStateSelectors.DATALAKE_ROOT_VOLUME_UPDATE_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.rootvolume.event.DatalakeRootVolumeUpdateStateSelectors.DATALAKE_ROOT_VOLUME_UPDATE_FINALIZE_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.rootvolume.event.DatalakeRootVolumeUpdateStateSelectors.DATALAKE_ROOT_VOLUME_UPDATE_FINISH_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.rootvolume.event.DatalakeRootVolumeUpdateStateSelectors.DATALAKE_ROOT_VOLUME_UPDATE_HANDLER_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.rootvolume.event.DatalakeRootVolumeUpdateStateSelectors.HANDLED_FAILED_DATALAKE_ROOT_VOLUME_UPDATE_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.events.RootVolumeUpdateRequest;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.verticalscale.rootvolume.event.DatalakeRootVolumeUpdateEvent;
import com.sequenceiq.datalake.flow.verticalscale.rootvolume.event.DatalakeRootVolumeUpdateFailedEvent;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(MockitoExtension.class)
class DatalakeRootVolumeUpdateActionsTest {

    private static final Long SDX_ID = 1L;

    private static final String USER_ID = "TEST_USER";

    private static final String SDX_NAME = "TEST_SDX";

    private static final String SDX_CRN = "TEST_SDX_CRN";

    private static final String TEST_ACCOUNT_ID = "ACCOUNT_ID";

    private static final String TEST_CLOUD_PLATFORM = "AWS";

    @Mock
    private SdxStatusService sdxStatusService;

    @InjectMocks
    private DatalakeRootVolumeUpdateActions underTest;

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

    private DiskUpdateRequest diskUpdateRequest;

    private RootVolumeUpdateRequest rootVolumeUpdateRequest;

    private SdxContext context;

    @BeforeEach
    void setUp() {
        context = new SdxContext(flowParameters, SDX_ID, USER_ID);
        diskUpdateRequest = new DiskUpdateRequest();
        diskUpdateRequest.setDiskType(DiskType.ROOT_DISK);
        diskUpdateRequest.setSize(210);
        diskUpdateRequest.setVolumeType("gp2");
        diskUpdateRequest.setGroup("test");
        rootVolumeUpdateRequest = RootVolumeUpdateRequest.convert(diskUpdateRequest);
    }

    @Test
    void testRootVolumeUpdateAction() throws Exception {
        DatalakeRootVolumeUpdateEvent event = getEvent(DATALAKE_ROOT_VOLUME_UPDATE_EVENT.event());
        Map<Object, Object> variables = new HashMap<>();
        AbstractDatalakeRootVolumeUpdateAction<DatalakeRootVolumeUpdateEvent> action =
                (AbstractDatalakeRootVolumeUpdateAction<DatalakeRootVolumeUpdateEvent>) underTest.rootVolumeUpdateAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);
        verify(eventBus).notify(captor.capture(), any());
        assertEquals(DATALAKE_ROOT_VOLUME_UPDATE_HANDLER_EVENT.selector(), captor.getValue());
        String statusMessage = "Root disk update is in progress for group of test on the Data Lake.";
        verify(sdxStatusService).setStatusForDatalakeAndNotifyWithStatusReason(eq(DatalakeStatusEnum.DATALAKE_DISK_UPDATE_IN_PROGRESS),
                eq(statusMessage), eq(SDX_ID));
    }

    @Test
    void testRootVolumeUpdateFinishedAction() throws Exception {
        DatalakeRootVolumeUpdateEvent event = getEvent(DATALAKE_ROOT_VOLUME_UPDATE_FINISH_EVENT.event());
        Map<Object, Object> variables = new HashMap<>();
        AbstractDatalakeRootVolumeUpdateAction<DatalakeRootVolumeUpdateEvent> action =
                (AbstractDatalakeRootVolumeUpdateAction<DatalakeRootVolumeUpdateEvent>) underTest.rootVolumeUpdateFinishedAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);
        verify(eventBus).notify(captor.capture(), any());
        assertEquals(DATALAKE_ROOT_VOLUME_UPDATE_FINALIZE_EVENT.selector(), captor.getValue());
        String statusMessage = "Root volume update  of test has finished on the Data Lake.";
        verify(sdxStatusService).setStatusForDatalakeAndNotifyWithStatusReason(eq(DatalakeStatusEnum.RUNNING),
                eq(statusMessage), eq(SDX_ID));
    }

    @Test
    void testFailedAction() throws Exception {
        Exception ex = new Exception("test");
        DatalakeRootVolumeUpdateEvent event = getEvent(DATALAKE_ROOT_VOLUME_UPDATE_EVENT.event());
        DatalakeRootVolumeUpdateFailedEvent failureEvent = DatalakeRootVolumeUpdateFailedEvent.builder()
                .withDatalakeRootVolumeUpdateEvent(event).withException(ex).withDatalakeStatus(DATALAKE_DISK_UPDATE_FAILED).build();
        Map<Object, Object> variables = new HashMap<>();
        AbstractDatalakeRootVolumeUpdateAction<DatalakeRootVolumeUpdateFailedEvent> action =
                (AbstractDatalakeRootVolumeUpdateAction<DatalakeRootVolumeUpdateFailedEvent>) underTest.failedAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, failureEvent, variables);
        verify(eventBus).notify(captor.capture(), any());
        assertEquals(HANDLED_FAILED_DATALAKE_ROOT_VOLUME_UPDATE_EVENT.selector(), captor.getValue());
        verify(sdxStatusService).setStatusForDatalakeAndNotify(eq(DATALAKE_DISK_UPDATE_FAILED),
                eq(DATALAKE_DISK_UPDATE_FAILED.getDefaultResourceEvent()),
                eq("test"), eq(SDX_ID));
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }

    private DatalakeRootVolumeUpdateEvent getEvent(String event) {
        return new DatalakeRootVolumeUpdateEvent(
                event, SDX_ID, new Promise<>(), SDX_NAME, SDX_CRN, SDX_CRN, SDX_NAME, TEST_ACCOUNT_ID, rootVolumeUpdateRequest,
                TEST_CLOUD_PLATFORM, SDX_ID, "userCrn");
    }
}

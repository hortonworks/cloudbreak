package com.sequenceiq.datalake.flow.encryptionprofile.handler;

import static com.sequenceiq.datalake.flow.encryptionprofile.SdxEnableEncryptionProfileEvent.SDX_ENABLE_ENCRYPTION_PROFILE_SUCCESS_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.encryptionprofile.event.SdxEnableEncryptionProfileFailedEvent;
import com.sequenceiq.datalake.flow.encryptionprofile.event.SdxEnableEncryptionProfileHandlerEvent;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.flowwait.SdxWaitService;
import com.sequenceiq.datalake.service.sdx.flowwait.exception.SdxWaitException;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class SdxEnableEncryptionProfileHandlerTest {

    private static final long SDX_ID = 1L;

    private static final String USER_ID = "userId";

    private static final String SDX_CRN = "crn:cdp:datalake:us-west-1:1234:datalake:dl";

    private static final String ENCRYPTION_PROFILE_CRN = "crn:cdp:environments:us-west-1:1234:encryptionProfile:ep";

    private static final int SLEEP_INTERVAL_IN_SECONDS = 30;

    private static final int DURATION_IN_MINUTES = 30;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private CloudbreakFlowService cloudbreakFlowService;

    @Mock
    private SdxWaitService sdxWaitService;

    @Mock
    private SdxService sdxService;

    @InjectMocks
    private SdxEnableEncryptionProfileHandler underTest;

    @Captor
    private ArgumentCaptor<PollingConfig> pollingConfigArgumentCaptor;

    private SdxEnableEncryptionProfileHandlerEvent event;

    @BeforeEach
    void setUp() {
        event = new SdxEnableEncryptionProfileHandlerEvent(
                EventSelectorUtil.selector(SdxEnableEncryptionProfileHandlerEvent.class), SDX_ID, USER_ID, ENCRYPTION_PROFILE_CRN);
    }

    @Test
    void testSelector() {
        assertEquals(EventSelectorUtil.selector(SdxEnableEncryptionProfileHandlerEvent.class), underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Selectable response = underTest.defaultFailureEvent(SDX_ID, new Exception("failed"), new Event<>(event));

        assertThat(response).isInstanceOf(SdxEnableEncryptionProfileFailedEvent.class);
        assertEquals(SDX_ID, response.getResourceId());
        assertEquals("failed", ((SdxEnableEncryptionProfileFailedEvent) response).getException().getMessage());
    }

    @Test
    void testSdxEnableEncryptionProfileHandlerSuccess() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setCrn(SDX_CRN);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, "flowId");
        when(sdxService.getById(SDX_ID)).thenReturn(sdxCluster);
        when(stackV4Endpoint.updateSslConfigurationsByCrn(eq(0L), eq(SDX_CRN), eq(ENCRYPTION_PROFILE_CRN))).thenReturn(flowIdentifier);

        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        assertEquals(SDX_ENABLE_ENCRYPTION_PROFILE_SUCCESS_EVENT.event(), response.getSelector());
        assertEquals(SDX_ID, response.getResourceId());
        verify(cloudbreakFlowService).saveLastCloudbreakFlowChainId(eq(sdxCluster), eq(flowIdentifier));
        verify(sdxWaitService).waitForCloudbreakFlow(eq(SDX_ID), pollingConfigArgumentCaptor.capture(), eq("Enable encryption profile"));
        PollingConfig pollingConfig = pollingConfigArgumentCaptor.getValue();
        assertEquals(SLEEP_INTERVAL_IN_SECONDS, pollingConfig.getSleepTime());
        assertEquals(TimeUnit.SECONDS, pollingConfig.getSleepTimeUnit());
        assertEquals(DURATION_IN_MINUTES, pollingConfig.getDuration());
        assertEquals(TimeUnit.MINUTES, pollingConfig.getDurationTimeUnit());
    }

    @Test
    void testSdxEnableEncryptionProfileHandlerFailure() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setCrn(SDX_CRN);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, "flowId");
        when(sdxService.getById(SDX_ID)).thenReturn(sdxCluster);
        when(stackV4Endpoint.updateSslConfigurationsByCrn(eq(0L), eq(SDX_CRN), eq(ENCRYPTION_PROFILE_CRN))).thenReturn(flowIdentifier);
        SdxWaitException cause = new SdxWaitException("wait failed", new RuntimeException());
        doThrow(cause).when(sdxWaitService).waitForCloudbreakFlow(anyLong(), any(), any());

        assertThatThrownBy(() -> underTest.doAccept(new HandlerEvent<>(new Event<>(event))))
                .isSameAs(cause);
        verify(cloudbreakFlowService).saveLastCloudbreakFlowChainId(eq(sdxCluster), eq(flowIdentifier));
    }
}

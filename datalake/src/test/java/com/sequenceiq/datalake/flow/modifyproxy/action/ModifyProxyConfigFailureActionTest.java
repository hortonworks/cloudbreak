package com.sequenceiq.datalake.flow.modifyproxy.action;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.modifyproxy.ModifyProxyConfigTrackerEvent;
import com.sequenceiq.datalake.flow.modifyproxy.event.ModifyProxyConfigFailureResponse;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.ActionTest;

@ExtendWith(MockitoExtension.class)
class ModifyProxyConfigFailureActionTest extends ActionTest {

    private static final long DATALAKE_ID = 1L;

    private static final String CAUSE = "cause";

    @Mock
    private SdxStatusService sdxStatusService;

    @InjectMocks
    private ModifyProxyConfigFailureAction underTest;

    private SdxContext context;

    @Mock
    private ModifyProxyConfigFailureResponse payload;

    @BeforeEach
    void setUp() {
        context = new SdxContext(flowParameters, DATALAKE_ID, "user-id");
        lenient().when(payload.getException()).thenReturn(new Exception(CAUSE));
    }

    @Test
    void createRequest() {
        Selectable result = underTest.createRequest(context);

        Assertions.assertThat(result)
                .isInstanceOf(SdxEvent.class)
                .extracting(SdxEvent.class::cast)
                .returns(ModifyProxyConfigTrackerEvent.MODIFY_PROXY_CONFIG_FAIL_HANDLED_EVENT.selector(), SdxEvent::selector)
                .returns(context.getSdxId(), SdxEvent::getResourceId)
                .returns(context.getUserId(), SdxEvent::getUserId);
    }

    @Test
    void doExecute() throws Exception {
        underTest.doExecute(context, payload, Map.of());

        verifySendEvent();
        verify(sdxStatusService).setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_PROXY_CONFIG_MODIFICATION_FAILED, CAUSE, DATALAKE_ID);
    }

}

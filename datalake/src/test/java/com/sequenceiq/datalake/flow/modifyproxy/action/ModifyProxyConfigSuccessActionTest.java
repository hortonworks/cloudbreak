package com.sequenceiq.datalake.flow.modifyproxy.action;

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
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.modifyproxy.ModifyProxyConfigTrackerEvent;
import com.sequenceiq.datalake.flow.modifyproxy.event.ModifyProxyConfigSuccessResponse;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.ActionTest;

@ExtendWith(MockitoExtension.class)
class ModifyProxyConfigSuccessActionTest extends ActionTest {

    @Mock
    private SdxStatusService sdxStatusService;

    @InjectMocks
    private ModifyProxyConfigSuccessAction underTest;

    private SdxContext context;

    @Mock
    private ModifyProxyConfigSuccessResponse payload;

    @BeforeEach
    void setUp() {
        context = new SdxContext(flowParameters, 1L, "user-id");
    }

    @Test
    void createRequest() {
        Selectable result = underTest.createRequest(context);

        Assertions.assertThat(result)
                .isInstanceOf(SdxEvent.class)
                .extracting(SdxEvent.class::cast)
                .returns(ModifyProxyConfigTrackerEvent.MODIFY_PROXY_CONFIG_FINISHED_EVENT.selector(), SdxEvent::selector)
                .returns(context.getSdxId(), SdxEvent::getResourceId)
                .returns(context.getUserId(), SdxEvent::getUserId);
    }

    @Test
    void doExecute() throws Exception {
        underTest.doExecute(context, payload, Map.of());

        verifySendEvent();
        verify(sdxStatusService).setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING, ResourceEvent.DATALAKE_PROXY_CONFIG_MODIFICATION_FINISHED,
                "Successfully modified proxy config", context.getSdxId());
    }

}

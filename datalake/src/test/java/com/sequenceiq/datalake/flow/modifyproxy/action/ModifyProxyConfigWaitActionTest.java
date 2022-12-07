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
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.modifyproxy.event.ModifyProxyConfigWaitRequest;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.ActionTest;

@ExtendWith(MockitoExtension.class)
class ModifyProxyConfigWaitActionTest extends ActionTest {

    @Mock
    private SdxStatusService sdxStatusService;

    @InjectMocks
    private ModifyProxyConfigWaitAction underTest;

    private SdxContext context;

    @Mock
    private SdxEvent payload;

    @BeforeEach
    void setUp() {
        context = new SdxContext(flowParameters, 1L, "user-id");
    }

    @Test
    void createRequest() {
        Selectable result = underTest.createRequest(context);

        Assertions.assertThat(result)
                .isInstanceOf(ModifyProxyConfigWaitRequest.class)
                .extracting(ModifyProxyConfigWaitRequest.class::cast)
                .returns(context.getSdxId(), SdxEvent::getResourceId)
                .returns(context.getUserId(), SdxEvent::getUserId);
    }

    @Test
    void doExecute() throws Exception {
        underTest.doExecute(context, payload, Map.of());

        verifySendEvent();
        verify(sdxStatusService).setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_PROXY_CONFIG_MODIFICATION_IN_PROGRESS,
                "Modifying proxy config", context.getSdxId());
    }

}

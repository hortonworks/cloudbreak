package com.sequenceiq.datalake.flow.modifyproxy.action;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.modifyproxy.ModifyProxyConfigTrackerEvent;
import com.sequenceiq.datalake.flow.modifyproxy.event.ModifyProxyConfigFailureResponse;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;

@Component("ModifyProxyConfigFailureAction")
public class ModifyProxyConfigFailureAction extends ModifyProxyConfigAction<ModifyProxyConfigFailureResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModifyProxyConfigFailureAction.class);

    @Inject
    private SdxStatusService sdxStatusService;

    protected ModifyProxyConfigFailureAction() {
        super(ModifyProxyConfigFailureResponse.class);
    }

    @Override
    protected void doExecute(SdxContext context, ModifyProxyConfigFailureResponse payload, Map<Object, Object> variables) throws Exception {
        LOGGER.info("Modify proxy config failed for SDX stack {}", context.getSdxId());
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_PROXY_CONFIG_MODIFICATION_FAILED,
                payload.getException().getMessage(), context.getSdxId());
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(SdxContext context) {
        return new SdxEvent(ModifyProxyConfigTrackerEvent.MODIFY_PROXY_CONFIG_FAIL_HANDLED_EVENT.selector(), context.getSdxId(), context.getUserId());
    }
}

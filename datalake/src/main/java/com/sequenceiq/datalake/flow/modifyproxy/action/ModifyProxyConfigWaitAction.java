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
import com.sequenceiq.datalake.flow.modifyproxy.event.ModifyProxyConfigWaitRequest;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;

@Component("ModifyProxyConfigWaitAction")
public class ModifyProxyConfigWaitAction extends ModifyProxyConfigAction<SdxEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxEvent.class);

    @Inject
    private SdxStatusService sdxStatusService;

    protected ModifyProxyConfigWaitAction() {
        super(SdxEvent.class);
    }

    @Override
    protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) throws Exception {
        LOGGER.info("Waiting for modify proxy config for SDX stack {}", context.getSdxId());
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_PROXY_CONFIG_MODIFICATION_IN_PROGRESS,
                "Modifying proxy config", context.getSdxId());
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(SdxContext context) {
        return new ModifyProxyConfigWaitRequest(context.getSdxId(), context.getUserId());
    }
}

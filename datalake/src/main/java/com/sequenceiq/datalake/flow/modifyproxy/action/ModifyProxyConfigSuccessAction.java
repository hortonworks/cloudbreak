package com.sequenceiq.datalake.flow.modifyproxy.action;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.modifyproxy.ModifyProxyConfigTrackerEvent;
import com.sequenceiq.datalake.flow.modifyproxy.event.ModifyProxyConfigSuccessResponse;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;

@Component("ModifyProxyConfigSuccessAction")
public class ModifyProxyConfigSuccessAction extends ModifyProxyConfigAction<ModifyProxyConfigSuccessResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModifyProxyConfigSuccessAction.class);

    @Inject
    private SdxStatusService sdxStatusService;

    protected ModifyProxyConfigSuccessAction() {
        super(ModifyProxyConfigSuccessResponse.class);
    }

    @Override
    protected void doExecute(SdxContext context, ModifyProxyConfigSuccessResponse payload, Map<Object, Object> variables) throws Exception {
        LOGGER.info("Modify proxy config finished for SDX stack {}", context.getSdxId());
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING, ResourceEvent.DATALAKE_PROXY_CONFIG_MODIFICATION_FINISHED,
                "Successfully modified proxy config", context.getSdxId());
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(SdxContext context) {
        return new SdxEvent(ModifyProxyConfigTrackerEvent.MODIFY_PROXY_CONFIG_FINISHED_EVENT.selector(), context.getSdxId(), context.getUserId());
    }
}

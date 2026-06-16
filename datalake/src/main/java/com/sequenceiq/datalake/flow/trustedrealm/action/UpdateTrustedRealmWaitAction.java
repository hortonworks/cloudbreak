package com.sequenceiq.datalake.flow.trustedrealm.action;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.trustedrealm.event.UpdateTrustedRealmWaitRequest;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;

@Component("UpdateTrustedRealmWaitAction")
public class UpdateTrustedRealmWaitAction extends UpdateTrustedRealmAction<SdxEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateTrustedRealmWaitAction.class);

    @Inject
    private SdxStatusService sdxStatusService;

    protected UpdateTrustedRealmWaitAction() {
        super(SdxEvent.class);
    }

    @Override
    protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) throws Exception {
        LOGGER.info("Waiting for update trusted realm for SDX stack {}", context.getSdxId());
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_UPDATE_TRUSTED_REALM_IN_PROGRESS,
                "Updating trusted realm", context.getSdxId());
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(SdxContext context) {
        return new UpdateTrustedRealmWaitRequest(context.getSdxId(), context.getUserId());
    }
}

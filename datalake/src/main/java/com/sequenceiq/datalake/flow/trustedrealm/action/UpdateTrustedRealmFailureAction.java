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
import com.sequenceiq.datalake.flow.trustedrealm.UpdateTrustedRealmTrackerEvent;
import com.sequenceiq.datalake.flow.trustedrealm.event.UpdateTrustedRealmFailureResponse;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;

@Component("UpdateTrustedRealmFailureAction")
public class UpdateTrustedRealmFailureAction extends UpdateTrustedRealmAction<UpdateTrustedRealmFailureResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateTrustedRealmFailureAction.class);

    @Inject
    private SdxStatusService sdxStatusService;

    protected UpdateTrustedRealmFailureAction() {
        super(UpdateTrustedRealmFailureResponse.class);
    }

    @Override
    protected void doExecute(SdxContext context, UpdateTrustedRealmFailureResponse payload, Map<Object, Object> variables) throws Exception {
        LOGGER.info("Update trusted realm failed for SDX stack {}", context.getSdxId());
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_UPDATE_TRUSTED_REALM_FAILED,
                payload.getException().getMessage(), context.getSdxId());
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(SdxContext context) {
        return new SdxEvent(UpdateTrustedRealmTrackerEvent.UPDATE_TRUSTED_REALM_FAIL_HANDLED_EVENT.selector(), context.getSdxId(), context.getUserId());
    }
}

package com.sequenceiq.datalake.flow.trustedrealm.action;

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
import com.sequenceiq.datalake.flow.trustedrealm.UpdateTrustedRealmTrackerEvent;
import com.sequenceiq.datalake.flow.trustedrealm.event.UpdateTrustedRealmSuccessResponse;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;

@Component("UpdateTrustedRealmSuccessAction")
public class UpdateTrustedRealmSuccessAction extends UpdateTrustedRealmAction<UpdateTrustedRealmSuccessResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateTrustedRealmSuccessAction.class);

    @Inject
    private SdxStatusService sdxStatusService;

    protected UpdateTrustedRealmSuccessAction() {
        super(UpdateTrustedRealmSuccessResponse.class);
    }

    @Override
    protected void doExecute(SdxContext context, UpdateTrustedRealmSuccessResponse payload, Map<Object, Object> variables) throws Exception {
        LOGGER.info("Update trusted realm finished for SDX stack {}", context.getSdxId());
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING, ResourceEvent.DATALAKE_UPDATE_TRUSTED_REALM_FINISHED,
                "Successfully updated trusted realm", context.getSdxId());
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(SdxContext context) {
        return new SdxEvent(UpdateTrustedRealmTrackerEvent.UPDATE_TRUSTED_REALM_FINISHED_EVENT.selector(), context.getSdxId(), context.getUserId());
    }
}

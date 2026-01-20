package com.sequenceiq.datalake.flow.update.publicdns;

import static com.sequenceiq.datalake.flow.update.publicdns.DatalakeUpdatePublicDnsEntriesFlowEvent.DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_FAIL_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.update.publicdns.DatalakeUpdatePublicDnsEntriesFlowEvent.DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_FINALIZED_EVENT;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.update.publicdns.handler.WaitDatalakeUpdateDnsEntriesRequest;
import com.sequenceiq.datalake.flow.update.publicdns.handler.WaitDatalakeUpdateDnsEntriesResponse;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.CloudbreakStackService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;

@Configuration
public class DatalakeUpdatePublicDnsEntriesFlowActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeUpdatePublicDnsEntriesFlowActions.class);

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private SdxService sdxService;

    @Inject
    private CloudbreakStackService cloudbreakStackService;

    @Bean(name = "DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_STATE")
    public Action<?, ?> updateDatalakePublicDnsEntriesAction() {

        return new AbstractSdxAction<>(DatalakeUpdatePublicDnsEntriesTriggerEvent.class) {
            @Override
            protected void doExecute(SdxContext context, DatalakeUpdatePublicDnsEntriesTriggerEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Initiating the process to update public DNS entries for the Datalake.");
                WaitDatalakeUpdateDnsEntriesRequest waitDatalakeUpdateDnsEntriesRequest = new WaitDatalakeUpdateDnsEntriesRequest(payload.getResourceId(),
                        context.getUserId());
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_IN_PROGRESS,
                        "Initiating the process to update public DNS entries for the Datalake",
                        context.getSdxId());
                SdxCluster sdxCluster = sdxService.getById(payload.getResourceId());
                cloudbreakStackService.updatePublicDnsEntries(sdxCluster);
                LOGGER.info("Successfully initiated the process to update public DNS entries for the Datalake with payload: {}", payload);
                sendEvent(context, waitDatalakeUpdateDnsEntriesRequest);
            }

            @Override
            protected Object getFailurePayload(DatalakeUpdatePublicDnsEntriesTriggerEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return new DatalakeUpdatePublicDnsEntriesFailedEvent(payload.getResourceId(), payload.getUserId(), ex);
            }
        };
    }

    @Bean(name = "DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_FINISHED_STATE")
    public Action<?, ?> updateDatalakePublicDnsEntriesFinishedAction() {

        return new AbstractSdxAction<>(WaitDatalakeUpdateDnsEntriesResponse.class) {
            @Override
            protected void doExecute(SdxContext context, WaitDatalakeUpdateDnsEntriesResponse payload, Map<Object, Object> variables) {
                LOGGER.info("Successfully updated public DNS entries for the Datalake");
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING,
                        "Successfully updated public DNS entries for the Datalake", context.getSdxId());
                sendEvent(context, new SdxEvent(DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_FINALIZED_EVENT.event(), context.getSdxId(), context.getUserId()));
            }

            @Override
            protected Object getFailurePayload(WaitDatalakeUpdateDnsEntriesResponse payload, Optional<SdxContext> flowContext, Exception ex) {
                return new DatalakeUpdatePublicDnsEntriesFailedEvent(payload.getResourceId(), payload.getUserId(), ex);
            }
        };
    }

    @Bean(name = "DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_FAILED_STATE")
    public Action<?, ?> updateDatalakePublicDnsEntriesFailedAction() {
        return new AbstractSdxAction<>(DatalakeUpdatePublicDnsEntriesFailedEvent.class) {

            @Override
            protected Selectable createRequest(SdxContext context) {
                return new SdxEvent(DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_FAIL_HANDLED_EVENT.event(), context.getSdxId(), context.getUserId());
            }

            @Override
            protected void doExecute(SdxContext context, DatalakeUpdatePublicDnsEntriesFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.warn("Failed to update public DNS entries for Datalake with error: '{}'", payload.getException().getMessage(), payload.getException());
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_FAILED,
                        Collections.singleton(payload.getException().getMessage()), "Failed to update public DNS entries for Datalake", context.getSdxId());
                sendEvent(context);
            }

            @Override
            protected Object getFailurePayload(DatalakeUpdatePublicDnsEntriesFailedEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }
        };
    }
}

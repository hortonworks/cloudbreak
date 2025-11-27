package com.sequenceiq.datalake.flow.salt.update;

import static com.sequenceiq.datalake.flow.salt.update.SaltUpdateEvent.SALT_UPDATE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.salt.update.SaltUpdateEvent.SALT_UPDATE_FINISHED_EVENT;

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
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.salt.update.event.SaltUpdateFailureResponse;
import com.sequenceiq.datalake.flow.salt.update.event.SaltUpdateRequest;
import com.sequenceiq.datalake.flow.salt.update.event.SaltUpdateSuccessResponse;
import com.sequenceiq.datalake.flow.salt.update.event.SaltUpdateTriggerEvent;
import com.sequenceiq.datalake.flow.salt.update.event.SaltUpdateWaitRequest;
import com.sequenceiq.datalake.flow.salt.update.event.SaltUpdateWaitSuccessResponse;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;

@Configuration
public class SaltUpdateActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltUpdateActions.class);

    private static final String PREVIOUS_STACK_STATUS = "previousStackStatus";

    @Inject
    private SdxStatusService sdxStatusService;

    @Bean(name = "SALT_UPDATE_STATE")
    public Action<?, ?> saltUpdate() {
        return new AbstractSdxAction<>(SaltUpdateTriggerEvent.class) {

            @Override
            protected void doExecute(SdxContext context, SaltUpdateTriggerEvent payload, Map<Object, Object> variables) throws Exception {
                variables.put(PREVIOUS_STACK_STATUS, sdxStatusService.getActualStatusForSdx(context.getSdxId()));
                LOGGER.info("Initiating Salt update for SDX stack {}", context.getSdxId());
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.SALT_UPDATE_IN_PROGRESS,
                        "Initiating SaltStack update", context.getSdxId());
                SaltUpdateRequest request = new SaltUpdateRequest(context.getSdxId(), context.getUserId(),
                        Optional.ofNullable(payload.isSkipHighstate()).orElse(Boolean.FALSE));
                sendEvent(context, request);
            }

            @Override
            protected Object getFailurePayload(SaltUpdateTriggerEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return new SaltUpdateFailureResponse(payload.getResourceId(), payload.getUserId(), ex);
            }
        };
    }

    @Bean(name = "SALT_UPDATE_WAIT_STATE")
    public Action<?, ?> saltUpdateWait() {
        return new AbstractSdxAction<>(SaltUpdateSuccessResponse.class) {

            @Override
            protected void doExecute(SdxContext context, SaltUpdateSuccessResponse payload, Map<Object, Object> variables) throws Exception {
                variables.put(PREVIOUS_STACK_STATUS, sdxStatusService.getActualStatusForSdx(context.getSdxId()));
                LOGGER.info("Waiting for Salt update for SDX stack {}", context.getSdxId());
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.SALT_UPDATE_IN_PROGRESS,
                        "Running SaltStack update", context.getSdxId());
                sendEvent(context);
            }

            @Override
            protected Object getFailurePayload(SaltUpdateSuccessResponse payload, Optional<SdxContext> flowContext, Exception ex) {
                return new SaltUpdateFailureResponse(payload.getResourceId(), payload.getUserId(), ex);
            }

            @Override
            protected Selectable createRequest(SdxContext context) {
                return new SaltUpdateWaitRequest(context.getSdxId(), context.getUserId());
            }
        };
    }

    @Bean(name = "SALT_UPDATE_FINISHED_STATE")
    public Action<?, ?> saltUpdateFinished() {
        return new AbstractSdxAction<>(SaltUpdateWaitSuccessResponse.class) {

            @Override
            protected void doExecute(SdxContext context, SaltUpdateWaitSuccessResponse payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Salt update for SDX stack {} finished", context.getSdxId());
                String statusReason = "SaltStack update finished successfully";
                if (variables.get(PREVIOUS_STACK_STATUS) != null) {
                    String previousStatusReason = ((SdxStatusEntity) variables.get(PREVIOUS_STACK_STATUS)).getStatusReason();
                    if (!previousStatusReason.isBlank()) {
                        if (previousStatusReason.startsWith(statusReason)) {
                            statusReason = previousStatusReason;
                        } else {
                            statusReason += ": " + previousStatusReason;
                        }
                    }
                }
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.SALT_UPDATE_FINISHED, statusReason, context.getSdxId());
                sendEvent(context);
            }

            @Override
            protected Object getFailurePayload(SaltUpdateWaitSuccessResponse payload, Optional<SdxContext> flowContext, Exception ex) {
                return new SaltUpdateFailureResponse(payload.getResourceId(), payload.getUserId(), ex);
            }

            @Override
            protected Selectable createRequest(SdxContext context) {
                return new SdxEvent(SALT_UPDATE_FINISHED_EVENT.event(), context.getSdxId(), context.getUserId());
            }
        };
    }

    @Bean(name = "SALT_UPDATE_FAILED_STATE")
    public Action<?, ?> rotateSaltPasswordFailed() {
        return new AbstractSdxAction<>(SaltUpdateFailureResponse.class) {

            @Override
            protected void doExecute(SdxContext context, SaltUpdateFailureResponse payload, Map<Object, Object> variables) throws Exception {
                LOGGER.warn("Salt update for SDX stack {} failed", context.getSdxId());
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.SALT_UPDATE_FAILED,
                        Collections.singleton(payload.getException().getMessage()), "SaltStack update failed", context.getSdxId());
                sendEvent(context);
            }

            @Override
            protected Object getFailurePayload(SaltUpdateFailureResponse payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }

            @Override
            protected Selectable createRequest(SdxContext context) {
                return new SdxEvent(SALT_UPDATE_FAIL_HANDLED_EVENT.event(), context.getSdxId(), context.getUserId());
            }
        };
    }
}

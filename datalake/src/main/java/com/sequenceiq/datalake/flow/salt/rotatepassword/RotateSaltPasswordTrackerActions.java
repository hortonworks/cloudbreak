package com.sequenceiq.datalake.flow.salt.rotatepassword;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.salt.rotatepassword.event.RotateSaltPasswordFailureResponse;
import com.sequenceiq.datalake.flow.salt.rotatepassword.event.RotateSaltPasswordWaitRequest;
import com.sequenceiq.datalake.flow.salt.rotatepassword.event.RotateSaltPasswordSuccessResponse;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;

@Configuration
public class RotateSaltPasswordTrackerActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(RotateSaltPasswordTrackerActions.class);

    @Inject
    private SdxStatusService sdxStatusService;

    @Bean(name = "ROTATE_SALT_PASSWORD_WAITING_STATE")
    public Action<?, ?> rotateSaltPasswordTracking() {
        return new AbstractSdxAction<>(SdxEvent.class) {

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Waiting for rotate salt password for SDX stack {}", context.getSdxId());
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.SALT_PASSWORD_ROTATION_IN_PROGRESS,
                        "Rotating SaltStack user password", context.getSdxId());
                sendEvent(context);
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return new RotateSaltPasswordFailureResponse(payload.getResourceId(), payload.getUserId(), ex);
            }

            @Override
            protected Selectable createRequest(SdxContext context) {
                return new RotateSaltPasswordWaitRequest(context.getSdxId(), context.getUserId());
            }
        };
    }

    @Bean(name = "ROTATE_SALT_PASSWORD_SUCCESS_STATE")
    public Action<?, ?> rotateSaltPasswordFinished() {
        return new AbstractSdxAction<>(RotateSaltPasswordSuccessResponse.class) {

            @Override
            protected void doExecute(SdxContext context, RotateSaltPasswordSuccessResponse payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Rotating salt password for SDX stack {} finished", context.getSdxId());
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.SALT_PASSWORD_ROTATION_FINISHED,
                        "Rotated SaltStack user password successfully", context.getSdxId());
                sendEvent(context);
            }

            @Override
            protected Object getFailurePayload(RotateSaltPasswordSuccessResponse payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }

            @Override
            protected Selectable createRequest(SdxContext context) {
                return new SdxEvent(RotateSaltPasswordTrackerEvent.ROTATE_SALT_PASSWORD_FINISHED_EVENT.event(), context.getSdxId(), context.getUserId());
            }
        };
    }

    @Bean(name = "ROTATE_SALT_PASSWORD_FAILED_STATE")
    public Action<?, ?> rotateSaltPasswordFailed() {
        return new AbstractSdxAction<>(RotateSaltPasswordFailureResponse.class) {

            @Override
            protected void doExecute(SdxContext context, RotateSaltPasswordFailureResponse payload, Map<Object, Object> variables) throws Exception {
                LOGGER.warn("Rotating salt password for SDX stack {} failed", context.getSdxId());
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.SALT_PASSWORD_ROTATION_FAILED,
                        Collections.singleton(payload.getException().getMessage()), "Rotating SaltStack user password failed", context.getSdxId());
                sendEvent(context);
            }

            @Override
            protected Object getFailurePayload(RotateSaltPasswordFailureResponse payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }

            @Override
            protected Selectable createRequest(SdxContext context) {
                return new SdxEvent(RotateSaltPasswordTrackerEvent.ROTATE_SALT_PASSWORD_FAIL_HANDLED_EVENT.event(), context.getSdxId(), context.getUserId());
            }
        };
    }
}

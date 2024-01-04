package com.sequenceiq.cloudbreak.core.flow2.cluster.salt.rotatepassword;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SALT_PASSWORD_ROTATE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SALT_PASSWORD_ROTATE_FINISHED;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateService;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.StackUpdaterService;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RotateSaltPasswordFailureResponse;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RotateSaltPasswordRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RotateSaltPasswordSuccessResponse;
import com.sequenceiq.cloudbreak.service.salt.RotateSaltPasswordService;

@Configuration
public class RotateSaltPasswordActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(RotateSaltPasswordActions.class);

    @Inject
    private SaltUpdateService saltUpdateService;

    @Inject
    private StackUpdaterService stackUpdaterService;

    @Inject
    private RotateSaltPasswordService rotateSaltPasswordService;

    @Bean(name = "ROTATE_SALT_PASSWORD_STATE")
    public Action<?, ?> rotateSaltPassword() {
        return new RotateSaltPasswordAction<>(RotateSaltPasswordRequest.class) {

            @Override
            protected void prepareExecution(RotateSaltPasswordRequest payload, Map<Object, Object> variables) {
                super.prepareExecution(payload, variables);
                variables.put(REASON, payload.getReason());
                variables.put(TYPE, payload.getType());
            }

            @Override
            protected void doExecute(RotateSaltPasswordContext context, RotateSaltPasswordRequest payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Rotating salt password for stack {}", context.getStack().getResourceCrn());
                saltUpdateService.rotateSaltPassword(context.getStackId(), context.getType());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(RotateSaltPasswordContext context) {
                return new RotateSaltPasswordRequest(context.getStack().getId(), context.getReason(), context.getType());
            }
        };
    }

    @Bean(name = "ROTATE_SALT_PASSWORD_SUCCESS_STATE")
    public Action<?, ?> rotateSaltPasswordFinished() {
        return new RotateSaltPasswordAction<>(RotateSaltPasswordSuccessResponse.class) {

            @Override
            protected void doExecute(RotateSaltPasswordContext context, RotateSaltPasswordSuccessResponse payload, Map<Object, Object> variables)
                    throws Exception {
                LOGGER.info("Rotating salt password for stack {} finished, restoring previous status: {}",
                        context.getStack().getResourceCrn(), context.getPreviousStackStatus());
                String statusReason = "SaltStack user password rotated, restored previous status";
                String previousStatusReason = context.getPreviousStackStatus().getStatusReason();
                if (!previousStatusReason.isBlank()) {
                    if (previousStatusReason.startsWith(statusReason)) {
                        statusReason = previousStatusReason;
                    } else {
                        statusReason += ": " + previousStatusReason;
                    }
                }
                stackUpdaterService.updateStatus(payload.getResourceId(), context.getPreviousStackStatus().getDetailedStackStatus(),
                        CLUSTER_SALT_PASSWORD_ROTATE_FINISHED, statusReason);
                rotateSaltPasswordService.sendSuccessUsageReport(context.getStack().getResourceCrn(), context.getReason());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(RotateSaltPasswordContext context) {
                return new StackEvent(RotateSaltPasswordEvent.ROTATE_SALT_PASSWORD_FINISHED_EVENT.event(), context.getStackId());
            }
        };
    }

    @Bean(name = "ROTATE_SALT_PASSWORD_FAILED_STATE")
    public Action<?, ?> rotateSaltPasswordFailed() {
        return new RotateSaltPasswordAction<>(RotateSaltPasswordFailureResponse.class) {

            @Override
            protected void doExecute(RotateSaltPasswordContext context, RotateSaltPasswordFailureResponse payload, Map<Object, Object> variables)
                    throws Exception {
                LOGGER.warn("Rotating salt password for stack {} failed", context.getStack().getResourceCrn());
                stackUpdaterService.updateStatusAndSendEventWithArgs(payload.getResourceId(), DetailedStackStatus.SALT_UPDATE_FAILED,
                        CLUSTER_SALT_PASSWORD_ROTATE_FAILED, "Failed to rotate SaltStack user password", payload.getException().getMessage());
                rotateSaltPasswordService.sendFailureUsageReport(context.getStack().getResourceCrn(), context.getReason(),
                        payload.getException().getMessage());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(RotateSaltPasswordContext context) {
                return new StackEvent(RotateSaltPasswordEvent.ROTATE_SALT_PASSWORD_FAIL_HANDLED_EVENT.event(), context.getStackId());
            }
        };
    }

}

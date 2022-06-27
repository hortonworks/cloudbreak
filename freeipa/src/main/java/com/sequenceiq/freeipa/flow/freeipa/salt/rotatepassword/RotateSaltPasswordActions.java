package com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.AVAILABLE;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.SALT_STATE_UPDATE_FAILED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.SALT_STATE_UPDATE_IN_PROGRESS;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.event.RotateSaltPasswordFailureResponse;
import com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.event.RotateSaltPasswordRequest;
import com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.event.RotateSaltPasswordSuccessResponse;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.provision.action.AbstractStackProvisionAction;
import com.sequenceiq.freeipa.orchestrator.RotateSaltPasswordService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@Configuration
public class RotateSaltPasswordActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(RotateSaltPasswordActions.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private RotateSaltPasswordService rotateSaltPasswordService;

    @Bean(name = "ROTATE_SALT_PASSWORD_STATE")
    public Action<?, ?> rotateSaltPassword() {
        return new AbstractStackProvisionAction<>(StackEvent.class) {

            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Rotating salt password for freeipa stack {}", context.getStack().getResourceCrn());
                stackUpdater.updateStackStatus(payload.getResourceId(), SALT_STATE_UPDATE_IN_PROGRESS, "Rotating SaltStack user password");
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new RotateSaltPasswordRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "ROTATE_SALT_PASSWORD_SUCCESS_STATE")
    public Action<?, ?> rotateSaltPasswordFinished() {
        return new AbstractStackProvisionAction<>(RotateSaltPasswordSuccessResponse.class) {

            @Override
            protected void doExecute(StackContext context, RotateSaltPasswordSuccessResponse payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Rotating salt password for freeipa stack {} finished", context.getStack().getResourceCrn());
                stackUpdater.updateStackStatus(payload.getResourceId(), AVAILABLE, "SaltStack user password rotated");
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new StackEvent(RotateSaltPasswordEvent.ROTATE_SALT_PASSWORD_FINISHED_EVENT.event(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "ROTATE_SALT_PASSWORD_FAILED_STATE")
    public Action<?, ?> rotateSaltPasswordFailed() {
        return new AbstractStackProvisionAction<>(RotateSaltPasswordFailureResponse.class) {

            @Override
            protected void doExecute(StackContext context, RotateSaltPasswordFailureResponse payload, Map<Object, Object> variables) throws Exception {
                LOGGER.warn("Rotating salt password for freeipa stack {} failed", context.getStack().getResourceCrn());
                stackUpdater.updateStackStatus(payload.getResourceId(), SALT_STATE_UPDATE_FAILED, "Failed to rotate SaltStack user password");
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new StackEvent(RotateSaltPasswordEvent.ROTATE_SALT_PASSWORD_FAIL_HANDLED_EVENT.event(), context.getStack().getId());
            }
        };
    }
}

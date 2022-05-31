package com.sequenceiq.cloudbreak.core.flow2.cluster.salt.rotatepassword;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SALT_PASSWORD_ROTATE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SALT_PASSWORD_ROTATE_FINISHED;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateService;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.StackUpdaterService;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.action.AbstractStackCreationAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackCreationContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RotateSaltPasswordFailureResponse;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RotateSaltPasswordRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RotateSaltPasswordSuccessResponse;

@Configuration
public class RotateSaltPasswordActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(RotateSaltPasswordActions.class);

    @Inject
    private SaltUpdateService saltUpdateService;

    @Inject
    private StackUpdaterService stackUpdaterService;

    @Bean(name = "ROTATE_SALT_PASSWORD_STATE")
    public Action<?, ?> rotateSaltPassword() {
        return new AbstractStackCreationAction<>(StackEvent.class) {

            @Override
            protected void doExecute(StackCreationContext context, StackEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Rotating salt password for stack {}", context.getStack().getResourceCrn());
                saltUpdateService.rotateSaltPassword(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                return new RotateSaltPasswordRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "ROTATE_SALT_PASSWORD_SUCCESS_STATE")
    public Action<?, ?> rotateSaltPasswordFinished() {
        return new AbstractStackCreationAction<>(RotateSaltPasswordSuccessResponse.class) {

            @Override
            protected void doExecute(StackCreationContext context, RotateSaltPasswordSuccessResponse payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Rotating salt password for stack {} finished", context.getStack().getResourceCrn());
                stackUpdaterService.updateStatus(payload.getResourceId(), DetailedStackStatus.AVAILABLE,
                        CLUSTER_SALT_PASSWORD_ROTATE_FINISHED, "Salt password rotated");
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                return new StackEvent(RotateSaltPasswordEvent.ROTATE_SALT_PASSWORD_FINISHED_EVENT.event(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "ROTATE_SALT_PASSWORD_FAILED_STATE")
    public Action<?, ?> rotateSaltPasswordFailed() {
        return new AbstractStackCreationAction<>(RotateSaltPasswordFailureResponse.class) {

            @Override
            protected void doExecute(StackCreationContext context, RotateSaltPasswordFailureResponse payload, Map<Object, Object> variables) throws Exception {
                LOGGER.warn("Rotating salt password for stack {} failed", context.getStack().getResourceCrn());
                stackUpdaterService.updateStatusAndSendEventWithArgs(payload.getResourceId(), DetailedStackStatus.SALT_UPDATE_FAILED,
                        CLUSTER_SALT_PASSWORD_ROTATE_FAILED, "Failed to rotate salt password", payload.getException().getMessage());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                return new StackEvent(RotateSaltPasswordEvent.ROTATE_SALT_PASSWORD_FAIL_HANDLED_EVENT.event(), context.getStack().getId());
            }
        };
    }

}

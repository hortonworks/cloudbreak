package com.sequenceiq.freeipa.flow.freeipa.salt.update;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.bootstrap.BootstrapMachinesRequest;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.bootstrap.BootstrapMachinesSuccess;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.orchestrator.OrchestratorConfigRequest;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.orchestrator.OrchestratorConfigSuccess;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.services.InstallFreeIpaServicesRequest;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.services.InstallFreeIpaServicesSuccess;
import com.sequenceiq.freeipa.flow.stack.AbstractStackFailureAction;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.StackFailureContext;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.flow.stack.provision.action.AbstractStackProvisionAction;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@Configuration
public class SaltUpdateActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltUpdateActions.class);

    @Inject
    private StackUpdater stackUpdater;

    @Bean(name = "UPDATE_SALT_STATE_FILES_STATE")
    public Action<?, ?> updateSaltFilesAction() {
        return new AbstractStackProvisionAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                stackUpdater.updateStackStatus(payload.getResourceId(), DetailedStackStatus.SALT_STATE_UPDATE_IN_PROGRESS, "Salt state update in progress");
                LOGGER.info("Reupload salt state files");
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new BootstrapMachinesRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "UPDATE_ORCHESTRATOR_CONFIG_STATE")
    public Action<?, ?> orchestratorConfig() {
        return new AbstractStackProvisionAction<>(BootstrapMachinesSuccess.class) {

            @Override
            protected void doExecute(StackContext context, BootstrapMachinesSuccess payload, Map<Object, Object> variables) {
                LOGGER.info("Reupload pillar files");
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new OrchestratorConfigRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "RUN_HIGHSTATE_STATE")
    public Action<?, ?> runHighstateAction() {
        return new AbstractStackProvisionAction<>(OrchestratorConfigSuccess.class) {
            @Override
            protected void doExecute(StackContext context, OrchestratorConfigSuccess payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Run highstate");
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new InstallFreeIpaServicesRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "SALT_UPDATE_FINISHED_STATE")
    public Action<?, ?> saltUpdateFinishedAction() {
        return new AbstractStackProvisionAction<>(InstallFreeIpaServicesSuccess.class) {
            @Override
            protected void doExecute(StackContext context, InstallFreeIpaServicesSuccess payload, Map<Object, Object> variables) {
                stackUpdater.updateStackStatus(payload.getResourceId(), DetailedStackStatus.AVAILABLE, "Salt update finished");
                LOGGER.info("Salt state update finished successfully");
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new StackEvent(SaltUpdateEvent.SALT_UPDATE_FINISHED_EVENT.event(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "SALT_UPDATE_FAILED_STATE")
    public Action<?, ?> saltUpdateFailedAction() {
        return new AbstractStackFailureAction<SaltUpdateState, SaltUpdateEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                LOGGER.error("Salt state update failed", payload.getException());
                stackUpdater.updateStackStatus(payload.getResourceId(), DetailedStackStatus.SALT_STATE_UPDATE_FAILED,
                        "Salt update failed with: " + payload.getException().getMessage());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(SaltUpdateEvent.SALT_UPDATE_FAILURE_HANDLED_EVENT.event(), context.getStack().getId());
            }
        };
    }
}

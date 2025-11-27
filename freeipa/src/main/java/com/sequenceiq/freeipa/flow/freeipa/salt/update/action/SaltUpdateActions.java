package com.sequenceiq.freeipa.flow.freeipa.salt.update.action;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.bootstrap.BootstrapMachinesSuccess;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.orchestrator.OrchestratorConfigRequest;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.orchestrator.OrchestratorConfigSuccess;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.services.InstallFreeIpaServicesRequest;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.services.InstallFreeIpaServicesSuccess;
import com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateTriggerEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.provision.action.AbstractStackProvisionAction;

@Configuration
public class SaltUpdateActions {

    public static final String SKIP_HIGHSTATE = "skipHighstate";

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltUpdateActions.class);

    @Bean(name = "UPDATE_SALT_STATE_FILES_STATE")
    public Action<?, ?> updateSaltFilesAction() {
        return new UpdateSaltFilesAction(SaltUpdateTriggerEvent.class);
    }

    @Bean(name = "UPDATE_ORCHESTRATOR_CONFIG_STATE")
    public Action<?, ?> orchestratorConfig() {
        return new AbstractStackProvisionAction<>(BootstrapMachinesSuccess.class) {

            @Override
            protected void doExecute(StackContext context, BootstrapMachinesSuccess payload, Map<Object, Object> variables) {
                LOGGER.info("Reupload pillar files");
                if (skipHighstate(variables)) {
                    sendEvent(context, new InstallFreeIpaServicesSuccess(context.getStack().getId()));
                } else {
                    sendEvent(context, new OrchestratorConfigRequest(context.getStack().getId()));
                }
            }

            private static Boolean skipHighstate(Map<Object, Object> variables) {
                return Optional.ofNullable(variables.get(SKIP_HIGHSTATE))
                        .map(Boolean.class::cast)
                        .orElse(Boolean.FALSE);
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
        return new SaltUpdateFinishedAction();
    }

    @Bean(name = "SALT_UPDATE_FAILED_STATE")
    public Action<?, ?> saltUpdateFailedAction() {
        return new SaltUpdateFailureAction();
    }

}

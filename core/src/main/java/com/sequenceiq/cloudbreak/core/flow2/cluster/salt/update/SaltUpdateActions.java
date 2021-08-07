package com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.AbstractClusterAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.action.AbstractStackCreationAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackCreationContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.kerberos.KeytabConfigurationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.kerberos.KeytabConfigurationSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.BootstrapMachinesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.BootstrapMachinesSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StartAmbariServicesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StartClusterManagerServicesSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadRecipesSuccess;

@Configuration
public class SaltUpdateActions {

    @Inject
    private SaltUpdateService saltUpdateService;

    @Bean(name = "UPDATE_SALT_STATE_FILES_STATE")
    public Action<?, ?> updateSaltFilesAction() {
        return new AbstractStackCreationAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackCreationContext context, StackEvent payload, Map<Object, Object> variables) {
                saltUpdateService.bootstrappingMachines(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                return new BootstrapMachinesRequest(context.getStack().getId(), true);
            }
        };
    }

    @Bean(name = "UPLOAD_RECIPES_FOR_SU_STATE")
    public Action<?, ?> uploadRecipesAction() {
        return new AbstractClusterAction<>(BootstrapMachinesSuccess.class) {
            @Override
            protected void doExecute(ClusterViewContext context, BootstrapMachinesSuccess payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new UploadRecipesRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "RECONFIGURE_KEYTABS_FOR_SU_STATE")
    public Action<?, ?> configureKeytabsAction() {
        return new AbstractClusterAction<>(UploadRecipesSuccess.class) {
            @Override
            protected void doExecute(ClusterViewContext context, UploadRecipesSuccess payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new KeytabConfigurationRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "RUN_HIGHSTATE_STATE")
    public Action<?, ?> runHighstateAction() {
        return new AbstractClusterAction<>(KeytabConfigurationSuccess.class) {
            @Override
            protected void doExecute(ClusterViewContext context, KeytabConfigurationSuccess payload, Map<Object, Object> variables) throws Exception {
                saltUpdateService.startingClusterServices(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new StartAmbariServicesRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "SALT_UPDATE_FINISHED_STATE")
    public Action<?, ?> saltUpdateFinishedAction() {
        return new AbstractClusterAction<>(StartClusterManagerServicesSuccess.class) {
            @Override
            protected void doExecute(ClusterViewContext context, StartClusterManagerServicesSuccess payload, Map<Object, Object> variables) {
                saltUpdateService.clusterInstallationFinished(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new StackEvent(SaltUpdateEvent.SALT_UPDATE_FINISHED_EVENT.event(), context.getStackId());
            }
        };
    }

    @Bean(name = "SALT_UPDATE_FAILED_STATE")
    public Action<?, ?> saltUpdateFailedAction() {
        return new AbstractStackFailureAction<ClusterCreationState, ClusterCreationEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                saltUpdateService.handleClusterCreationFailure(context.getStackView(), payload.getException());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(SaltUpdateEvent.SALT_UPDATE_FAILURE_HANDLED_EVENT.event(), context.getStackView().getId());
            }
        };
    }
}

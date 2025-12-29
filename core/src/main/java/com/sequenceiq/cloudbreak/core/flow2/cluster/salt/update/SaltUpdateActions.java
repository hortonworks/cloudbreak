package com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

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
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.SaltUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StartAmbariServicesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StartClusterManagerServicesSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadRecipesSuccess;
import com.sequenceiq.cloudbreak.util.SaltUpdateSkipHighstateFlagUtil;
import com.sequenceiq.flow.core.PayloadConverter;

@Configuration
public class SaltUpdateActions {

    @Inject
    private SaltUpdateService saltUpdateService;

    @Bean(name = "UPDATE_SALT_STATE_FILES_STATE")
    public Action<?, ?> updateSaltFilesAction() {
        return new AbstractStackCreationAction<>(SaltUpdateTriggerEvent.class) {

            @Override
            protected void prepareExecution(SaltUpdateTriggerEvent payload, Map<Object, Object> variables) {
                SaltUpdateSkipHighstateFlagUtil.putToVariables(payload.isSkipHighstate(), variables);
            }

            @Override
            protected void doExecute(StackCreationContext context, SaltUpdateTriggerEvent payload, Map<Object, Object> variables) {
                saltUpdateService.bootstrappingMachines(context.getStackId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                return new BootstrapMachinesRequest(context.getStackId(), true);
            }

            @Override
            protected void initPayloadConverterMap(List<PayloadConverter<SaltUpdateTriggerEvent>> payloadConverters) {
                payloadConverters.add(new StackEventToSaltUpdateTriggerEventConverter());
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
                sendEvent(context, new KeytabConfigurationRequest(context.getStackId(), Boolean.FALSE));
            }
        };
    }

    @Bean(name = "RUN_HIGHSTATE_STATE")
    public Action<?, ?> runHighstateAction() {
        return new AbstractClusterAction<>(KeytabConfigurationSuccess.class) {
            @Override
            protected void doExecute(ClusterViewContext context, KeytabConfigurationSuccess payload, Map<Object, Object> variables) {
                if (SaltUpdateSkipHighstateFlagUtil.getFromVariables(variables)) {
                    sendEvent(context, new StartClusterManagerServicesSuccess(context.getStackId()));
                } else {
                    saltUpdateService.startingClusterServices(context.getStackId());
                    sendEvent(context, new StartAmbariServicesRequest(context.getStackId(), false, false));
                }
            }
        };
    }

    @Bean(name = "SALT_UPDATE_FINISHED_STATE")
    public Action<?, ?> saltUpdateFinishedAction() {
        return new AbstractClusterAction<>(StartClusterManagerServicesSuccess.class) {
            @Override
            protected void doExecute(ClusterViewContext context, StartClusterManagerServicesSuccess payload, Map<Object, Object> variables) {
                saltUpdateService.clusterInstallationFinished(context.getStackId());
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
                saltUpdateService.handleClusterCreationFailure(context.getStack(), payload.getException());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(SaltUpdateEvent.SALT_UPDATE_FAILURE_HANDLED_EVENT.event(), context.getStackId());
            }
        };
    }
}

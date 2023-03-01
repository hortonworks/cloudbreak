package com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update;

import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.action.AbstractStackCreationAction.PROVISION_TYPE;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
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
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
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
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ProvisionType;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.service.flowlog.FlowChainLogService;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;

@Configuration
public class SaltUpdateActions {

    @Inject
    private SaltUpdateService saltUpdateService;

    @Inject
    private FlowChainLogService flowChainLogService;

    @Inject
    private FlowLogDBService flowLogDBService;

    @Bean(name = "UPDATE_SALT_STATE_FILES_STATE")
    public Action<?, ?> updateSaltFilesAction() {
        return new AbstractStackCreationAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackCreationContext context, StackEvent payload, Map<Object, Object> variables) {
                saltUpdateService.bootstrappingMachines(context.getStackId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                return new BootstrapMachinesRequest(context.getStackId(), true);
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
                return new KeytabConfigurationRequest(context.getStackId(), Boolean.FALSE);
            }
        };
    }

    @Bean(name = "RUN_HIGHSTATE_STATE")
    public Action<?, ?> runHighstateAction() {
        return new AbstractClusterAction<>(KeytabConfigurationSuccess.class) {
            @Override
            protected void doExecute(ClusterViewContext context, KeytabConfigurationSuccess payload, Map<Object, Object> variables) {
                saltUpdateService.startingClusterServices(context.getStackId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new StartAmbariServicesRequest(context.getStackId(),
                        false, false);
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

            @Inject
            private StackDtoService stackDtoService;

            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                Boolean checkIfBackupDatalakeDatabaseFlowChain = isBackupDatalakeDatabaseFlowChain(payload.getResourceId());
                saltUpdateService.handleClusterCreationFailure(context.getStack(), payload.getException(), context.getStackId(),
                        checkIfBackupDatalakeDatabaseFlowChain);
                sendEvent(context);
            }

            @Override
            protected StackFailureContext createFlowContext(FlowParameters flowParameters,
                StateContext<ClusterCreationState, ClusterCreationEvent> stateContext, StackFailureEvent payload) {
                if (isBackupDatalakeDatabaseFlowChain(payload.getResourceId())) {
                    StackView stack = stackDtoService.getStackViewById(payload.getResourceId());
                    MDCBuilder.buildMdcContext(stack);
                    Map<Object, Object> variables = stateContext.getExtendedState().getVariables();
                    ProvisionType provisionType = (ProvisionType) variables.getOrDefault(PROVISION_TYPE, ProvisionType.REGULAR);
                    return new StackFailureContext(flowParameters, stack, stack.getId(), provisionType);
                } else {
                    return (super.createFlowContext(flowParameters, stateContext, payload));
                }
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(SaltUpdateEvent.SALT_UPDATE_FAILURE_HANDLED_EVENT.event(), context.getStackId());
            }

            private Boolean isBackupDatalakeDatabaseFlowChain(Long resourceId) {
                String flowChainType = "";
                List<FlowLog> flowLogs = flowLogDBService.findAllByResourceIdOrderByCreatedDesc(resourceId);
                if (!flowLogs.isEmpty()) {
                    FlowLog latestFlowLog = flowLogs.iterator().next();
                    String flowChainId = latestFlowLog.getFlowChainId();
                    flowChainType = flowChainLogService.getFlowChainType(flowChainId);
                }
                return "BackupDatalakeDatabaseFlowEventChainFactory".equals(flowChainType);
            }
        };
    }
}

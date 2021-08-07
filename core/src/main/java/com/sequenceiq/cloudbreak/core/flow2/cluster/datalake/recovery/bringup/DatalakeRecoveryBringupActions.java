package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.recovery.bringup;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.recovery.bringup.DatalakeRecoveryBringupEvent.RECOVERY_BRINGUP_FAIL_HANDLED_EVENT;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterRecoveryTriggerEvent;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.recovery.bringup.DatalakeRecoverySetupNewInstancesFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.recovery.bringup.DatalakeRecoverySetupNewInstancesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.recovery.bringup.DatalakeRecoverySetupNewInstancesSuccess;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

@Configuration
public class DatalakeRecoveryBringupActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeRecoveryBringupActions.class);

    @Inject
    private DatalakeRecoveryBringupService datalakeRecoveryBringupStatusService;

    @Inject
    private StackService stackService;

    @Bean(name = "RECOVERY_SETUP_NEW_INSTANCES_STATE")
    public Action<?, ?> datalakeRecoverySetupNewInstances() {
        return new AbstractDatalakeRecoveryBringupAction<>(ClusterRecoveryTriggerEvent.class) {

            @Override
            protected void doExecute(DatalakeRecoveryBringupContext context, ClusterRecoveryTriggerEvent payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(DatalakeRecoveryBringupContext context) {
                return new DatalakeRecoverySetupNewInstancesRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "RECOVERY_BRINGUP_FINISHED_STATE")
    public Action<?, ?> datalakeRecoveryBringupFinished() {
        return new AbstractDatalakeRecoveryBringupAction<>(DatalakeRecoverySetupNewInstancesSuccess.class) {

            @Override
            protected void doExecute(DatalakeRecoveryBringupContext context, DatalakeRecoverySetupNewInstancesSuccess payload, Map<Object, Object> variables) {
                datalakeRecoveryBringupStatusService.handleDatalakeRecoveryBringupSuccess(context.getStackId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(DatalakeRecoveryBringupContext context) {
                return new StackEvent(DatalakeRecoveryBringupEvent.RECOVERY_BRINGUP_FINALIZED_EVENT.event(), context.getStackId());
            }
        };
    }

    @Bean(name = "RECOVERY_BRINGUP_FAILED_STATE")
    public Action<?, ?> datalakeRecoveryBringupFailedAction() {
        return new AbstractDatalakeRecoveryBringupAction<>(DatalakeRecoverySetupNewInstancesFailedEvent.class) {

            @Override
            protected DatalakeRecoveryBringupContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatalakeRecoverySetupNewInstancesFailedEvent payload) {
                Exception exception = payload.getException();
                Long stackId = payload.getResourceId();
                Flow flow = getFlow(flowParameters.getFlowId());
                StackView stackView = stackService.getViewByIdWithoutAuth(stackId);
                MDCBuilder.buildMdcContext(stackView);
                flow.setFlowFailed(exception);
                LOGGER.error("Datalake recovery failed for stack with id: {}", stackId, exception);
                return DatalakeRecoveryBringupContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(DatalakeRecoveryBringupContext context,
                    DatalakeRecoverySetupNewInstancesFailedEvent payload, Map<Object, Object> variables) {
                datalakeRecoveryBringupStatusService.handleDatalakeRecoveryBringupFailure(context.getStackId(),
                        payload.getException().getMessage(), payload.getDetailedStatus());
                sendEvent(context, RECOVERY_BRINGUP_FAIL_HANDLED_EVENT.event(), payload);
            }
        };
    }
}

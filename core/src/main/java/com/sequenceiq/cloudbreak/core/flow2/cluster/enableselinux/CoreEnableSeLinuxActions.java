package com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event.CoreEnableSeLinuxHandlerSelectors.ENABLE_SELINUX_CORE_HANDLER;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event.CoreEnableSeLinuxHandlerSelectors.ENABLE_SELINUX_CORE_VALIDATION_HANDLER;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event.CoreEnableSeLinuxStateSelectors.FINALIZE_ENABLE_SELINUX_CORE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event.CoreEnableSeLinuxStateSelectors.HANDLED_FAILED_ENABLE_SELINUX_CORE_EVENT;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event.CoreEnableSeLinuxEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event.CoreEnableSeLinuxFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event.CoreEnableSeLinuxStateSelectors;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;

@Configuration
public class CoreEnableSeLinuxActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreEnableSeLinuxActions.class);

    public CoreEnableSeLinuxActions() {
    }

    @Bean(name = "ENABLE_SELINUX_CORE_VALIDATION_STATE")
    public Action<?, ?> enableSeLinuxValidationAction() {
        return new AbstractCoreEnableSeLinuxAction<>(CoreEnableSeLinuxEvent.class) {
            @Override
            protected void doExecute(CommonContext context, CoreEnableSeLinuxEvent payload, Map<Object, Object> variables) {
                sendEvent(context, ENABLE_SELINUX_CORE_VALIDATION_HANDLER.selector(), payload);
            }
        };
    }

    @Bean(name = "ENABLE_SELINUX_CORE_STATE")
    public Action<?, ?> enableSeLinuxInFreeIpaAction() {
        return new AbstractCoreEnableSeLinuxAction<>(CoreEnableSeLinuxEvent.class) {
            @Override
            protected void doExecute(CommonContext context, CoreEnableSeLinuxEvent payload, Map<Object, Object> variables) {
                sendEvent(context, ENABLE_SELINUX_CORE_HANDLER.selector(), payload);
            }
        };
    }

    @Bean(name = "ENABLE_SELINUX_CORE_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractCoreEnableSeLinuxAction<>(CoreEnableSeLinuxEvent.class) {
            @Override
            protected void doExecute(CommonContext context, CoreEnableSeLinuxEvent payload, Map<Object, Object> variables) {
                getMetricService().incrementMetricCounter(MetricType.ENABLE_SELINUX_SUCCESSFUL, payload.getResourceName());
                sendEvent(context, FINALIZE_ENABLE_SELINUX_CORE_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "ENABLE_SELINUX_CORE_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractCoreEnableSeLinuxAction<>(CoreEnableSeLinuxFailedEvent.class) {

            @Override
            protected CommonContext createFlowContext(FlowParameters flowParameters, StateContext<CoreEnableSeLinuxState,
                CoreEnableSeLinuxStateSelectors> stateContext, CoreEnableSeLinuxFailedEvent payload) {
                Flow flow = getFlow(flowParameters.getFlowId());
                flow.setFlowFailed(payload.getException());
                return super.createFlowContext(flowParameters, stateContext, payload);
            }

            @Override
            protected void doExecute(CommonContext context, CoreEnableSeLinuxFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.error(String.format("Failed to Enable SeLinux on Stack '%s'.", payload.getResourceId()), payload.getException());
                getMetricService().incrementMetricCounter(MetricType.ENABLE_SELINUX_FAILED, payload.getException().getMessage());
                sendEvent(context, HANDLED_FAILED_ENABLE_SELINUX_CORE_EVENT.event(), payload);
            }
        };
    }

}

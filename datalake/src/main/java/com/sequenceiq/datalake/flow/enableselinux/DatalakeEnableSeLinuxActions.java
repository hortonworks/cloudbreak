package com.sequenceiq.datalake.flow.enableselinux;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_ENABLE_SELINUX_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_ENABLE_SELINUX_VALIDATION_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_ENABLE_SELINUX_ON_DATALAKE_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_ENABLE_SELINUX_ON_DATALAKE_IN_PROGRESS;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_ENABLE_SELINUX_VALIDATION_IN_PROGRESS;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.RUNNING;
import static com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxHandlerSelectors.ENABLE_SELINUX_DATALAKE_HANDLER;
import static com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxHandlerSelectors.ENABLE_SELINUX_DATALAKE_VALIDATION_HANDLER;
import static com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxStateSelectors.FINALIZE_ENABLE_SELINUX_DATALAKE_EVENT;
import static com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxStateSelectors.HANDLED_FAILED_ENABLE_SELINUX_DATALAKE_EVENT;
import static com.sequenceiq.datalake.metric.MetricType.SDX_ENABLE_SELINUX_FAILED;
import static com.sequenceiq.datalake.metric.MetricType.SDX_ENABLE_SELINUX_FINISHED;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxEvent;
import com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxFailedEvent;
import com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxStateSelectors;
import com.sequenceiq.datalake.metric.SdxMetricService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;

@Configuration
public class DatalakeEnableSeLinuxActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeEnableSeLinuxActions.class);

    private final SdxStatusService sdxStatusService;

    private SdxMetricService metricService;

    public DatalakeEnableSeLinuxActions(SdxStatusService sdxStatusService, SdxMetricService metricService) {
        this.sdxStatusService = sdxStatusService;
        this.metricService = metricService;
    }

    @Bean(name = "ENABLE_SELINUX_DATALAKE_VALIDATION_STATE")
    public Action<?, ?> enableSeLinuxValidationAction() {
        return new AbstractDatalakeEnableSeLinuxAction<>(DatalakeEnableSeLinuxEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DatalakeEnableSeLinuxEvent payload, Map<Object, Object> variables) {
                sdxStatusService.setStatusForDatalakeAndNotifyWithStatusReason(
                        DATALAKE_ENABLE_SELINUX_VALIDATION_IN_PROGRESS,
                        String.format("Validation of enable selinux is in progress of %s on the Data Lake.",
                                payload.getResourceName()),
                        payload.getResourceId());
                sendEvent(context, ENABLE_SELINUX_DATALAKE_VALIDATION_HANDLER.selector(), payload);
            }
        };
    }

    @Bean(name = "ENABLE_SELINUX_DATALAKE_STATE")
    public Action<?, ?> enableSeLinuxInDatalakeAction() {
        return new AbstractDatalakeEnableSeLinuxAction<>(DatalakeEnableSeLinuxEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DatalakeEnableSeLinuxEvent payload, Map<Object, Object> variables) {
                sdxStatusService.setStatusForDatalakeAndNotifyWithStatusReason(
                        DATALAKE_ENABLE_SELINUX_ON_DATALAKE_IN_PROGRESS,
                        String.format("Enable SeLinux on the Data Lake."),
                        payload.getResourceId());
                sendEvent(context, ENABLE_SELINUX_DATALAKE_HANDLER.selector(), payload);
            }
        };
    }

    @Bean(name = "ENABLE_SELINUX_DATALAKE_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractDatalakeEnableSeLinuxAction<>(DatalakeEnableSeLinuxEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DatalakeEnableSeLinuxEvent payload, Map<Object, Object> variables) {
                SdxCluster sdxCluster = sdxStatusService.setStatusForDatalakeAndNotify(
                        RUNNING,
                        "Datalake is running",
                        payload.getResourceId());
                metricService.incrementMetricCounter(SDX_ENABLE_SELINUX_FINISHED, sdxCluster);
                sendEvent(context, FINALIZE_ENABLE_SELINUX_DATALAKE_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "ENABLE_SELINUX_DATALAKE_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractDatalakeEnableSeLinuxAction<>(DatalakeEnableSeLinuxFailedEvent.class) {

            @Override
            protected CommonContext createFlowContext(FlowParameters flowParameters, StateContext<DatalakeEnableSeLinuxState,
                DatalakeEnableSeLinuxStateSelectors> stateContext, DatalakeEnableSeLinuxFailedEvent payload) {
                Flow flow = getFlow(flowParameters.getFlowId());
                flow.setFlowFailed(payload.getException());
                return super.createFlowContext(flowParameters, stateContext, payload);
            }

            @Override
            protected void doExecute(CommonContext context, DatalakeEnableSeLinuxFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.error(String.format("Failed to Enable SeLinux in datalake '%s'. Status: '%s'.",
                        payload.getStatus()), payload.getException());
                SdxCluster sdxCluster = sdxStatusService.setStatusForDatalakeAndNotifyWithStatusReason(
                        DATALAKE_ENABLE_SELINUX_ON_DATALAKE_FAILED,
                        String.format("Enable SeLinux failed on the Data Lake."),
                        payload.getResourceId());
                metricService.incrementMetricCounter(SDX_ENABLE_SELINUX_FAILED, sdxCluster);
                sendEvent(context, HANDLED_FAILED_ENABLE_SELINUX_DATALAKE_EVENT.event(), payload);
            }

            private ResourceEvent convertStatus(DatalakeStatusEnum status) {
                switch (status) {
                    case DATALAKE_ENABLE_SELINUX_VALIDATION_FAILED:
                        return DATALAKE_ENABLE_SELINUX_VALIDATION_FAILED;
                    case DATALAKE_ENABLE_SELINUX_ON_DATALAKE_FAILED:
                        return ResourceEvent.DATALAKE_ENABLE_SELINUX_ON_DATALAKE_FAILED;
                    default:
                        return DATALAKE_ENABLE_SELINUX_FAILED;
                }
            }
        };
    }

}

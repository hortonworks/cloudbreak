package com.sequenceiq.datalake.flow.enableselinux;

import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_ENABLE_SELINUX_ON_DATALAKE_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_ENABLE_SELINUX_ON_DATALAKE_IN_PROGRESS;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.RUNNING;
import static com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxStateSelectors.FINALIZE_ENABLE_SELINUX_DATALAKE_EVENT;
import static com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxStateSelectors.HANDLED_FAILED_ENABLE_SELINUX_DATALAKE_EVENT;
import static com.sequenceiq.datalake.metric.MetricType.SDX_ENABLE_SELINUX_FAILED;
import static com.sequenceiq.datalake.metric.MetricType.SDX_ENABLE_SELINUX_FINISHED;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxEvent;
import com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxFailedEvent;
import com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxHandlerEvent;
import com.sequenceiq.datalake.flow.enableselinux.event.DatalakeEnableSeLinuxStateSelectors;
import com.sequenceiq.datalake.metric.SdxMetricService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;

@Configuration
public class DatalakeEnableSeLinuxActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeEnableSeLinuxActions.class);

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private SdxMetricService metricService;

    @Bean(name = "ENABLE_SELINUX_DATALAKE_STATE")
    public Action<?, ?> enableSeLinuxInDatalakeAction() {
        return new AbstractDatalakeEnableSeLinuxAction<>(DatalakeEnableSeLinuxEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DatalakeEnableSeLinuxEvent payload, Map<Object, Object> variables) {
                sdxStatusService.setStatusForDatalakeAndNotifyWithStatusReason(DATALAKE_ENABLE_SELINUX_ON_DATALAKE_IN_PROGRESS,
                        "Enable SELinux on Data Lake.", payload.getResourceId());
                DatalakeEnableSeLinuxHandlerEvent event = new DatalakeEnableSeLinuxHandlerEvent(payload.getResourceId(), payload.getResourceName(),
                        payload.getResourceCrn());
                sendEvent(context, event);
            }
        };
    }

    @Bean(name = "ENABLE_SELINUX_DATALAKE_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractDatalakeEnableSeLinuxAction<>(DatalakeEnableSeLinuxEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DatalakeEnableSeLinuxEvent payload, Map<Object, Object> variables) {
                SdxCluster sdxCluster = sdxStatusService.setStatusForDatalakeAndNotify(
                        RUNNING, "Data Lake SELinux set to 'ENFORCING' complete.", payload.getResourceId());
                metricService.incrementMetricCounter(SDX_ENABLE_SELINUX_FINISHED, sdxCluster);
                DatalakeEnableSeLinuxEvent event = DatalakeEnableSeLinuxEvent.builder().withSelector(FINALIZE_ENABLE_SELINUX_DATALAKE_EVENT.event())
                        .withResourceId(payload.getResourceId()).withResourceCrn(payload.getResourceCrn()).withResourceName(payload.getResourceName()).build();
                sendEvent(context, event);
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
                LOGGER.error(String.format("Failed to Enable SELinux in data lake '%s'. Status: '%s'.",
                        payload.getResourceCrn(), payload.getStatus()), payload.getException());
                SdxCluster sdxCluster = sdxStatusService.setStatusForDatalakeAndNotifyWithStatusReason(
                        DATALAKE_ENABLE_SELINUX_ON_DATALAKE_FAILED,
                        "Enable SELinux failed on the Data Lake.",
                        payload.getResourceId());
                metricService.incrementMetricCounter(SDX_ENABLE_SELINUX_FAILED, sdxCluster);
                DatalakeEnableSeLinuxEvent event = DatalakeEnableSeLinuxEvent.builder().withSelector(HANDLED_FAILED_ENABLE_SELINUX_DATALAKE_EVENT.event())
                        .withResourceId(payload.getResourceId()).withResourceCrn(payload.getResourceCrn()).withResourceName(payload.getResourceName()).build();
                DatalakeEnableSeLinuxFailedEvent failHandledEvent = new DatalakeEnableSeLinuxFailedEvent(event, payload.getException(), payload.getStatus());
                sendEvent(context, failHandledEvent);
            }
        };
    }
}

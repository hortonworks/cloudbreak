package com.sequenceiq.datalake.flow.modifyselinux;

import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_MODIFY_SELINUX_ON_DATALAKE_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_MODIFY_SELINUX_ON_DATALAKE_IN_PROGRESS;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.RUNNING;
import static com.sequenceiq.datalake.flow.modifyselinux.event.DatalakeModifySeLinuxStateSelectors.FINALIZE_MODIFY_SELINUX_DATALAKE_EVENT;
import static com.sequenceiq.datalake.flow.modifyselinux.event.DatalakeModifySeLinuxStateSelectors.HANDLED_FAILED_MODIFY_SELINUX_DATALAKE_EVENT;
import static com.sequenceiq.datalake.metric.MetricType.SDX_ENABLE_SELINUX_FAILED;
import static com.sequenceiq.datalake.metric.MetricType.SDX_ENABLE_SELINUX_FINISHED;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.modifyselinux.event.DatalakeModifySeLinuxEvent;
import com.sequenceiq.datalake.flow.modifyselinux.event.DatalakeModifySeLinuxFailedEvent;
import com.sequenceiq.datalake.flow.modifyselinux.event.DatalakeModifySeLinuxHandlerEvent;
import com.sequenceiq.datalake.metric.SdxMetricService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.CommonContext;

@Configuration
public class DatalakeEnableSeLinuxActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeEnableSeLinuxActions.class);

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private SdxMetricService metricService;

    @Bean(name = "MODIFY_SELINUX_DATALAKE_STATE")
    public Action<?, ?> enableSeLinuxInDatalakeAction() {
        return new AbstractDatalakeEnableSeLinuxAction<>(DatalakeModifySeLinuxEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DatalakeModifySeLinuxEvent payload, Map<Object, Object> variables) {
                sdxStatusService.setStatusForDatalakeAndNotifyWithStatusReason(DATALAKE_MODIFY_SELINUX_ON_DATALAKE_IN_PROGRESS,
                        "Modify SELinux Mode on Data Lake.", payload.getResourceId());
                DatalakeModifySeLinuxHandlerEvent event = new DatalakeModifySeLinuxHandlerEvent(payload.getResourceId(), payload.getResourceName(),
                        payload.getResourceCrn(), payload.getSelinuxMode());
                sendEvent(context, event);
            }
        };
    }

    @Bean(name = "MODIFY_SELINUX_DATALAKE_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractDatalakeEnableSeLinuxAction<>(DatalakeModifySeLinuxEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DatalakeModifySeLinuxEvent payload, Map<Object, Object> variables) {
                SdxCluster sdxCluster = sdxStatusService.setStatusForDatalakeAndNotify(
                        RUNNING, "Data Lake SELinux set to 'ENFORCING' complete.", payload.getResourceId());
                metricService.incrementMetricCounter(SDX_ENABLE_SELINUX_FINISHED, sdxCluster);
                DatalakeModifySeLinuxEvent event = DatalakeModifySeLinuxEvent.builder().withSelector(FINALIZE_MODIFY_SELINUX_DATALAKE_EVENT.event())
                        .withResourceId(payload.getResourceId()).withResourceCrn(payload.getResourceCrn()).withResourceName(payload.getResourceName()).build();
                sendEvent(context, event);
            }
        };
    }

    @Bean(name = "MODIFY_SELINUX_DATALAKE_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractDatalakeEnableSeLinuxAction<>(DatalakeModifySeLinuxFailedEvent.class) {

            @Override
            protected void doExecute(CommonContext context, DatalakeModifySeLinuxFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.error(String.format("Failed to Enable SELinux in data lake '%s'. Status: '%s'.",
                        payload.getResourceCrn(), payload.getStatus()), payload.getException());
                SdxCluster sdxCluster = sdxStatusService.setStatusForDatalakeAndNotifyWithStatusReason(
                        DATALAKE_MODIFY_SELINUX_ON_DATALAKE_FAILED,
                        "Enable SELinux failed on the Data Lake.",
                        payload.getResourceId());
                metricService.incrementMetricCounter(SDX_ENABLE_SELINUX_FAILED, sdxCluster);
                DatalakeModifySeLinuxEvent event = DatalakeModifySeLinuxEvent.builder().withSelector(HANDLED_FAILED_MODIFY_SELINUX_DATALAKE_EVENT.event())
                        .withResourceId(payload.getResourceId()).withResourceCrn(payload.getResourceCrn()).withResourceName(payload.getResourceName()).build();
                DatalakeModifySeLinuxFailedEvent failHandledEvent = new DatalakeModifySeLinuxFailedEvent(event, payload.getException(), payload.getStatus());
                sendEvent(context, failHandledEvent);
            }
        };
    }
}

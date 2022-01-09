package com.sequenceiq.datalake.flow.upgrade.ccm;

import static com.sequenceiq.datalake.flow.upgrade.ccm.UpgradeCcmStateSelectors.UPGRADE_CCM_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.upgrade.ccm.UpgradeCcmStateSelectors.UPGRADE_CCM_FINALIZED_EVENT;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.upgrade.ccm.event.UpgradeCcmFailedEvent;
import com.sequenceiq.datalake.flow.upgrade.ccm.event.UpgradeCcmStackEvent;
import com.sequenceiq.datalake.flow.upgrade.ccm.event.UpgradeCcmStackRequest;
import com.sequenceiq.datalake.flow.upgrade.ccm.event.UpgradeCcmSuccessEvent;
import com.sequenceiq.datalake.metric.MetricType;
import com.sequenceiq.datalake.metric.SdxMetricService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;

@Configuration
public class UpgradeCcmActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeCcmActions.class);

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private SdxMetricService metricService;

    @Bean(name = "UPGRADE_CCM_UPGRADE_STACK_STATE")
    public Action<?, ?> upgradeCcmStack() {
        return new AbstractUpgradeCcmSdxAction<>(UpgradeCcmStackEvent.class) {

            @Override
            protected void doExecute(SdxContext context, UpgradeCcmStackEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Execute CCM upgrade stack flow for SDX: {}", payload.getResourceId());
                UpgradeCcmStackRequest request = UpgradeCcmStackRequest.from(context, payload.getSdxCluster());
                sendEvent(context, request);
            }

            @Override
            protected Object getFailurePayload(UpgradeCcmStackEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return UpgradeCcmFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "UPGRADE_CCM_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractUpgradeCcmSdxAction<>(UpgradeCcmSuccessEvent.class) {

            @Override
            protected void doExecute(SdxContext context, UpgradeCcmSuccessEvent payload, Map<Object, Object> variables) {
                LOGGER.info("CCM upgrade finalized for SDX: {}", payload.getResourceId());
                SdxCluster sdxCluster = sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING,
                        "Cluster Connectivity Manager upgrade completed successfully", payload.getResourceId());
                metricService.incrementMetricCounter(MetricType.UPGRADE_CCM_FINISHED, sdxCluster);
                sendEvent(context, UPGRADE_CCM_FINALIZED_EVENT.event(), payload);
            }

        };
    }

    @Bean(name = "UPGRADE_CCM_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractUpgradeCcmSdxAction<>(UpgradeCcmFailedEvent.class) {

            @Override
            protected void doExecute(SdxContext context, UpgradeCcmFailedEvent payload, Map<Object, Object> variables) {
                Exception exception = payload.getException();
                DatalakeStatusEnum failedStatus = DatalakeStatusEnum.DATALAKE_UPGRADE_CCM_FAILED;
                LOGGER.info("Update SDX status to {} for resource: {}", failedStatus, payload.getResourceId(), exception);
                String statusReason = "Cluster Connectivity Manager upgrade failed";
                if (exception.getMessage() != null) {
                    statusReason = exception.getMessage();
                }
                SdxCluster sdxCluster = sdxStatusService.setStatusForDatalakeAndNotify(failedStatus, statusReason, payload.getResourceId());
                metricService.incrementMetricCounter(MetricType.UPGRADE_CCM_FAILED, sdxCluster);
                sendEvent(context, UPGRADE_CCM_FAILED_HANDLED_EVENT.event(), payload);
            }

        };
    }
}

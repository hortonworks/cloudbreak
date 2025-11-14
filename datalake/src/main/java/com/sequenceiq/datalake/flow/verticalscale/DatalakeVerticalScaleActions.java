package com.sequenceiq.datalake.flow.verticalscale;

import static com.sequenceiq.datalake.flow.verticalscale.event.DatalakeVerticalScaleHandlerSelectors.VERTICAL_SCALING_DATALAKE_HANDLER;
import static com.sequenceiq.datalake.flow.verticalscale.event.DatalakeVerticalScaleHandlerSelectors.VERTICAL_SCALING_DATALAKE_VALIDATION_HANDLER;
import static com.sequenceiq.datalake.flow.verticalscale.event.DatalakeVerticalScaleStateSelectors.FINALIZE_VERTICAL_SCALING_DATALAKE_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.event.DatalakeVerticalScaleStateSelectors.HANDLED_FAILED_VERTICAL_SCALING_DATALAKE_EVENT;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.flow.verticalscale.event.DatalakeVerticalScaleEvent;
import com.sequenceiq.datalake.flow.verticalscale.event.DatalakeVerticalScaleFailedEvent;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.CommonContext;

@Configuration
public class DatalakeVerticalScaleActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeVerticalScaleActions.class);

    private final SdxStatusService sdxStatusService;

    private final EntitlementService entitlementService;

    public DatalakeVerticalScaleActions(SdxStatusService sdxStatusService, EntitlementService entitlementService) {
        this.sdxStatusService = sdxStatusService;
        this.entitlementService = entitlementService;
    }

    @Bean(name = "VERTICAL_SCALING_DATALAKE_VALIDATION_STATE")
    public Action<?, ?> verticalScaleValidationAction() {
        return new AbstractDatalakeVerticalScaleAction<>(DatalakeVerticalScaleEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DatalakeVerticalScaleEvent payload, Map<Object, Object> variables) {
                sdxStatusService.setStatusForDatalakeAndNotifyWithStatusReason(DatalakeStatusEnum.DATALAKE_VERTICAL_SCALE_VALIDATION_IN_PROGRESS,
                        String.format("Validation of vertical scale is in progress for group of %s with instance type of %s on the Data Lake.",
                                payload.getVerticalScaleRequest().getGroup(), payload.getVerticalScaleRequest().getTemplate().getInstanceType()),
                        payload.getResourceId());
                sendEvent(context, VERTICAL_SCALING_DATALAKE_VALIDATION_HANDLER.selector(), payload);
            }
        };
    }

    @Bean(name = "VERTICAL_SCALING_DATALAKE_STATE")
    public Action<?, ?> verticalScaleInDatalakeAction() {
        return new AbstractDatalakeVerticalScaleAction<>(DatalakeVerticalScaleEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DatalakeVerticalScaleEvent payload, Map<Object, Object> variables) {
                sdxStatusService.setStatusForDatalakeAndNotifyWithStatusReason(DatalakeStatusEnum.DATALAKE_VERTICAL_SCALE_ON_DATALAKE_IN_PROGRESS,
                        String.format("Vertical scaling is in progress for group of %s with instance type of %s on the Data Lake.",
                                payload.getVerticalScaleRequest().getGroup(), payload.getVerticalScaleRequest().getTemplate().getInstanceType()),
                        payload.getResourceId());
                sendEvent(context, VERTICAL_SCALING_DATALAKE_HANDLER.selector(), payload);
            }
        };
    }

    @Bean(name = "VERTICAL_SCALING_DATALAKE_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractDatalakeVerticalScaleAction<>(DatalakeVerticalScaleEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DatalakeVerticalScaleEvent payload, Map<Object, Object> variables) {
                String accountId = Crn.safeFromString(payload.getResourceCrn()).getAccountId();
                if (entitlementService.isVerticalScaleHaEnabled(accountId)) {
                    sdxStatusService.setStatusForDatalakeAndNotifyWithStatusReason(DatalakeStatusEnum.RUNNING,
                            "Vertical scale has finished on the Data Lake.", payload.getResourceId());
                } else {
                    sdxStatusService.setStatusForDatalakeAndNotifyWithStatusReason(DatalakeStatusEnum.STOPPED,
                            "Vertical scale has finished on the Data Lake.", payload.getResourceId());
                }
                sendEvent(context, FINALIZE_VERTICAL_SCALING_DATALAKE_EVENT.selector(), payload);
            }
        };
    }

    @Bean(name = "VERTICAL_SCALING_DATALAKE_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractDatalakeVerticalScaleAction<>(DatalakeVerticalScaleFailedEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DatalakeVerticalScaleFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.error(String.format("Failed to Vertical scale in DataLake '%s'. Status: '%s'.",
                        payload.getDataLakeVerticalScaleEvent(), payload.getDatalakeStatus()), payload.getException());
                sdxStatusService.setStatusForDatalakeAndNotifyWithStatusReason(
                        payload.getDatalakeStatus(),
                        String.format("Vertical scaling has failed on the Data Lake. %s ", payload.getException().getMessage()),
                        payload.getResourceId());
                sendEvent(context, HANDLED_FAILED_VERTICAL_SCALING_DATALAKE_EVENT.event(), payload);
            }
        };
    }

}

package com.sequenceiq.datalake.flow.verticalscale;

import static com.sequenceiq.datalake.flow.verticalscale.event.DataLakeVerticalScaleHandlerSelectors.VERTICAL_SCALING_DATALAKE_HANDLER;
import static com.sequenceiq.datalake.flow.verticalscale.event.DataLakeVerticalScaleHandlerSelectors.VERTICAL_SCALING_DATALAKE_VALIDATION_HANDLER;
import static com.sequenceiq.datalake.flow.verticalscale.event.DataLakeVerticalScaleStateSelectors.FINALIZE_VERTICAL_SCALING_DATALAKE_EVENT;
import static com.sequenceiq.datalake.flow.verticalscale.event.DataLakeVerticalScaleStateSelectors.HANDLED_FAILED_VERTICAL_SCALING_DATALAKE_EVENT;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.flow.verticalscale.event.DataLakeVerticalScaleEvent;
import com.sequenceiq.datalake.flow.verticalscale.event.DataLakeVerticalScaleFailedEvent;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.CommonContext;

@Configuration
public class DataLakeVerticalScaleActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataLakeVerticalScaleActions.class);

    private final SdxStatusService sdxStatusService;

    public DataLakeVerticalScaleActions(SdxStatusService sdxStatusService) {
        this.sdxStatusService = sdxStatusService;
    }

    @Bean(name = "VERTICAL_SCALING_DATALAKE_VALIDATION_STATE")
    public Action<?, ?> verticalScaleValidationAction() {
        return new AbstractDataLakeVerticalScaleAction<>(DataLakeVerticalScaleEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DataLakeVerticalScaleEvent payload, Map<Object, Object> variables) {
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.VERTICAL_SCALE_VALIDATION_IN_PROGRESS,
                                "Vertical scaling validation is in progress on the Data Lake.", payload.getResourceId());
                sendEvent(context, VERTICAL_SCALING_DATALAKE_VALIDATION_HANDLER.selector(), payload);
            }
        };
    }

    @Bean(name = "VERTICAL_SCALING_DATALAKE_STATE")
    public Action<?, ?> verticalScaleInFreeIpaAction() {
        return new AbstractDataLakeVerticalScaleAction<>(DataLakeVerticalScaleEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DataLakeVerticalScaleEvent payload, Map<Object, Object> variables) {
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.VERTICAL_SCALE_ON_DATALAKE_IN_PROGRESS,
                        "Vertical scaling is in progress on the Data Lake.", payload.getResourceId());
                sendEvent(context, VERTICAL_SCALING_DATALAKE_HANDLER.selector(), payload);
            }
        };
    }

    @Bean(name = "VERTICAL_SCALING_DATALAKE_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractDataLakeVerticalScaleAction<>(DataLakeVerticalScaleEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DataLakeVerticalScaleEvent payload, Map<Object, Object> variables) {
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.STOPPED,
                        "Vertical scaling finished on the Data Lake.", payload.getResourceId());
                sendEvent(context, FINALIZE_VERTICAL_SCALING_DATALAKE_EVENT.selector(), payload);
            }
        };
    }

    @Bean(name = "VERTICAL_SCALING_DATALAKE_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractDataLakeVerticalScaleAction<>(DataLakeVerticalScaleFailedEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DataLakeVerticalScaleFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.error(String.format("Failed to Vertical scale in DataLake '%s'. Status: '%s'.",
                        payload.getDataLakeVerticalScaleEvent(), payload.getDatalakeStatus()), payload.getException());
                sdxStatusService.setStatusForDatalakeAndNotify(payload.getDatalakeStatus(),
                        convertStatus(payload.getDatalakeStatus()),
                        payload.getException().getMessage(),
                        payload.getResourceId());
                sendEvent(context, HANDLED_FAILED_VERTICAL_SCALING_DATALAKE_EVENT.event(), payload);
            }

            private ResourceEvent convertStatus(DatalakeStatusEnum status) {
                switch (status) {
                    case VERTICAL_SCALE_VALIDATION_FAILED:
                        return ResourceEvent.VERTICAL_SCALE_VALIDATION_FAILED;
                    case VERTICAL_SCALE_ON_DATALAKE_FAILED:
                        return ResourceEvent.VERTICAL_SCALE_ON_DATALAKE_FAILED;
                    default:
                        return ResourceEvent.VERTICAL_SCALE_FAILED;
                }
            }
        };
    }

}

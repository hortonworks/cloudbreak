package com.sequenceiq.environment.environment.flow.upgrade.ccm;

import static com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmHandlerSelectors.UPGRADE_CCM_DATAHUB_HANDLER;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmHandlerSelectors.UPGRADE_CCM_DATALAKE_HANDLER;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmHandlerSelectors.UPGRADE_CCM_FREEIPA_HANDLER;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmHandlerSelectors.UPGRADE_CCM_VALIDATION_HANDLER;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmStateSelectors.FINALIZE_UPGRADE_CCM_EVENT;
import static com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmStateSelectors.HANDLED_FAILED_UPGRADE_CCM_EVENT;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmEvent;
import com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmFailedEvent;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.environment.metrics.EnvironmentMetricService;
import com.sequenceiq.environment.metrics.MetricType;
import com.sequenceiq.flow.core.CommonContext;

@Configuration
public class UpgradeCcmActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeCcmActions.class);

    private final EnvironmentStatusUpdateService environmentStatusUpdateService;

    private final EnvironmentMetricService metricService;

    public UpgradeCcmActions(EnvironmentStatusUpdateService environmentStatusUpdateService, EnvironmentMetricService metricService) {
        this.environmentStatusUpdateService = environmentStatusUpdateService;
        this.metricService = metricService;
    }

    @Bean(name = "UPGRADE_CCM_VALIDATION_STATE")
    public Action<?, ?> upgradeCcmValidationAction() {
        return new AbstractUpgradeCcmAction<>(UpgradeCcmEvent.class) {
            @Override
            protected void doExecute(CommonContext context, UpgradeCcmEvent payload, Map<Object, Object> variables) {
                EnvironmentDto envDto = environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.UPGRADE_CCM_VALIDATION_IN_PROGRESS,
                                ResourceEvent.ENVIRONMENT_UPGRADE_CCM_VALIDATION_STARTED, UpgradeCcmState.UPGRADE_CCM_VALIDATION_STATE);
                sendEvent(context, UPGRADE_CCM_VALIDATION_HANDLER.selector(), envDto);
            }
        };
    }

    @Bean(name = "UPGRADE_CCM_FREEIPA_STATE")
    public Action<?, ?> upgradeCcmInFreeIpaAction() {
        return new AbstractUpgradeCcmAction<>(UpgradeCcmEvent.class) {
            @Override
            protected void doExecute(CommonContext context, UpgradeCcmEvent payload, Map<Object, Object> variables) {
                EnvironmentDto envDto = environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.UPGRADE_CCM_ON_FREEIPA_IN_PROGRESS,
                                ResourceEvent.ENVIRONMENT_UPGRADE_CCM_ON_FREEIPA_STARTED, UpgradeCcmState.UPGRADE_CCM_FREEIPA_STATE);
                sendEvent(context, UPGRADE_CCM_FREEIPA_HANDLER.selector(), envDto);
            }
        };
    }

    @Bean(name = "UPGRADE_CCM_DATALAKE_STATE")
    public Action<?, ?> upgradeCcmInDataLakeAction() {
        return new AbstractUpgradeCcmAction<>(UpgradeCcmEvent.class) {
            @Override
            protected void doExecute(CommonContext context, UpgradeCcmEvent payload, Map<Object, Object> variables) {
                EnvironmentDto envDto = environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.UPGRADE_CCM_ON_DATALAKE_IN_PROGRESS,
                                ResourceEvent.ENVIRONMENT_UPGRADE_CCM_ON_DATALAKE_STARTED, UpgradeCcmState.UPGRADE_CCM_DATALAKE_STATE);
                sendEvent(context, UPGRADE_CCM_DATALAKE_HANDLER.selector(), envDto);
            }
        };
    }

    @Bean(name = "UPGRADE_CCM_DATAHUB_STATE")
    public Action<?, ?> upgradeCcmInDataHubAction() {
        return new AbstractUpgradeCcmAction<>(UpgradeCcmEvent.class) {
            @Override
            protected void doExecute(CommonContext context, UpgradeCcmEvent payload, Map<Object, Object> variables) {
                EnvironmentDto envDto = environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.UPGRADE_CCM_ON_DATAHUB_IN_PROGRESS,
                                ResourceEvent.ENVIRONMENT_UPGRADE_CCM_ON_DATAHUB_STARTED, UpgradeCcmState.UPGRADE_CCM_DATAHUB_STATE);
                sendEvent(context, UPGRADE_CCM_DATAHUB_HANDLER.selector(), envDto);
            }
        };
    }

    @Bean(name = "UPGRADE_CCM_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractUpgradeCcmAction<>(UpgradeCcmEvent.class) {
            @Override
            protected void doExecute(CommonContext context, UpgradeCcmEvent payload, Map<Object, Object> variables) {
                EnvironmentDto environmentDto = environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.AVAILABLE,
                                ResourceEvent.ENVIRONMENT_UPGRADE_CCM_FINISHED, UpgradeCcmState.UPGRADE_CCM_FINISHED_STATE);
                metricService.incrementMetricCounter(MetricType.ENV_UPGRADE_CCM_FINISHED, environmentDto);
                sendEvent(context, FINALIZE_UPGRADE_CCM_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "UPGRADE_CCM_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractUpgradeCcmAction<>(UpgradeCcmFailedEvent.class) {
            @Override
            protected void doExecute(CommonContext context, UpgradeCcmFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.error(String.format("Failed to upgrade CCM in environment '%s'. Status: '%s'.",
                        payload.getEnvironmentDto(), payload.getEnvironmentStatus()), payload.getException());
                EnvironmentDto environmentDto = environmentStatusUpdateService
                        .updateFailedEnvironmentStatusAndNotify(context, payload, payload.getEnvironmentStatus(),
                                convertStatus(payload.getEnvironmentStatus()), UpgradeCcmState.UPGRADE_CCM_FAILED_STATE);
                metricService.incrementMetricCounter(MetricType.ENV_UPGRADE_CCM_FAILED, environmentDto, payload.getException());
                sendEvent(context, HANDLED_FAILED_UPGRADE_CCM_EVENT.event(), payload);
            }

            private ResourceEvent convertStatus(EnvironmentStatus status) {
                switch (status) {
                    case UPGRADE_CCM_VALIDATION_FAILED:
                        return ResourceEvent.ENVIRONMENT_UPGRADE_CCM_VALIDATION_FAILED;
                    case UPGRADE_CCM_ON_FREEIPA_FAILED:
                        return ResourceEvent.ENVIRONMENT_UPGRADE_CCM_ON_FREEIPA_FAILED;
                    case UPGRADE_CCM_ON_DATALAKE_FAILED:
                        return ResourceEvent.ENVIRONMENT_UPGRADE_CCM_ON_DATALAKE_FAILED;
                    case UPGRADE_CCM_ON_DATAHUB_FAILED:
                        return ResourceEvent.ENVIRONMENT_UPGRADE_CCM_ON_DATAHUB_FAILED;
                    default:
                        return ResourceEvent.ENVIRONMENT_UPGRADE_CCM_FAILED;
                }
            }
        };
    }

}

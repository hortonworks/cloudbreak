package com.sequenceiq.environment.environment.flow.verticalscale.freeipa;

import static com.sequenceiq.environment.environment.flow.verticalscale.freeipa.event.EnvironmentVerticalScaleHandlerSelectors.VERTICAL_SCALING_FREEIPA_HANDLER;
import static com.sequenceiq.environment.environment.flow.verticalscale.freeipa.event.EnvironmentVerticalScaleHandlerSelectors.VERTICAL_SCALING_FREEIPA_VALIDATION_HANDLER;
import static com.sequenceiq.environment.environment.flow.verticalscale.freeipa.event.EnvironmentVerticalScaleStateSelectors.FINALIZE_VERTICAL_SCALING_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.verticalscale.freeipa.event.EnvironmentVerticalScaleStateSelectors.HANDLED_FAILED_VERTICAL_SCALING_FREEIPA_EVENT;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.verticalscale.freeipa.event.EnvironmentVerticalScaleEvent;
import com.sequenceiq.environment.environment.flow.verticalscale.freeipa.event.EnvironmentVerticalScaleFailedEvent;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.metrics.EnvironmentMetricService;
import com.sequenceiq.environment.metrics.MetricType;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@Configuration
public class EnvironmentVerticalScaleActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentVerticalScaleActions.class);

    private static final String GROUP_BEING_SCALED = "GROUP_BEING_SCALED";

    private static final String PREVIOUS_INSTANCE_TYPE = "PREVIOUS_INSTANCE_TYPE";

    private static final String TARGET_INSTANCE_TYPE = "TARGET_INSTANCE_TYPE";

    private final EnvironmentStatusUpdateService environmentStatusUpdateService;

    private final EnvironmentMetricService metricService;

    private final FreeIpaService freeIpaService;

    public EnvironmentVerticalScaleActions(EnvironmentStatusUpdateService environmentStatusUpdateService,
            EnvironmentMetricService metricService, FreeIpaService freeIpaService) {
        this.environmentStatusUpdateService = environmentStatusUpdateService;
        this.metricService = metricService;
        this.freeIpaService = freeIpaService;
    }

    @Bean(name = "VERTICAL_SCALING_FREEIPA_VALIDATION_STATE")
    public Action<?, ?> verticalScaleValidationAction() {
        return new AbstractEnvironmentVerticalScaleAction<>(EnvironmentVerticalScaleEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvironmentVerticalScaleEvent payload, Map<Object, Object> variables) {
                environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.VERTICAL_SCALE_VALIDATION_IN_PROGRESS,
                                ResourceEvent.ENVIRONMENT_VERTICAL_SCALE_VALIDATION_STARTED,
                                EnvironmentVerticalScaleState.VERTICAL_SCALING_FREEIPA_VALIDATION_STATE);
                sendEvent(context, VERTICAL_SCALING_FREEIPA_VALIDATION_HANDLER.selector(), payload);
            }
        };
    }

    @Bean(name = "VERTICAL_SCALING_FREEIPA_STATE")
    public Action<?, ?> verticalScaleInFreeIpaAction() {
        return new AbstractEnvironmentVerticalScaleAction<>(EnvironmentVerticalScaleEvent.class) {
            @Override
            protected void prepareExecution(EnvironmentVerticalScaleEvent payload, Map<Object, Object> variables) {
                Optional<DescribeFreeIpaResponse> describeFreeIpaResponse = freeIpaService.describe(payload.getResourceCrn());
                String previousInstanceType = getPreviousInstanceType(payload, describeFreeIpaResponse);
                variables.put(PREVIOUS_INSTANCE_TYPE, previousInstanceType);
                variables.put(TARGET_INSTANCE_TYPE, payload.getFreeIPAVerticalScaleRequest().getTemplate().getInstanceType());
                variables.put(GROUP_BEING_SCALED, payload.getFreeIPAVerticalScaleRequest().getGroup());
            }

            @Override
            protected void doExecute(CommonContext context, EnvironmentVerticalScaleEvent payload, Map<Object, Object> variables) {
                String groupBeingScaled = (String) variables.getOrDefault(GROUP_BEING_SCALED, "unknown");
                String previousInstanceType = (String) variables.getOrDefault(PREVIOUS_INSTANCE_TYPE, "unknown");
                String targetInstanceType = (String) variables.getOrDefault(TARGET_INSTANCE_TYPE, "unknown");
                environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.VERTICAL_SCALE_ON_FREEIPA_IN_PROGRESS,
                                ResourceEvent.ENVIRONMENT_VERTICAL_SCALE_ON_FREEIPA_STARTED,
                                List.of(groupBeingScaled, previousInstanceType, targetInstanceType),
                                EnvironmentVerticalScaleState.VERTICAL_SCALING_FREEIPA_STATE);
                sendEvent(context, VERTICAL_SCALING_FREEIPA_HANDLER.selector(), payload);
            }

            private String getPreviousInstanceType(EnvironmentVerticalScaleEvent payload, Optional<DescribeFreeIpaResponse> describeFreeIpaResponse) {
                if (describeFreeIpaResponse.isPresent()) {
                    return describeFreeIpaResponse.get().getInstanceGroups()
                            .stream()
                            .filter(ig -> ig.getName().equals(payload.getFreeIPAVerticalScaleRequest().getGroup()))
                            .map(ig -> ig.getInstanceTemplate().getInstanceType())
                            .findFirst()
                            .orElse("unknown");
                } else {
                    return "unknown";
                }
            }
        };
    }

    @Bean(name = "VERTICAL_SCALING_FREEIPA_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractEnvironmentVerticalScaleAction<>(EnvironmentVerticalScaleEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvironmentVerticalScaleEvent payload, Map<Object, Object> variables) {
                String groupBeingScaled = (String) variables.getOrDefault(GROUP_BEING_SCALED, "unknown");
                String previousInstanceType = (String) variables.getOrDefault(PREVIOUS_INSTANCE_TYPE, "unknown");
                String targetInstanceType = (String) variables.getOrDefault(TARGET_INSTANCE_TYPE, "unknown");
                EnvironmentDto environmentDto = environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.ENV_STOPPED,
                                ResourceEvent.ENVIRONMENT_VERTICAL_SCALE_FINISHED,
                                List.of(groupBeingScaled, previousInstanceType, targetInstanceType),
                                EnvironmentVerticalScaleState.VERTICAL_SCALING_FREEIPA_FINISHED_STATE);
                metricService.incrementMetricCounter(MetricType.ENV_VERTICAL_SCALE_FINISHED, environmentDto);
                sendEvent(context, FINALIZE_VERTICAL_SCALING_FREEIPA_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "VERTICAL_SCALING_FREEIPA_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractEnvironmentVerticalScaleAction<>(EnvironmentVerticalScaleFailedEvent.class) {

            @Override
            protected void doExecute(CommonContext context, EnvironmentVerticalScaleFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.error(String.format("Failed to Vertical scale in environment '%s'. Status: '%s'.",
                        payload.getVerticalScaleFreeIPAEvent(), payload.getEnvironmentStatus()), payload.getException());
                String groupBeingScaled = (String) variables.getOrDefault(GROUP_BEING_SCALED, "unknown");
                String previousInstanceType = (String) variables.getOrDefault(PREVIOUS_INSTANCE_TYPE, "unknown");
                String targetInstanceType = (String) variables.getOrDefault(TARGET_INSTANCE_TYPE, "unknown");
                EnvironmentDto environmentDto = environmentStatusUpdateService
                        .updateFailedEnvironmentStatusAndNotify(context,
                                payload,
                                payload.getEnvironmentStatus(),
                                convertStatus(payload.getEnvironmentStatus()),
                                List.of(groupBeingScaled, previousInstanceType, targetInstanceType, payload.getException().getMessage()),
                                EnvironmentVerticalScaleState.VERTICAL_SCALING_FREEIPA_FAILED_STATE);
                metricService.incrementMetricCounter(MetricType.ENV_VERTICAL_SCALE_FAILED, environmentDto, payload.getException());
                sendEvent(context, HANDLED_FAILED_VERTICAL_SCALING_FREEIPA_EVENT.event(), payload);
            }

            private ResourceEvent convertStatus(EnvironmentStatus status) {
                switch (status) {
                    case VERTICAL_SCALE_VALIDATION_FAILED:
                        return ResourceEvent.ENVIRONMENT_VERTICAL_SCALE_VALIDATION_FAILED;
                    case VERTICAL_SCALE_ON_FREEIPA_FAILED:
                        return ResourceEvent.ENVIRONMENT_VERTICAL_SCALE_ON_FREEIPA_FAILED;
                    default:
                        return ResourceEvent.ENVIRONMENT_VERTICAL_SCALE_FAILED;
                }
            }
        };
    }

}

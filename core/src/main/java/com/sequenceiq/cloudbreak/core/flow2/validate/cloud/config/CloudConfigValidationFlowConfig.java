package com.sequenceiq.cloudbreak.core.flow2.validate.cloud.config;

import static com.sequenceiq.cloudbreak.core.flow2.validate.cloud.config.CloudConfigValidationEvent.VALIDATE_CLOUD_CONFIG_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.cloud.config.CloudConfigValidationEvent.VALIDATE_CLOUD_CONFIG_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.cloud.config.CloudConfigValidationEvent.VALIDATE_CLOUD_CONFIG_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.cloud.config.CloudConfigValidationEvent.VALIDATE_CLOUD_CONFIG_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.cloud.config.CloudConfigValidationState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.validate.cloud.config.CloudConfigValidationState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.validate.cloud.config.CloudConfigValidationState.VALIDATE_CLOUD_CONFIG_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.validate.cloud.config.CloudConfigValidationState.VALIDATE_CLOUD_CONFIG_STATE;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizer;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.FlowFinalizerCallback;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class CloudConfigValidationFlowConfig extends AbstractFlowConfiguration<CloudConfigValidationState, CloudConfigValidationEvent>
        implements RetryableFlowConfiguration<CloudConfigValidationEvent> {

    private static final List<Transition<CloudConfigValidationState, CloudConfigValidationEvent>> TRANSITIONS =
            new Builder<CloudConfigValidationState, CloudConfigValidationEvent>()
            .defaultFailureEvent(VALIDATE_CLOUD_CONFIG_FAILED_EVENT)
            .from(INIT_STATE).to(VALIDATE_CLOUD_CONFIG_STATE).event(VALIDATE_CLOUD_CONFIG_EVENT).defaultFailureEvent()
            .from(VALIDATE_CLOUD_CONFIG_STATE).to(FINAL_STATE).event(VALIDATE_CLOUD_CONFIG_FINISHED_EVENT).defaultFailureEvent()
            .build();

    private static final FlowEdgeConfig<CloudConfigValidationState, CloudConfigValidationEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, VALIDATE_CLOUD_CONFIG_FAILED_STATE, VALIDATE_CLOUD_CONFIG_FAILURE_HANDLED_EVENT);

    @Inject
    private StackStatusFinalizer stackStatusFinalizer;

    public CloudConfigValidationFlowConfig() {
        super(CloudConfigValidationState.class, CloudConfigValidationEvent.class);
    }

    @Override
    protected List<Transition<CloudConfigValidationState, CloudConfigValidationEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<CloudConfigValidationState, CloudConfigValidationEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public CloudConfigValidationEvent[] getEvents() {
        return CloudConfigValidationEvent.values();
    }

    @Override
    public CloudConfigValidationEvent[] getInitEvents() {
        return new CloudConfigValidationEvent[] {
                VALIDATE_CLOUD_CONFIG_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Validate cloud config for stack";
    }

    @Override
    public CloudConfigValidationEvent getRetryableEvent() {
        return VALIDATE_CLOUD_CONFIG_FAILURE_HANDLED_EVENT;
    }

    @Override
    public FlowFinalizerCallback getFinalizerCallBack() {
        return stackStatusFinalizer;
    }
}

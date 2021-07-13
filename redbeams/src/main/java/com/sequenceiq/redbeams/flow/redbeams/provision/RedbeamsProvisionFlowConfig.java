package com.sequenceiq.redbeams.flow.redbeams.provision;

import static com.sequenceiq.redbeams.flow.redbeams.provision.RedbeamsProvisionEvent.ALLOCATE_DATABASE_SERVER_FAILED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.provision.RedbeamsProvisionEvent.ALLOCATE_DATABASE_SERVER_FINISHED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.provision.RedbeamsProvisionEvent.REDBEAMS_PROVISION_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.provision.RedbeamsProvisionEvent.REDBEAMS_PROVISION_FAILED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.provision.RedbeamsProvisionEvent.REDBEAMS_PROVISION_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.provision.RedbeamsProvisionEvent.REDBEAMS_PROVISION_FINISHED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.provision.RedbeamsProvisionEvent.UPDATE_DATABASE_SERVER_REGISTRATION_FAILED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.provision.RedbeamsProvisionEvent.UPDATE_DATABASE_SERVER_REGISTRATION_FINISHED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.provision.RedbeamsProvisionState.ALLOCATE_DATABASE_SERVER_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.provision.RedbeamsProvisionState.FINAL_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.provision.RedbeamsProvisionState.INIT_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.provision.RedbeamsProvisionState.REDBEAMS_PROVISION_FAILED_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.provision.RedbeamsProvisionState.REDBEAMS_PROVISION_FINISHED_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.provision.RedbeamsProvisionState.UPDATE_DATABASE_SERVER_REGISTRATION_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class RedbeamsProvisionFlowConfig extends AbstractFlowConfiguration<RedbeamsProvisionState, RedbeamsProvisionEvent>
        implements RetryableFlowConfiguration<RedbeamsProvisionEvent> {

    private static final RedbeamsProvisionEvent[] REDBEAMS_INIT_EVENTS = {REDBEAMS_PROVISION_EVENT};

    private static final List<Transition<RedbeamsProvisionState, RedbeamsProvisionEvent>> TRANSITIONS =
            new Builder<RedbeamsProvisionState, RedbeamsProvisionEvent>().defaultFailureEvent(REDBEAMS_PROVISION_FAILED_EVENT)
            .from(INIT_STATE).to(ALLOCATE_DATABASE_SERVER_STATE).event(REDBEAMS_PROVISION_EVENT).defaultFailureEvent()
            .from(ALLOCATE_DATABASE_SERVER_STATE).to(UPDATE_DATABASE_SERVER_REGISTRATION_STATE).event(ALLOCATE_DATABASE_SERVER_FINISHED_EVENT)
                    .failureState(REDBEAMS_PROVISION_FAILED_STATE).failureEvent(ALLOCATE_DATABASE_SERVER_FAILED_EVENT)
            .from(UPDATE_DATABASE_SERVER_REGISTRATION_STATE).to(REDBEAMS_PROVISION_FINISHED_STATE).event(UPDATE_DATABASE_SERVER_REGISTRATION_FINISHED_EVENT)
                    .failureState(REDBEAMS_PROVISION_FAILED_STATE).failureEvent(UPDATE_DATABASE_SERVER_REGISTRATION_FAILED_EVENT)
            .from(REDBEAMS_PROVISION_FINISHED_STATE).to(FINAL_STATE).event(REDBEAMS_PROVISION_FINISHED_EVENT).noFailureEvent()
            .from(REDBEAMS_PROVISION_FAILED_STATE).to(FINAL_STATE).event(REDBEAMS_PROVISION_FAILURE_HANDLED_EVENT).noFailureEvent()
            .build();

    private static final FlowEdgeConfig<RedbeamsProvisionState, RedbeamsProvisionEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, REDBEAMS_PROVISION_FAILED_STATE, REDBEAMS_PROVISION_FAILURE_HANDLED_EVENT);

    public RedbeamsProvisionFlowConfig() {
        super(RedbeamsProvisionState.class, RedbeamsProvisionEvent.class);
    }

    @Override
    protected List<Transition<RedbeamsProvisionState, RedbeamsProvisionEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<RedbeamsProvisionState, RedbeamsProvisionEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public RedbeamsProvisionEvent[] getEvents() {
        return RedbeamsProvisionEvent.values();
    }

    @Override
    public RedbeamsProvisionEvent[] getInitEvents() {
        return REDBEAMS_INIT_EVENTS;
    }

    @Override
    public String getDisplayName() {
        return "Provision RDS";
    }

    @Override
    public RedbeamsProvisionEvent getRetryableEvent() {
        return REDBEAMS_PROVISION_FAILURE_HANDLED_EVENT;
    }

    @Override
    public OperationType getFlowOperationType() {
        return OperationType.PROVISION;
    }
}

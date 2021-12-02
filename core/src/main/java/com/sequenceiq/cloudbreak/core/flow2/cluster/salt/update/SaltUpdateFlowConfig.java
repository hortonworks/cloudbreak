package com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent.BOOTSTRAP_MACHINES_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent.BOOTSTRAP_MACHINES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent.CONFIGURE_KEYTABS_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent.CONFIGURE_KEYTABS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent.SALT_UPDATE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent.SALT_UPDATE_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent.SALT_UPDATE_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent.SALT_UPDATE_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent.START_AMBARI_SERVICES_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent.START_AMBARI_SERVICES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent.UPLOAD_RECIPES_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent.UPLOAD_RECIPES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateState.RECONFIGURE_KEYTABS_FOR_SU_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateState.RUN_HIGHSTATE_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateState.SALT_UPDATE_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateState.SALT_UPDATE_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateState.UPDATE_SALT_STATE_FILES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateState.UPLOAD_RECIPES_FOR_SU_STATE;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizer;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.FlowFinalizerCallback;

@Component
public class SaltUpdateFlowConfig extends AbstractFlowConfiguration<SaltUpdateState, SaltUpdateEvent> {

    private static final List<Transition<SaltUpdateState, SaltUpdateEvent>> TRANSITIONS =
            new Builder<SaltUpdateState, SaltUpdateEvent>().defaultFailureEvent(SALT_UPDATE_FAILED_EVENT)
            .from(INIT_STATE).to(UPDATE_SALT_STATE_FILES_STATE).event(SALT_UPDATE_EVENT).defaultFailureEvent()
            .from(UPDATE_SALT_STATE_FILES_STATE).to(UPLOAD_RECIPES_FOR_SU_STATE).event(BOOTSTRAP_MACHINES_FINISHED_EVENT)
                    .failureEvent(BOOTSTRAP_MACHINES_FAILED_EVENT)
            .from(UPLOAD_RECIPES_FOR_SU_STATE).to(RECONFIGURE_KEYTABS_FOR_SU_STATE).event(UPLOAD_RECIPES_FINISHED_EVENT)
                    .failureEvent(UPLOAD_RECIPES_FAILED_EVENT)
            .from(RECONFIGURE_KEYTABS_FOR_SU_STATE).to(RUN_HIGHSTATE_STATE).event(CONFIGURE_KEYTABS_FINISHED_EVENT).failureEvent(CONFIGURE_KEYTABS_FAILED_EVENT)
            .from(RUN_HIGHSTATE_STATE).to(SALT_UPDATE_FINISHED_STATE).event(START_AMBARI_SERVICES_FINISHED_EVENT)
                    .failureEvent(START_AMBARI_SERVICES_FAILED_EVENT)
            .from(SALT_UPDATE_FINISHED_STATE).to(FINAL_STATE).event(SALT_UPDATE_FINISHED_EVENT).noFailureEvent()
            .build();

    @Inject
    private StackStatusFinalizer stackStatusFinalizer;

    public SaltUpdateFlowConfig() {
        super(SaltUpdateState.class, SaltUpdateEvent.class);
    }

    @Override
    protected List<Transition<SaltUpdateState, SaltUpdateEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<SaltUpdateState, SaltUpdateEvent> getEdgeConfig() {
        return new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, SALT_UPDATE_FAILED_STATE, SALT_UPDATE_FAILURE_HANDLED_EVENT);
    }

    @Override
    public SaltUpdateEvent[] getEvents() {
        return SaltUpdateEvent.values();
    }

    @Override
    public SaltUpdateEvent[] getInitEvents() {
        return new SaltUpdateEvent[]{SALT_UPDATE_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Salt update";
    }

    @Override
    public FlowFinalizerCallback getFinalizerCallBack() {
        return stackStatusFinalizer;
    }
}

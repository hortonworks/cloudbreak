package com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.CcmUpgradeEvent.CCM_UPGRADE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.CcmUpgradeEvent.CCM_UPGRADE_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.CcmUpgradeEvent.CCM_UPGRADE_PREPARATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.CcmUpgradeEvent.CCM_UPGRADE_PREPARATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.CcmUpgradeEvent.CCM_UPGRADE_REMOVE_AUTOSSH_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.CcmUpgradeEvent.CCM_UPGRADE_RE_REGISTER_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.CcmUpgradeEvent.CCM_UPGRADE_UNREGISTER_HOSTS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.CcmUpgradeEvent.FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.CcmUpgradeEvent.FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.CcmUpgradeState.CCM_UPGRADE_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.CcmUpgradeState.CCM_UPGRADE_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.CcmUpgradeState.CCM_UPGRADE_PREPARATION_FAILED;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.CcmUpgradeState.CCM_UPGRADE_PREPARATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.CcmUpgradeState.CCM_UPGRADE_REMOVE_AUTOSSH;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.CcmUpgradeState.CCM_UPGRADE_RE_REGISTER_TO_CP;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.CcmUpgradeState.CCM_UPGRADE_UNREGISTER_HOSTS;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.CcmUpgradeState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.CcmUpgradeState.INIT_STATE;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizer;
import com.sequenceiq.flow.core.FlowTriggerCondition;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.FlowFinalizerCallback;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class CcmUpgradeFlowConfig extends AbstractFlowConfiguration<CcmUpgradeState, CcmUpgradeEvent>
        implements RetryableFlowConfiguration<CcmUpgradeEvent> {

    private static final List<Transition<CcmUpgradeState, CcmUpgradeEvent>> TRANSITIONS =
            new Builder<CcmUpgradeState, CcmUpgradeEvent>()
                    .defaultFailureEvent(CCM_UPGRADE_FAILED_EVENT)
                    .from(INIT_STATE).to(CCM_UPGRADE_PREPARATION_STATE)
                    .event(CCM_UPGRADE_EVENT)
                    .defaultFailureEvent()

                    .from(CCM_UPGRADE_PREPARATION_STATE).to(CCM_UPGRADE_RE_REGISTER_TO_CP)
                    .event(CCM_UPGRADE_PREPARATION_FINISHED_EVENT)
                    .failureState(CCM_UPGRADE_PREPARATION_FAILED)
                    .failureEvent(CCM_UPGRADE_PREPARATION_FAILED_EVENT)

                    .from(CCM_UPGRADE_RE_REGISTER_TO_CP).to(CCM_UPGRADE_REMOVE_AUTOSSH)
                    .event(CCM_UPGRADE_RE_REGISTER_FINISHED_EVENT)
                    .failureState(CCM_UPGRADE_FAILED_STATE)
                    .defaultFailureEvent()

                    .from(CCM_UPGRADE_REMOVE_AUTOSSH).to(CCM_UPGRADE_UNREGISTER_HOSTS)
                    .event(CCM_UPGRADE_REMOVE_AUTOSSH_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(CCM_UPGRADE_UNREGISTER_HOSTS).to(CCM_UPGRADE_FINISHED_STATE)
                    .event(CCM_UPGRADE_UNREGISTER_HOSTS_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(CCM_UPGRADE_FINISHED_STATE).to(FINAL_STATE)
                    .event(FINALIZED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<CcmUpgradeState, CcmUpgradeEvent> EDGE_CONFIG = new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE,
            CCM_UPGRADE_FAILED_STATE, FAIL_HANDLED_EVENT);

    @Inject
    private StackStatusFinalizer stackStatusFinalizer;

    public CcmUpgradeFlowConfig() {
        super(CcmUpgradeState.class, CcmUpgradeEvent.class);
    }

    @Override
    public FlowTriggerCondition getFlowTriggerCondition() {
        return getApplicationContext().getBean(CcmUpgradeFlowTriggerCondition.class);
    }

    @Override
    protected List<Transition<CcmUpgradeState, CcmUpgradeEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<CcmUpgradeState, CcmUpgradeEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public CcmUpgradeEvent[] getEvents() {
        return CcmUpgradeEvent.values();
    }

    @Override
    public CcmUpgradeEvent[] getInitEvents() {
        return new CcmUpgradeEvent[] {
                CCM_UPGRADE_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "CCM upgrade";
    }

    @Override
    public CcmUpgradeEvent getRetryableEvent() {
        return FAIL_HANDLED_EVENT;
    }

    @Override
    public FlowFinalizerCallback getFinalizerCallBack() {
        return stackStatusFinalizer;
    }
}

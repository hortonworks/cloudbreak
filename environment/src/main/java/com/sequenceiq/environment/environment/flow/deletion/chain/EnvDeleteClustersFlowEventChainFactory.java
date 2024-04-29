package com.sequenceiq.environment.environment.flow.deletion.chain;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.DELETE_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.DELETE_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.DELETE_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.UNSET;
import static com.sequenceiq.environment.environment.flow.deletion.EnvDeleteState.ENV_DELETE_FAILED_STATE;
import static com.sequenceiq.environment.environment.flow.deletion.EnvDeleteState.ENV_DELETE_FINISHED_STATE;
import static com.sequenceiq.environment.environment.flow.deletion.EnvDeleteState.INIT_STATE;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvClustersDeleteStateSelectors.START_DATAHUB_CLUSTERS_DELETE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_FREEIPA_DELETE_EVENT;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.EnvironmentUseCaseAware;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class EnvDeleteClustersFlowEventChainFactory implements FlowEventChainFactory<EnvDeleteEvent>, EnvironmentUseCaseAware {

    private EnvironmentService environmentService;

    public EnvDeleteClustersFlowEventChainFactory(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    @Override
    public String initEvent() {
        return FlowChainTriggers.ENV_DELETE_CLUSTERS_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(EnvDeleteEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();

        List<Environment> childEnvironments =
                environmentService.findAllByAccountIdAndParentEnvIdAndArchivedIsFalse(ThreadBasedUserCrnProvider.getAccountId(), event.getResourceId());
        childEnvironments.forEach(childEnvironment -> {
            List<Selectable> flowEventsForChildEnvironment = getFlowEventsForChildEnvironment(event, childEnvironment);
            flowEventChain.addAll(flowEventsForChildEnvironment);
        });

        flowEventChain.addAll(getFlowEventsForParentEnvironment(event));

        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }

    private List<Selectable> getFlowEventsForParentEnvironment(EnvDeleteEvent event) {
        return getFlowEvents(event.getResourceId(), event.getResourceName(), event.getResourceCrn(), event.isForceDelete(), event.accepted());
    }

    private List<Selectable> getFlowEventsForChildEnvironment(EnvDeleteEvent event, Environment environment) {
        return getFlowEvents(environment.getId(), environment.getName(), environment.getResourceCrn(), event.isForceDelete(), event.accepted());
    }

    private List<Selectable> getFlowEvents(Long resourceId, String resourceName, String resourceCrn, boolean forceDelete, Promise<AcceptResult> accepted) {
        return List.of(
                EnvDeleteEvent.builder()
                        .withSelector(START_DATAHUB_CLUSTERS_DELETE_EVENT.event())
                        .withResourceId(resourceId)
                        .withAccepted(accepted)
                        .withResourceName(resourceName)
                        .withResourceCrn(resourceCrn)
                        .withForceDelete(forceDelete)
                        .build(),
                EnvDeleteEvent.builder()
                        .withSelector(START_FREEIPA_DELETE_EVENT.event())
                        .withResourceId(resourceId)
                        .withResourceName(resourceName)
                        .withResourceCrn(resourceCrn)
                        .withForceDelete(forceDelete)
                        .build()
        );
    }

    @Override
    public Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        if (INIT_STATE.equals(flowState)) {
            return DELETE_STARTED;
        } else if (ENV_DELETE_FINISHED_STATE.equals(flowState)) {
            return DELETE_FINISHED;
        } else if (ENV_DELETE_FAILED_STATE.equals(flowState)) {
            return DELETE_FAILED;
        } else {
            return UNSET;
        }
    }
}

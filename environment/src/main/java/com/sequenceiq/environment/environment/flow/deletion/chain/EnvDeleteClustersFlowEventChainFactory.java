package com.sequenceiq.environment.environment.flow.deletion.chain;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvClustersDeleteStateSelectors.START_DATAHUB_CLUSTERS_DELETE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_FREEIPA_DELETE_EVENT;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

import reactor.rx.Promise;

@Component
// TODO: CB-11559
public class EnvDeleteClustersFlowEventChainFactory implements FlowEventChainFactory<EnvDeleteEvent> {

    private EnvironmentService environmentService;

    public EnvDeleteClustersFlowEventChainFactory(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    @Override
    public String initEvent() {
        return FlowChainTriggers.ENV_DELETE_CLUSTERS_TRIGGER_EVENT.getValue();
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

        return new FlowTriggerEventQueue(getName(), flowEventChain);
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
}

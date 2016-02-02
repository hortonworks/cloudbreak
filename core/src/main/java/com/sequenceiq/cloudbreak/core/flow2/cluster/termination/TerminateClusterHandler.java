package com.sequenceiq.cloudbreak.core.flow2.cluster.termination;

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ContainerOrchestratorResolver;
import com.sequenceiq.cloudbreak.core.flow.handlers.AmbariClusterEventHandler;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Container;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential;
import com.sequenceiq.cloudbreak.repository.ContainerRepository;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class TerminateClusterHandler implements AmbariClusterEventHandler<TerminateClusterRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TerminateClusterHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterService clusterService;

    @Inject
    private ContainerOrchestratorResolver containerOrchestratorResolver;

    @Inject
    private ContainerRepository containerRepository;

    @Override
    public void accept(Event<TerminateClusterRequest> terminateClusterRequestEvent) {
        LOGGER.info("Received event: {}", terminateClusterRequestEvent);
        TerminateClusterRequest request = terminateClusterRequestEvent.getData();
        Cluster cluster = clusterService.getById(request.getClusterContext().getClusterId());
        try {
            Orchestrator orchestrator = cluster.getStack().getOrchestrator();
            OrchestrationCredential credential = new OrchestrationCredential(orchestrator.getApiEndpoint(), orchestrator.getAttributes().getMap());
            ContainerOrchestrator containerOrchestrator = containerOrchestratorResolver.get(orchestrator.getType());
            Set<Container> containers = containerRepository.findContainersInCluster(cluster.getId());
            List<String> containerIds = FluentIterable.from(containers).transform(new Function<Container, String>() {
                @Nullable
                @Override
                public String apply(Container input) {
                    return input.getContainerId();
                }
            }).toList();
            containerOrchestrator.deleteContainer(containerIds, credential);
        } catch (CloudbreakException | CloudbreakOrchestratorException e) {
            //TODO
        }

        TerminateClusterResult result = new TerminateClusterResult(request);
        eventBus.notify(result.selector(), new Event(terminateClusterRequestEvent.getHeaders(), result));

    }

    @Override
    public Class<TerminateClusterRequest> type() {
        return TerminateClusterRequest.class;
    }
}

package com.sequenceiq.periscope.monitor.evaluator;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.model.ChangedNodesReport;
import com.sequenceiq.periscope.aspects.AmbariRequestLogging;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.FailedNode;
import com.sequenceiq.periscope.log.MDCBuilder;
import com.sequenceiq.periscope.monitor.context.ClusterIdEvaluatorContext;
import com.sequenceiq.periscope.monitor.context.EvaluatorContext;
import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent;
import com.sequenceiq.periscope.repository.FailedNodeRepository;
import com.sequenceiq.periscope.service.AmbariClientProvider;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.configuration.CloudbreakClientConfiguration;

@Component("AmbariAgentHealthEvaluator")
@Scope("prototype")
public class AmbariAgentHealthEvaluator extends EvaluatorExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariAgentHealthEvaluator.class);

    private static final String ALERT_STATE = "state";

    private static final String ALERT_TS = "timestamp";

    private static final String CRITICAL_ALERT_STATE = "CRITICAL";

    private static final String AMBARI_AGENT_HEARTBEAT = "Ambari Agent Heartbeat";

    private static final String AMBARI_AGENT_HEARTBEAT_DEF_NAME = "ambari_server_agent_heartbeat";

    private static final String HOST_NAME = "host_name";

    private static final String EVALUATOR_NAME = AmbariAgentHealthEvaluator.class.getName();

    @Inject
    private ClusterService clusterService;

    @Inject
    private AmbariClientProvider ambariClientProvider;

    @Inject
    private CloudbreakClientConfiguration cloudbreakClientConfiguration;

    @Inject
    private AmbariRequestLogging ambariRequestLogging;

    @Inject
    private EventPublisher eventPublisher;

    @Inject
    private FailedNodeRepository failedNodeRepository;

    private long clusterId;

    @Override
    public void setContext(EvaluatorContext context) {
        clusterId = (long) context.getData();
    }

    @Override
    @Nonnull
    public EvaluatorContext getContext() {
        return new ClusterIdEvaluatorContext(clusterId);
    }

    @Override
    public String getName() {
        return EVALUATOR_NAME;
    }

    @Override
    public void execute() {
        long start = System.currentTimeMillis();
        try {
            Cluster cluster = clusterService.findById(clusterId);
            MDCBuilder.buildMdcContext(cluster);
            LOGGER.info("Checking '{}' alerts for cluster {}.", AMBARI_AGENT_HEARTBEAT, cluster.getId());
            AmbariClient ambariClient = ambariClientProvider.createAmbariClient(cluster);
            List<Map<String, Object>> alerts = ambariRequestLogging.logging(() -> ambariClient.getAlertByNameAndState(
                    AMBARI_AGENT_HEARTBEAT_DEF_NAME, CRITICAL_ALERT_STATE), "alert[" + AMBARI_AGENT_HEARTBEAT_DEF_NAME + ", " + CRITICAL_ALERT_STATE + "]");
            List<String> hostNamesToRecover = new ArrayList<>();
            for (Map<String, Object> history : alerts) {
                String hostName = (String) history.get(HOST_NAME);
                hostNamesToRecover.add(hostName);
                LOGGER.info("Alert: {} is in '{}' state for host '{}'.", AMBARI_AGENT_HEARTBEAT, CRITICAL_ALERT_STATE, hostName);
            }
            List<FailedNode> failedNodes = failedNodeRepository.findByClusterId(clusterId);
            Optional<ChangedNodesReport> changedNodesRequest = getChangedNodes(Set.copyOf(hostNamesToRecover), failedNodes);
            if (changedNodesRequest.isPresent()) {
                LOGGER.info("Nodes state changed for cluster {}. New failed nodes {}, new healthy nodes {}.",
                        clusterId,
                        changedNodesRequest.get().getNewFailedNodes(),
                        changedNodesRequest.get().getNewHealthyNodes());
                try (Response response = cloudbreakClientConfiguration.cloudbreakClient()
                        .autoscaleEndpoint().changedNodesReport(cluster.getStackId(), changedNodesRequest.get())) {
                    if (Status.ACCEPTED.getStatusCode() == response.getStatus()) {
                        updateFailedNodes(failedNodes, changedNodesRequest.get());
                    }
                }
            } else {
                LOGGER.debug("Nodes state not changed for cluster {}", clusterId);
            }
        } catch (Exception e) {
            LOGGER.warn(String.format("Failed to retrieve '%s' alerts. Original message: %s", AMBARI_AGENT_HEARTBEAT, e.getMessage()));
            eventPublisher.publishEvent(new UpdateFailedEvent(clusterId));
        } finally {
            LOGGER.info("Finished {} for cluster {} in {} ms", AMBARI_AGENT_HEARTBEAT, clusterId, System.currentTimeMillis() - start);
        }

    }

    private Optional<ChangedNodesReport> getChangedNodes(Set<String> unhealthyNodes, List<FailedNode> registeredFailedNodes) {
        Set<String> registeredFailedNodeNames = registeredFailedNodes
                .stream()
                .map(FailedNode::getName)
                .collect(toSet());
        Set<String> newFailedNodes = Sets.difference(unhealthyNodes, registeredFailedNodeNames);
        Set<String> newHealthyNodes = Sets.difference(registeredFailedNodeNames, unhealthyNodes);
        if (newFailedNodes.isEmpty() && newHealthyNodes.isEmpty()) {
            return Optional.empty();
        } else {
            ChangedNodesReport request = new ChangedNodesReport();
            request.setNewFailedNodes(List.copyOf(newFailedNodes));
            request.setNewHealthyNodes(List.copyOf(newHealthyNodes));
            return Optional.of(request);
        }
    }

    private void updateFailedNodes(List<FailedNode> failedNodes, ChangedNodesReport changedNodes) {
        if (!changedNodes.getNewFailedNodes().isEmpty()) {
            failedNodeRepository.saveAll(changedNodes.getNewFailedNodes()
                    .stream()
                    .map(name -> {
                        FailedNode node = new FailedNode();
                        node.setClusterId(clusterId);
                        node.setName(name);
                        return node;
                    })
                    .collect(toList()));
        }
        if (!changedNodes.getNewHealthyNodes().isEmpty()) {
            Set<String> healthyNodeNames = Set.copyOf(changedNodes.getNewHealthyNodes());
            List<FailedNode> recoveredNodes = failedNodes
                    .stream()
                    .filter(node -> healthyNodeNames.contains(node.getName()))
                    .collect(toList());
            failedNodeRepository.deleteAll(recoveredNodes);
        }
    }
}

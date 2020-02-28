package com.sequenceiq.periscope.monitor.evaluator;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.ChangedNodesReportV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ClusterManagerVariant;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.FailedNode;
import com.sequenceiq.periscope.monitor.context.ClusterIdEvaluatorContext;
import com.sequenceiq.periscope.monitor.context.EvaluatorContext;
import com.sequenceiq.periscope.repository.FailedNodeRepository;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.evaluator.HostHealthEvaluatorService;

@Component("ClusterManagerHostHealthEvaluator")
@Scope("prototype")
public class ClusterManagerHostHealthEvaluator extends EvaluatorExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterManagerHostHealthEvaluator.class);

    private static final String EVALUATOR_NAME = ClusterManagerHostHealthEvaluator.class.getName();

    @Inject
    private ClusterService clusterService;

    @Inject
    private HostHealthEvaluatorService hostHealthEvaluatorService;

    private long clusterId;

    @Inject
    private FailedNodeRepository failedNodeRepository;

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
        Cluster cluster = clusterService.findById(clusterId);
        ClusterManagerVariant variant = cluster.getClusterManager().getVariant();
        List<String> hostNamesToRecover = hostHealthEvaluatorService.get(variant).determineHostnamesToRecover(cluster);
        List<FailedNode> failedNodes = failedNodeRepository.findByClusterId(clusterId);
        Optional<ChangedNodesReportV4Request> changedNodesRequest = getChangedNodes(Set.copyOf(hostNamesToRecover), failedNodes);
        if (changedNodesRequest.isPresent()) {
            LOGGER.info("Nodes state changed for cluster {}. New failed nodes {}, new healthy nodes {}.",
                    clusterId,
                    changedNodesRequest.get().getNewFailedNodes(),
                    changedNodesRequest.get().getNewHealthyNodes());
            updateFailedNodes(failedNodes, changedNodesRequest.get());
        } else {
            LOGGER.debug("Nodes state not changed for cluster {}", clusterId);
        }
    }

    private Optional<ChangedNodesReportV4Request> getChangedNodes(Set<String> unhealthyNodes, List<FailedNode> registeredFailedNodes) {
        Set<String> registeredFailedNodeNames = registeredFailedNodes
                .stream()
                .map(FailedNode::getName)
                .collect(toSet());
        Set<String> newFailedNodes = Sets.difference(unhealthyNodes, registeredFailedNodeNames);
        Set<String> newHealthyNodes = Sets.difference(registeredFailedNodeNames, unhealthyNodes);
        if (newFailedNodes.isEmpty() && newHealthyNodes.isEmpty()) {
            return Optional.empty();
        } else {
            ChangedNodesReportV4Request request = new ChangedNodesReportV4Request();
            request.setNewFailedNodes(List.copyOf(newFailedNodes));
            request.setNewHealthyNodes(List.copyOf(newHealthyNodes));
            return Optional.of(request);
        }
    }

    private void updateFailedNodes(List<FailedNode> failedNodes, ChangedNodesReportV4Request changedNodes) {
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

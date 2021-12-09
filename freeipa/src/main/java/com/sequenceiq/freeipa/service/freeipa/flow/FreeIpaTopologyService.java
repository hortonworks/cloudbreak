package com.sequenceiq.freeipa.service.freeipa.flow;

import static com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil.ignoreNotFoundException;
import static java.util.function.Predicate.not;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.TopologySegment;
import com.sequenceiq.freeipa.client.model.TopologySuffix;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@Service
public class FreeIpaTopologyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaTopologyService.class);

    private static final Integer MAX_REPLICATION_CONNECTIONS = 4;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    public void updateReplicationTopology(Long stackId, Set<String> fqdnsToExclude, FreeIpaClient freeIpaClient) throws FreeIpaClientException {
        Set<String> allNodesFqdn = instanceMetaDataService.findNotTerminatedForStack(stackId).stream()
                .map(InstanceMetaData::getDiscoveryFQDN)
                .filter(StringUtils::isNotBlank)
                .filter(not(fqdnsToExclude::contains))
                .collect(Collectors.toSet());
        Set<TopologySegment> topology = generateTopology(allNodesFqdn).stream()
                .map(this::convertUnorderedPairToTopologySegment)
                .collect(Collectors.toSet());
        List<TopologySuffix> topologySuffixes = freeIpaClient.findAllTopologySuffixes();
        for (TopologySuffix topologySuffix : topologySuffixes) {
            createMissingSegments(freeIpaClient, topologySuffix.getCn(), topology);
            removeExtraSegments(freeIpaClient, topologySuffix.getCn(), topology);
        }
    }

    private TopologySegment convertUnorderedPairToTopologySegment(UnorderedPair pair) {
        TopologySegment s = new TopologySegment();
        s.setDirection("both");
        s.setLeftNode(pair.getLeft());
        s.setRightNode(pair.getRight());
        s.setCn(String.format("%s-to-%s", pair.getLeft(), pair.getRight()));
        return s;
    }

    @VisibleForTesting
    Set<UnorderedPair> generateTopology(Set<String> nodes) {
        LOGGER.debug("Generating topology for FreeIPA [{}}", nodes);
        Map<String, Integer> connectionRemaining = createConnectionRemainingPerNode(nodes);
        Set<UnorderedPair> generatedTopology = new HashSet<>();
        for (String node : new LinkedHashSet<>(connectionRemaining.keySet())) {
            Integer remainingConnectionForNode = getAndRemoveNodeFromConnectionRemaining(connectionRemaining, node);
            for (int i = remainingConnectionForNode; i > 0; i--) {
                Optional<String> selectedPairForNode = selectPairForNode(connectionRemaining);
                selectedPairForNode.ifPresent(selectedNode -> {
                    decreaseRemainingCount(connectionRemaining, selectedNode);
                    generatedTopology.add(new UnorderedPair(node, selectedNode));
                });
            }
        }
        LOGGER.debug("Generated topology: {}", generatedTopology);
        return generatedTopology;
    }

    private Integer getAndRemoveNodeFromConnectionRemaining(Map<String, Integer> connectionRemaining, String node) {
        Integer remainingConnectionForNode = connectionRemaining.get(node);
        connectionRemaining.remove(node);
        return remainingConnectionForNode;
    }

    private Map<String, Integer> createConnectionRemainingPerNode(Set<String> nodes) {
        int maxConnectionCount = Math.min(nodes.size() - 1, MAX_REPLICATION_CONNECTIONS);
        return nodes.stream()
                .sorted()
                .collect(Collectors.toMap(n -> n, n -> maxConnectionCount, (k, v) -> v, LinkedHashMap::new));
    }

    private Optional<String> selectPairForNode(Map<String, Integer> connectionRemaining) {
        Optional<Integer> maxConnectionRemaining = connectionRemaining.values().stream().max(Integer::compareTo);
        return selectNodeWithMaxConnectionRemainingFirstByName(connectionRemaining, maxConnectionRemaining);
    }

    private Optional<String> selectNodeWithMaxConnectionRemainingFirstByName(Map<String, Integer> connectionRemaining,
            Optional<Integer> maxConnectionRemaining) {
        return maxConnectionRemaining.flatMap(max -> connectionRemaining.entrySet().stream()
                .filter(entry -> max.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .min(String::compareTo));
    }

    private void decreaseRemainingCount(Map<String, Integer> connectionRemainingByNode, String node) {
        connectionRemainingByNode.computeIfPresent(node, (key, connectionRemaining) -> connectionRemaining - 1);
    }

    private void createMissingSegments(FreeIpaClient freeIpaClient, String topologySuffixCn, Set<TopologySegment> topology) throws FreeIpaClientException {
        Set<UnorderedPair> existingTopology = freeIpaClient.findTopologySegments(topologySuffixCn).stream()
                .map(segment -> new UnorderedPair(segment.getLeftNode(), segment.getRightNode()))
                .collect(Collectors.toSet());
        Set<TopologySegment> segmentsToAdd = topology.stream()
                .filter(segment -> !existingTopology.contains(new UnorderedPair(segment.getLeftNode(), segment.getRightNode())))
                .collect(Collectors.toSet());
        for (TopologySegment segment : segmentsToAdd) {
            freeIpaClient.addTopologySegment(topologySuffixCn, segment);
        }
    }

    private void removeExtraSegments(FreeIpaClient freeIpaClient, String topologySuffixCn, Set<TopologySegment> topology) throws FreeIpaClientException {
        Set<UnorderedPair> topologyToKeep = topology.stream()
                .map(segment -> new UnorderedPair(segment.getLeftNode(), segment.getRightNode()))
                .collect(Collectors.toSet());
        Set<TopologySegment> segmentsToRemove = freeIpaClient.findTopologySegments(topologySuffixCn).stream()
                .filter(segment -> !topologyToKeep.contains(new UnorderedPair(segment.getLeftNode(), segment.getRightNode())))
                .collect(Collectors.toSet());
        for (TopologySegment segment : segmentsToRemove) {
            ignoreNotFoundException(() -> freeIpaClient.deleteTopologySegment(topologySuffixCn, segment),
                    "Deleting topology segment for [{}] but it was not found", segment);
        }
    }

    @VisibleForTesting
    static class UnorderedPair {
        private final String left;

        private final String right;

        UnorderedPair(String item1, String item2) {
            if (item1.compareTo(item2) < 0) {
                this.left = item1;
                this.right = item2;
            } else {
                this.left = item2;
                this.right = item1;
            }
        }

        String getLeft() {
            return left;
        }

        String getRight() {
            return right;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof UnorderedPair)) {
                return false;
            }

            UnorderedPair other = (UnorderedPair) obj;

            return left.equals(other.left) && right.equals(other.right);
        }

        @Override
        public int hashCode() {
            return Objects.hash(left, right);
        }

        @Override
        public String toString() {
            return System.lineSeparator() + "UnorderedPair{" +
                    "left='" + left + '\'' +
                    ", right='" + right + '\'' +
                    '}';
        }
    }

}

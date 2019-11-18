package com.sequenceiq.freeipa.service.freeipa.flow;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.TopologySegment;
import com.sequenceiq.freeipa.client.model.TopologySuffix;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.stack.StackService;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Iterator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Map;
import java.util.stream.Collectors;

@Service
class FreeIpaTopologyService {

    private static final Integer MAX_REPLICATION_CONNECTIONS = 4;

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    public void updateReplicationTopology(Long stackId) throws FreeIpaClientException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Set<String> allNodesFqdn = stack.getNotDeletedInstanceMetaDataSet().stream()
                .map(InstanceMetaData::getDiscoveryFQDN)
                .collect(Collectors.toSet());
        FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStack(stack);
        Set<TopologySegment> topology = generateTopology(allNodesFqdn).stream()
                .map(pair -> {
                    TopologySegment s = new TopologySegment();
                    s.setDirection("both");
                    s.setLeftNode(pair.getLeft());
                    s.setRightNode(pair.getRight());
                    s.setCn(String.format("%s-to-%s", pair.getLeft(), pair.getRight()));
                    return s;
                }).collect(Collectors.toSet());
        List<TopologySuffix> topologySuffixes = freeIpaClient.findAllTopologySuffixes();
        for (TopologySuffix topologySuffix : topologySuffixes) {
            createMissingSegments(freeIpaClient, topologySuffix.getCn(), topology);
            removeExtraSegements(freeIpaClient, topologySuffix.getCn(), topology);
        }
    }

    @VisibleForTesting
    Set<UnorderedPair> generateTopology(Set<String> nodes) {
        Map<String, Integer> connectionRemaining = nodes.stream()
                .sorted()
                .collect(Collectors.toMap(n -> n, n -> MAX_REPLICATION_CONNECTIONS, (k, v) -> v, LinkedHashMap::new));
        Set<UnorderedPair> ret = new HashSet<>();
        Iterator<String> itr2 = null;
        for (Map.Entry<String, Integer> entry : connectionRemaining.entrySet()) {
            String node = entry.getKey();
            Integer remainingConnectionsForNode = entry.getValue();
            for (int i = remainingConnectionsForNode; i > 0; i--) {
                // Note: It is not possible for itr2 to reference on object with 0 remaining connections
                // because itr2 is after the node iterator and the node iterator is checked for
                // remaining connections.
                if (itr2 == null || !itr2.hasNext()) {
                    itr2 = getIteratorAfterCurrentNode(node, connectionRemaining.keySet());
                    if (!itr2.hasNext()) {
                        break;
                    }
                }
                String node2 = itr2.next();
                connectionRemaining.put(node2, connectionRemaining.get(node2) - 1);
                ret.add(new UnorderedPair(node, node2));
            }
        }
        return ret;
    }

    @SuppressWarnings("checkstyle:EmptyBlock")
    private Iterator<String> getIteratorAfterCurrentNode(String node, Set<String> keySet) {
        Iterator<String> ret = keySet.iterator();
        while (ret.hasNext()) {
            if (ret.next().equals(node)) {
                break;
            }
        }
        return ret;
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

    private void removeExtraSegements(FreeIpaClient freeIpaClient, String topologySuffixCn, Set<TopologySegment> topology) throws FreeIpaClientException {
        Set<UnorderedPair> topologyToKeep = topology.stream()
                .map(segment -> new UnorderedPair(segment.getLeftNode(), segment.getRightNode()))
                .collect(Collectors.toSet());
        Set<TopologySegment> segmentsToRemove = freeIpaClient.findTopologySegments(topologySuffixCn).stream()
                .filter(segment -> !topologyToKeep.contains(new UnorderedPair(segment.getLeftNode(), segment.getRightNode())))
                .collect(Collectors.toSet());
        for (TopologySegment segment : segmentsToRemove) {
            freeIpaClient.deleteTopologySegment(topologySuffixCn, segment);
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
            if (obj == null || !(obj instanceof UnorderedPair)) {
                return false;
            }

            UnorderedPair other = (UnorderedPair) obj;

            return left.equals(other.left) && right.equals(other.right);
        }

        @Override
        public int hashCode() {
            return Objects.hash(left, right);
        }
    }

}

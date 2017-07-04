package com.sequenceiq.cloudbreak.service.ha;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.CloudbreakNode;

@Service
public class EvenFlowDistributor implements FlowDistributor {

    private static final Logger LOGGER = LoggerFactory.getLogger(EvenFlowDistributor.class);

    @Override
    public Map<CloudbreakNode, List<String>> distribute(List<String> flows, List<CloudbreakNode> nodes) {
        Map<CloudbreakNode, List<String>> result = new HashMap<>();
        int nodeCount = nodes.size();
        int flowCount = flows.size();
        double flowPerNode = (double) flowCount / nodeCount;
        LOGGER.info("Number of flows to distribute: {}, across: {} nodes, f/n: {}", flowCount, nodeCount, flowPerNode);
        int i = 0;
        for (String flow : flows) {
            if (nodeCount == i) {
                i = 0;
            }
            result.computeIfAbsent(nodes.get(i), k -> new ArrayList<>()).add(flow);
            i++;
        }
        return result;
    }
}

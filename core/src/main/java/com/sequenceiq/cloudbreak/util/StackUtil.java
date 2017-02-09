package com.sequenceiq.cloudbreak.util;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorType;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;

@Service
public class StackUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackUtil.class);

    @Inject
    private OrchestratorTypeResolver orchestratorTypeResolver;

    public Set<Node> collectNodes(Stack stack) {
        Set<Node> agents = new HashSet<>();
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            for (InstanceMetaData instanceMetaData : instanceGroup.getInstanceMetaData()) {
                Node node = new Node(instanceMetaData.getPrivateIp(), instanceMetaData.getPublicIp(), instanceMetaData.getDiscoveryFQDN());
                node.setHostGroup(instanceGroup.getGroupName());
                agents.add(node);
            }
        }
        return agents;
    }

    public String extractAmbariIp(Stack stack) {
        String ambariIp = null;
        try {
            OrchestratorType orchestratorType = orchestratorTypeResolver.resolveType(stack.getOrchestrator().getType());
            if (orchestratorType != null && orchestratorType.containerOrchestrator()) {
                ambariIp = stack.getCluster().getAmbariIp();
            } else {
                Set<InstanceMetaData> gateway = stack.getGatewayInstanceGroup().getInstanceMetaData();
                if (stack.getCluster().getAmbariIp() != null && gateway != null && !gateway.isEmpty()) {
                    ambariIp = gateway.iterator().next().getPublicIpWrapper();
                }
            }
        } catch (CloudbreakException ex) {
            LOGGER.error("Could not resolve orchestrator type: ", ex);
        }
        return ambariIp;
    }
}

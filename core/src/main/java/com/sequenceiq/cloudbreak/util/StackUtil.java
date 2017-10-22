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
import com.sequenceiq.cloudbreak.domain.StackMinimal;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;

@Service
public class StackUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackUtil.class);

    @Inject
    private OrchestratorTypeResolver orchestratorTypeResolver;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    public Set<Node> collectNodes(Stack stack) {
        Set<Node> agents = new HashSet<>();
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            for (InstanceMetaData im : instanceGroup.getInstanceMetaData()) {
                agents.add(new Node(im.getPrivateIp(), im.getPublicIp(), im.getDiscoveryFQDN(), im.getInstanceGroupName()));
            }
        }
        return agents;
    }

    public String extractAmbariIp(StackMinimal stack) {
        return extractAmbariIp(stack.getId(), stack.getOrchestrator().getType(), stack.getCluster() != null ? stack.getCluster().getAmbariIp() : null);
    }

    public String extractAmbariIp(Stack stack) {
        return extractAmbariIp(stack.getId(), stack.getOrchestrator().getType(), stack.getCluster() != null ? stack.getCluster().getAmbariIp() : null);
    }

    private String extractAmbariIp(long stackId, String orchestratorName, String ambariIp) {
        String result = null;
        try {
            OrchestratorType orchestratorType = orchestratorTypeResolver.resolveType(orchestratorName);
            if (orchestratorType != null && orchestratorType.containerOrchestrator()) {
                result = ambariIp;
            } else {
                InstanceMetaData gatewayInstance = instanceMetaDataRepository.getPrimaryGatewayInstanceMetadata(stackId);
                if (gatewayInstance != null) {
                    result = gatewayInstance.getPublicIpWrapper();
                }
            }
        } catch (CloudbreakException ex) {
            LOGGER.error("Could not resolve orchestrator type: ", ex);
        }
        return result;
    }
}

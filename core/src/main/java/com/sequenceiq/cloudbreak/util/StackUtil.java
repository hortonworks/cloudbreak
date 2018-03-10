package com.sequenceiq.cloudbreak.util;

import java.time.Duration;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.common.model.OrchestratorType;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.view.StackView;
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

    public String extractAmbariIp(StackView stackView) {
        return extractAmbariIp(stackView.getId(), stackView.getOrchestrator().getType(),
                stackView.getClusterView() != null ? stackView.getClusterView().getAmbariIp() : null);
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

    public long getUptimeForCluster(Cluster cluster, boolean addUpsinceToUptime) {
        Duration uptime = Duration.ZERO;
        if (StringUtils.isNotBlank(cluster.getUptime())) {
            uptime = Duration.parse(cluster.getUptime());
        }
        if (cluster.getUpSince() != null && addUpsinceToUptime) {
            long now = new Date().getTime();
            uptime = uptime.plusMillis(now - cluster.getUpSince());
        }
        return uptime.toMillis();
    }
}

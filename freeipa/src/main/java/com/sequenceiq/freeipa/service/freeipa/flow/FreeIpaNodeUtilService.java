package com.sequenceiq.freeipa.service.freeipa.flow;

import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.freeipa.entity.InstanceMetaData;

@Service
public class FreeIpaNodeUtilService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaNodeUtilService.class);

    public Set<Node> mapInstancesToNodes(Set<InstanceMetaData> instanceMetaDatas) {
        Set<Node> allNodes = instanceMetaDatas.stream()
                .map(im -> new Node(im.getPrivateIp(), im.getPublicIpWrapper(), im.getInstanceId(),
                        im.getInstanceGroup().getTemplate().getInstanceType(), im.getDiscoveryFQDN(), im.getInstanceGroup().getGroupName()))
                .collect(Collectors.toSet());
        validateNodeIsPresent(allNodes);
        return allNodes;
    }

    private void validateNodeIsPresent(Set<Node> allNodes) {
        if (allNodes.isEmpty()) {
            String errorMessage = "There are no nodes FreeIPA nodes.";
            LOGGER.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }
    }
}

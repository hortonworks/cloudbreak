package com.sequenceiq.cloudbreak.util;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;

public class StackUtil {

    private StackUtil() {
    }

    public static Set<Node> collectNodes(Stack stack) {
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
}

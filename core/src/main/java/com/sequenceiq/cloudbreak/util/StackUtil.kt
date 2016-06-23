package com.sequenceiq.cloudbreak.util

import java.util.HashSet

import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.orchestrator.model.Node

object StackUtil {

    fun collectNodes(stack: Stack): Set<Node> {
        val agents = HashSet<Node>()
        for (instanceGroup in stack.instanceGroups) {
            for (instanceMetaData in instanceGroup.instanceMetaData) {
                val node = Node(instanceMetaData.privateIp, instanceMetaData.publicIp, instanceMetaData.discoveryFQDN)
                node.hostGroup = instanceGroup.groupName
                agents.add(node)
            }
        }
        return agents
    }
}

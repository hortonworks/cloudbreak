package com.sequenceiq.cloudbreak.controller.validation.blueprint

import com.google.common.collect.Lists
import com.sequenceiq.cloudbreak.domain.HostGroup

class BlueprintServiceComponent internal constructor(val name: String, hostgroup: String, nodeCount: Int) {
    var nodeCount: Int = 0
        private set
    private val hostgroups: MutableList<String>

    init {
        this.nodeCount = nodeCount
        this.hostgroups = Lists.newArrayList(hostgroup)
    }

    fun update(hostGroup: HostGroup) {
        nodeCount += hostGroup.constraint.hostCount!!
        hostgroups.add(hostGroup.name)
    }

    fun getHostgroups(): List<String> {
        return hostgroups
    }
}

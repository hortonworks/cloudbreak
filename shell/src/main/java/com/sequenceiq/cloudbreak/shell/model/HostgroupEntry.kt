package com.sequenceiq.cloudbreak.shell.model

import java.util.HashSet

class HostgroupEntry(override val nodeCount: Int?, recipeIdSet: Set<Long>) : NodeCountEntry {
    val recipeIdSet: Set<Long> = HashSet()

    init {
        this.recipeIdSet = recipeIdSet
    }
}

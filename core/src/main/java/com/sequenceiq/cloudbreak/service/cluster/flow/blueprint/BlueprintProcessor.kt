package com.sequenceiq.cloudbreak.service.cluster.flow.blueprint

interface BlueprintProcessor {

    fun addConfigEntries(originalBlueprint: String, properties: List<BlueprintConfigurationEntry>, override: Boolean): String

    fun getComponentsInHostGroup(blueprintText: String, hostGroup: String): Set<String>

    fun componentExistsInBlueprint(component: String, blueprintText: String): Boolean

    fun removeComponentFromBlueprint(component: String, blueprintText: String): String

    fun modifyHdpVersion(originalBlueprint: String, hdpVersion: String): String

    fun addComponentToHostgroups(component: String, hostGroupNames: Collection<String>, blueprintText: String): String
}

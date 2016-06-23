package com.sequenceiq.cloudbreak.service.decorator

import javax.inject.Inject

import java.util.HashSet

import com.sequenceiq.cloudbreak.api.model.HostGroupJson
import com.sequenceiq.cloudbreak.controller.validation.blueprint.BlueprintValidator
import com.sequenceiq.cloudbreak.domain.Blueprint
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.HostGroup
import com.sequenceiq.cloudbreak.domain.SssdConfig
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService
import com.sequenceiq.cloudbreak.service.sssdconfig.SssdConfigService
import com.sequenceiq.cloudbreak.service.stack.StackService
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component

@Component
class ClusterDecorator : Decorator<Cluster> {

    private enum class DecorationData {
        STACK_ID,
        USER,
        BLUEPRINT_ID,
        HOSTGROUP_JSONS,
        VALIDATE_BLUEPRINT,
        SSSDCONFIG_ID
    }

    @Inject
    private val blueprintService: BlueprintService? = null

    @Inject
    private val blueprintValidator: BlueprintValidator? = null

    @Inject
    private val stackService: StackService? = null

    @Inject
    private val conversionService: ConversionService? = null

    @Inject
    private val hostGroupDecorator: HostGroupDecorator? = null

    @Inject
    private val sssdConfigService: SssdConfigService? = null

    override fun decorate(subject: Cluster, vararg data: Any): Cluster {
        if (null == data || data.size != DecorationData.values().size) {
            throw IllegalArgumentException("Invalid decoration data provided. Cluster: " + subject.name)
        }
        val stackId = data[DecorationData.STACK_ID.ordinal] as Long
        val user = data[DecorationData.USER.ordinal] as CbUser
        val blueprintId = data[DecorationData.BLUEPRINT_ID.ordinal] as Long
        val hostGroupsJsons = data[DecorationData.HOSTGROUP_JSONS.ordinal] as Set<HostGroupJson>
        subject.blueprint = blueprintService!!.get(blueprintId)
        subject.hostGroups = convertHostGroupsFromJson(stackId, user, subject, hostGroupsJsons)
        val validate = data[DecorationData.VALIDATE_BLUEPRINT.ordinal] as Boolean
        if (validate) {
            val blueprint = blueprintService.get(blueprintId)
            val stack = stackService!!.getById(stackId)
            blueprintValidator!!.validateBlueprintForStack(blueprint, subject.hostGroups, stack.instanceGroups)
        }
        if (data[DecorationData.SSSDCONFIG_ID.ordinal] != null) {
            val config = sssdConfigService!!.get(data[DecorationData.SSSDCONFIG_ID.ordinal] as Long)
            subject.sssdConfig = config
        }
        return subject
    }

    private fun convertHostGroupsFromJson(stackId: Long?, user: CbUser, cluster: Cluster, hostGroupsJsons: Set<HostGroupJson>): Set<HostGroup> {
        val hostGroups = HashSet<HostGroup>()
        for (json in hostGroupsJsons) {
            var hostGroup = conversionService!!.convert<HostGroup>(json, HostGroup::class.java)
            hostGroup.cluster = cluster
            hostGroup = hostGroupDecorator!!.decorate(hostGroup, stackId, user, json.constraint, json.recipeIds, true)
            hostGroups.add(hostGroup)
        }
        return hostGroups
    }

}

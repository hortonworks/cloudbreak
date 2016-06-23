package com.sequenceiq.cloudbreak.converter

import java.util.HashSet
import javax.inject.Inject

import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.TypeDescriptor
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Component

import com.google.common.base.Predicate
import com.google.common.collect.FluentIterable
import com.sequenceiq.cloudbreak.api.model.HostGroupJson
import com.sequenceiq.cloudbreak.api.model.InstanceGroupJson
import com.sequenceiq.cloudbreak.api.model.StackValidationRequest
import com.sequenceiq.cloudbreak.controller.BadRequestException
import com.sequenceiq.cloudbreak.domain.Blueprint
import com.sequenceiq.cloudbreak.domain.Constraint
import com.sequenceiq.cloudbreak.domain.HostGroup
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.Network
import com.sequenceiq.cloudbreak.domain.StackValidation
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService
import com.sequenceiq.cloudbreak.service.network.NetworkService

@Component
class JsonToStackValidationConverter : AbstractConversionServiceAwareConverter<StackValidationRequest, StackValidation>() {

    @Inject
    private val blueprintService: BlueprintService? = null
    @Inject
    private val networkService: NetworkService? = null
    @Inject
    private val conversionService: ConversionService? = null

    override fun convert(stackValidationRequest: StackValidationRequest): StackValidation {
        val stackValidation = StackValidation()
        val instanceGroups = convertInstanceGroups(stackValidationRequest.instanceGroups)
        stackValidation.instanceGroups = instanceGroups
        stackValidation.hostGroups = convertHostGroupsFromJson(instanceGroups, stackValidationRequest.hostGroups)
        try {
            val blueprint = blueprintService!!.get(stackValidationRequest.blueprintId)
            stackValidation.blueprint = blueprint
        } catch (e: AccessDeniedException) {
            throw AccessDeniedException(
                    String.format("Access to blueprint '%s' is denied or blueprint doesn't exist.", stackValidationRequest.blueprintId), e)
        }

        try {
            val network = networkService!!.get(stackValidationRequest.networkId)
            stackValidation.network = network
        } catch (e: AccessDeniedException) {
            throw AccessDeniedException(
                    String.format("Access to network '%s' is denied or network doesn't exist.", stackValidationRequest.networkId), e)
        }

        return stackValidation
    }

    private fun convertHostGroupsFromJson(instanceGroups: Set<InstanceGroup>, hostGroupsJsons: Set<HostGroupJson>): Set<HostGroup> {
        val hostGroups = HashSet<HostGroup>()
        for (json in hostGroupsJsons) {
            val hostGroup = HostGroup()
            hostGroup.name = json.name
            val constraint = conversionService!!.convert<Constraint>(json.constraint, Constraint::class.java)
            val instanceGroupName = json.constraint.instanceGroupName
            if (instanceGroupName != null) {
                val instanceGroup = FluentIterable.from(instanceGroups).firstMatch { instanceGroup -> instanceGroup!!.groupName == instanceGroupName }.get() ?: throw BadRequestException(String.format("Cannot find instance group named '%s' in instance group list", instanceGroupName))
                constraint.instanceGroup = instanceGroup
            }
            hostGroup.constraint = constraint
            hostGroups.add(hostGroup)
        }
        return hostGroups
    }

    private fun convertInstanceGroups(instanceGroupJsons: Set<InstanceGroupJson>): Set<InstanceGroup> {
        return conversionService.convert(instanceGroupJsons, TypeDescriptor.forObject(instanceGroupJsons),
                TypeDescriptor.collection(Set<Any>::class.java, TypeDescriptor.valueOf(InstanceGroup::class.java))) as Set<InstanceGroup>
    }
}

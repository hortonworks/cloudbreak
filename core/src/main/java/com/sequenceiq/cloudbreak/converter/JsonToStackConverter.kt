package com.sequenceiq.cloudbreak.converter

import java.util.Calendar
import java.util.HashMap

import javax.inject.Inject

import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.TypeDescriptor
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.InstanceGroupJson
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType
import com.sequenceiq.cloudbreak.api.model.StackRequest
import com.sequenceiq.cloudbreak.api.model.Status
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation
import com.sequenceiq.cloudbreak.controller.BadRequestException
import com.sequenceiq.cloudbreak.domain.FailurePolicy
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.Orchestrator
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.service.stack.StackParameterService

@Component
class JsonToStackConverter : AbstractConversionServiceAwareConverter<StackRequest, Stack>() {

    @Inject
    private val conversionService: ConversionService? = null

    @Inject
    private val stackParameterService: StackParameterService? = null

    override fun convert(source: StackRequest): Stack {
        val stack = Stack()
        stack.name = source.name
        stack.region = source.region
        stack.availabilityZone = source.availabilityZone
        stack.onFailureActionAction = source.onFailureAction
        stack.status = Status.REQUESTED
        stack.instanceGroups = convertInstanceGroups(source.instanceGroups, stack)
        stack.failurePolicy = conversionService.convert<FailurePolicy>(source.failurePolicy, FailurePolicy::class.java)
        stack.parameters = getValidParameters(source)
        stack.created = Calendar.getInstance().timeInMillis
        stack.platformVariant = source.platformVariant
        stack.orchestrator = conversionService!!.convert<Orchestrator>(source.orchestrator, Orchestrator::class.java)
        stack.relocateDocker = if (source.relocateDocker == null) true else source.relocateDocker
        return stack
    }

    private fun getValidParameters(stackRequest: StackRequest): Map<String, String> {
        val params = HashMap<String, String>()
        val userParams = stackRequest.parameters
        if (userParams != null) {
            for (stackParamValidation in stackParameterService!!.getStackParams(stackRequest)) {
                val paramName = stackParamValidation.name
                val value = userParams[paramName]
                if (value != null) {
                    params.put(paramName, value)
                }
            }
        }
        return params
    }

    private fun convertInstanceGroups(instanceGroupJsons: List<InstanceGroupJson>, stack: Stack): Set<InstanceGroup> {
        val convertedSet = conversionService.convert(instanceGroupJsons, TypeDescriptor.forObject(instanceGroupJsons),
                TypeDescriptor.collection(Set<Any>::class.java, TypeDescriptor.valueOf(InstanceGroup::class.java))) as Set<InstanceGroup>
        var gatewaySpecified = false
        for (instanceGroup in convertedSet) {
            instanceGroup.stack = stack
            if (!gatewaySpecified) {
                if (InstanceGroupType.GATEWAY == instanceGroup.instanceGroupType) {
                    gatewaySpecified = true
                }
            } else if (InstanceGroupType.GATEWAY == instanceGroup.instanceGroupType) {
                throw BadRequestException("Only 1 Ambari server can be specified")
            }
        }
        if (!gatewaySpecified) {
            throw BadRequestException("Ambari server must be specified")
        }
        return convertedSet
    }
}

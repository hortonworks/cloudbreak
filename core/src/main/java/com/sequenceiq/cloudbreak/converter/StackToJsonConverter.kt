package com.sequenceiq.cloudbreak.converter

import java.util.ArrayList

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.TypeDescriptor
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.ClusterResponse
import com.sequenceiq.cloudbreak.api.model.FailurePolicyJson
import com.sequenceiq.cloudbreak.api.model.ImageJson
import com.sequenceiq.cloudbreak.api.model.InstanceGroupJson
import com.sequenceiq.cloudbreak.api.model.OrchestratorResponse
import com.sequenceiq.cloudbreak.api.model.StackResponse
import com.sequenceiq.cloudbreak.cloud.model.Image
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.service.image.ImageService

@Component
class StackToJsonConverter : AbstractConversionServiceAwareConverter<Stack, StackResponse>() {

    @Inject
    private val conversionService: ConversionService? = null

    @Inject
    private val imageService: ImageService? = null

    override fun convert(source: Stack): StackResponse {
        val stackJson = StackResponse()
        try {
            val image = imageService!!.getImage(source.id)
            stackJson.image = conversionService.convert<ImageJson>(image, ImageJson::class.java)
        } catch (exc: CloudbreakImageNotFoundException) {
            LOGGER.info(exc.message)
        }

        stackJson.name = source.name
        stackJson.owner = source.owner
        stackJson.account = source.account
        stackJson.isPublicInAccount = source.isPublicInAccount
        stackJson.id = source.id
        if (source.credential == null) {
            stackJson.cloudPlatform = null
            stackJson.credentialId = null
        } else {
            stackJson.cloudPlatform = source.cloudPlatform()
            stackJson.credentialId = source.credential.id
        }
        stackJson.status = source.status
        stackJson.statusReason = source.statusReason
        stackJson.region = source.region
        stackJson.availabilityZone = source.availabilityZone
        stackJson.onFailureAction = source.onFailureActionAction
        if (source.securityGroup != null) {
            stackJson.securityGroupId = source.securityGroup.id
        }
        val templateGroups = ArrayList<InstanceGroupJson>()
        templateGroups.addAll(convertInstanceGroups(source.instanceGroups))
        stackJson.instanceGroups = templateGroups
        if (source.cluster != null) {
            stackJson.cluster = conversionService.convert<ClusterResponse>(source.cluster, ClusterResponse::class.java)
        } else {
            stackJson.cluster = ClusterResponse()
        }
        if (source.failurePolicy != null) {
            stackJson.failurePolicy = conversionService.convert<FailurePolicyJson>(source.failurePolicy, FailurePolicyJson::class.java)
        }
        if (source.network == null) {
            stackJson.networkId = null
        } else {
            stackJson.networkId = source.network.id
        }
        stackJson.relocateDocker = source.relocateDocker
        stackJson.parameters = source.parameters
        stackJson.platformVariant = source.platformVariant
        if (source.orchestrator != null) {
            stackJson.orchestrator = conversionService!!.convert<OrchestratorResponse>(source.orchestrator, OrchestratorResponse::class.java)
        }
        stackJson.created = source.created
        stackJson.gatewayPort = source.gatewayPort
        return stackJson
    }

    private fun convertInstanceGroups(instanceGroups: Set<InstanceGroup>): Set<InstanceGroupJson> {
        return conversionService.convert(instanceGroups, TypeDescriptor.forObject(instanceGroups),
                TypeDescriptor.collection(Set<Any>::class.java, TypeDescriptor.valueOf(InstanceGroupJson::class.java))) as Set<InstanceGroupJson>
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(StackToJsonConverter::class.java)
    }

}

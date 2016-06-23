package com.sequenceiq.cloudbreak.converter.spi

import javax.inject.Inject

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Template

@Component
class InstanceMetaDataToCloudInstanceConverter : AbstractConversionServiceAwareConverter<InstanceMetaData, CloudInstance>() {

    @Inject
    private val stackToCloudStackConverter: StackToCloudStackConverter? = null

    override fun convert(metaDataEnity: InstanceMetaData): CloudInstance {
        val group = metaDataEnity.instanceGroup
        val template = metaDataEnity.instanceGroup.template
        val status = getInstanceStatus(metaDataEnity)
        val instanceTemplate = stackToCloudStackConverter!!.buildInstanceTemplate(
                template, group.groupName, metaDataEnity.privateId, status)
        return CloudInstance(metaDataEnity.instanceId, instanceTemplate)
    }

    private fun getInstanceStatus(metaData: InstanceMetaData): InstanceStatus {
        when (metaData.instanceStatus) {

            InstanceStatus.REQUESTED -> return InstanceStatus.CREATE_REQUESTED
            InstanceStatus.CREATED -> return InstanceStatus.CREATED
            InstanceStatus.UNREGISTERED, InstanceStatus.REGISTERED -> return InstanceStatus.STARTED
            InstanceStatus.DECOMMISSIONED -> return InstanceStatus.DELETE_REQUESTED
            InstanceStatus.TERMINATED -> return InstanceStatus.TERMINATED
            else -> return InstanceStatus.UNKNOWN
        }
    }

}

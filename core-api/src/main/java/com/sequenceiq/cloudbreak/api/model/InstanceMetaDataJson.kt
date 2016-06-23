package com.sequenceiq.cloudbreak.api.model

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceGroupModelDescription
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceMetaDataModelDescription

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("InstanceMetaData")
class InstanceMetaDataJson : JsonEntity {

    @ApiModelProperty(InstanceMetaDataModelDescription.PRIVATE_IP)
    var privateIp: String? = null
    @ApiModelProperty(InstanceMetaDataModelDescription.PUBLIC_IP)
    var publicIp: String? = null
    @ApiModelProperty
    var sshPort: Int? = null
    @ApiModelProperty(InstanceMetaDataModelDescription.INSTANCE_ID)
    var instanceId: String? = null
    @ApiModelProperty(InstanceMetaDataModelDescription.AMBARI_SERVER)
    var ambariServer: Boolean? = null
    @ApiModelProperty(InstanceMetaDataModelDescription.DISCOVERY_FQDN)
    var discoveryFQDN: String? = null
    @ApiModelProperty(InstanceGroupModelDescription.INSTANCE_GROUP_NAME)
    var instanceGroup: String? = null
    @ApiModelProperty(InstanceGroupModelDescription.STATUS)
    var instanceStatus: InstanceStatus? = null
}

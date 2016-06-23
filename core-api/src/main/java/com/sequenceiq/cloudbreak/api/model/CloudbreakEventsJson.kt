package com.sequenceiq.cloudbreak.api.model

import com.sequenceiq.cloudbreak.doc.ModelDescriptions
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.BlueprintModelDescription
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.EventModelDescription
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceGroupModelDescription
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("CloudbreakEvent")
class CloudbreakEventsJson : JsonEntity {

    @ApiModelProperty(EventModelDescription.TYPE)
    var eventType: String? = null
    @ApiModelProperty(EventModelDescription.TIMESTAMP)
    var eventTimestamp: Long = 0
    @ApiModelProperty(EventModelDescription.MESSAGE)
    var eventMessage: String? = null
    @ApiModelProperty(ModelDescriptions.OWNER)
    var owner: String? = null
    @ApiModelProperty(ModelDescriptions.ACCOUNT)
    var account: String? = null
    @ApiModelProperty(ModelDescriptions.CLOUD_PLATFORM)
    var cloud: String? = null
    @ApiModelProperty(StackModelDescription.REGION)
    var region: String? = null
    @ApiModelProperty(StackModelDescription.AVAILABILITY_ZONE)
    var availabilityZone: String? = null
    @ApiModelProperty(StackModelDescription.BLUEPRINT_ID)
    var blueprintId: Long = 0
    @ApiModelProperty(BlueprintModelDescription.BLUEPRINT_NAME)
    var blueprintName: String? = null
    @ApiModelProperty(ClusterModelDescription.CLUSTER_ID)
    var clusterId: Long? = null
    @ApiModelProperty(ClusterModelDescription.CLUSTER_NAME)
    var clusterName: String? = null
    @ApiModelProperty(StackModelDescription.STACK_ID)
    var stackId: Long? = null
    @ApiModelProperty(StackModelDescription.STACK_NAME)
    var stackName: String? = null
    @ApiModelProperty(StackModelDescription.STACK_STATUS)
    var stackStatus: Status? = null
    @ApiModelProperty(InstanceGroupModelDescription.NODE_COUNT)
    var nodeCount: Int? = null
    @ApiModelProperty(InstanceGroupModelDescription.INSTANCE_GROUP_NAME)
    var instanceGroup: String? = null
    @ApiModelProperty(StackModelDescription.CLUSTER_STATUS)
    var clusterStatus: Status? = null
}

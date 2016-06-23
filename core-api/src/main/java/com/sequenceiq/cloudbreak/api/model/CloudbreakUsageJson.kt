package com.sequenceiq.cloudbreak.api.model

import com.sequenceiq.cloudbreak.doc.ModelDescriptions
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.UsageModelDescription
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("CloudbreakUsage")
class CloudbreakUsageJson : JsonEntity {
    @ApiModelProperty(ModelDescriptions.OWNER)
    var owner: String? = null

    @ApiModelProperty(StackModelDescription.USERNAME)
    var username: String? = null

    @ApiModelProperty(ModelDescriptions.ACCOUNT)
    var account: String? = null

    @ApiModelProperty(UsageModelDescription.DAY)
    var day: String? = null

    @ApiModelProperty(UsageModelDescription.PROVIDER)
    var provider: String? = null

    @ApiModelProperty(StackModelDescription.REGION)
    var region: String? = null

    @ApiModelProperty(StackModelDescription.AVAILABILITY_ZONE)
    var availabilityZone: String? = null

    @ApiModelProperty(UsageModelDescription.INSTANCE_HOURS)
    var instanceHours: Long? = null

    @ApiModelProperty(StackModelDescription.STACK_ID)
    var stackId: Long? = null

    @ApiModelProperty(StackModelDescription.STACK_NAME)
    var stackName: String? = null

    @ApiModelProperty(UsageModelDescription.COSTS)
    var costs: Double? = null

    @ApiModelProperty(UsageModelDescription.INSTANCE_TYPE)
    var instanceType: String? = null

    @ApiModelProperty(UsageModelDescription.INSTANCE_GROUP)
    var instanceGroup: String? = null
}

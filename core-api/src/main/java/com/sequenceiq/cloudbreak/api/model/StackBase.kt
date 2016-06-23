package com.sequenceiq.cloudbreak.api.model

import java.util.ArrayList
import java.util.HashMap

import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.sequenceiq.cloudbreak.doc.ModelDescriptions
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription

import io.swagger.annotations.ApiModelProperty

@JsonIgnoreProperties(ignoreUnknown = true)
abstract class StackBase : JsonEntity {
    @Size(max = 40, min = 5, message = "The length of the name has to be in range of 5 to 40")
    @Pattern(regexp = "([a-z][-a-z0-9]*[a-z0-9])", message = "The name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    @ApiModelProperty(value = StackModelDescription.STACK_NAME, required = true)
    var name: String? = null
    @ApiModelProperty(value = StackModelDescription.AVAILABILITY_ZONE, required = false)
    var availabilityZone: String? = null
    //    @NotNull
    @ApiModelProperty(value = StackModelDescription.REGION, required = true)
    var region: String? = null
    @ApiModelProperty(value = ModelDescriptions.CLOUD_PLATFORM, required = true)
    var cloudPlatform: String? = null
    @ApiModelProperty(StackModelDescription.PLATFORM_VARIANT)
    var platformVariant: String? = null
    //    @NotNull
    @ApiModelProperty(value = StackModelDescription.CREDENTIAL_ID, required = true)
    var credentialId: Long? = null
    @ApiModelProperty(StackModelDescription.FAILURE_ACTION)
    var onFailureAction = OnFailureAction.DO_NOTHING
    @ApiModelProperty(StackModelDescription.FAILURE_POLICY)
    var failurePolicy: FailurePolicyJson? = null
    @Valid
    @ApiModelProperty(required = true)
    var instanceGroups: List<InstanceGroupJson> = ArrayList()
    //    @NotNull
    @ApiModelProperty(value = StackModelDescription.SECURITY_GROUP_ID, required = true)
    var securityGroupId: Long? = null
    //    @NotNull
    @ApiModelProperty(value = StackModelDescription.NETWORK_ID, required = true)
    var networkId: Long? = null
    @ApiModelProperty(value = StackModelDescription.RELOCATE_DOCKER, required = false)
    var relocateDocker: Boolean? = null

    @ApiModelProperty(StackModelDescription.PARAMETERS)
    var parameters: Map<String, String> = HashMap()
}

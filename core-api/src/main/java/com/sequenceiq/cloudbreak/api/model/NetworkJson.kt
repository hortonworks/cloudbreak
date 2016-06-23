package com.sequenceiq.cloudbreak.api.model

import java.util.HashMap

import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.sequenceiq.cloudbreak.doc.ModelDescriptions
import com.sequenceiq.cloudbreak.validation.ValidSubnet

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
class NetworkJson : JsonEntity {
    @ApiModelProperty(value = ModelDescriptions.ID, required = false)
    var id: String? = null
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    @Size(max = 100, min = 1, message = "The length of the network's name has to be in range of 1 to 100")
    @Pattern(regexp = "([a-z][-a-z0-9]*[a-z0-9])", message = "The network's name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    var name: String? = null
    @ApiModelProperty(value = ModelDescriptions.DESCRIPTION, required = false)
    @Size(max = 1000)
    var description: String? = null
    @ApiModelProperty(value = ModelDescriptions.PUBLIC_IN_ACCOUNT, required = true)
    @NotNull
    var isPublicInAccount: Boolean = false
    @ApiModelProperty(value = ModelDescriptions.NetworkModelDescription.SUBNET_CIDR, required = true)
    @ValidSubnet
    var subnetCIDR: String? = null
    @ApiModelProperty(value = ModelDescriptions.NetworkModelDescription.PARAMETERS, required = true)
    var parameters: Map<String, Any> = HashMap()
    @ApiModelProperty(value = ModelDescriptions.CLOUD_PLATFORM, required = true)
    @NotNull
    var cloudPlatform: String? = null

    @ApiModelProperty(value = ModelDescriptions.TOPOLOGY_ID)
    var topologyId: Long? = null
}

package com.sequenceiq.cloudbreak.api.model

import javax.validation.constraints.Pattern

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.sequenceiq.cloudbreak.doc.ModelDescriptions
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("SecurityRule")
@JsonIgnoreProperties(ignoreUnknown = true)
class SecurityRuleJson : JsonEntity {

    @ApiModelProperty(value = ModelDescriptions.ID, required = false)
    var id: Long? = null
    @ApiModelProperty(value = ModelDescriptions.SecurityRuleModelDescription.SUBNET, required = true)
    @Pattern(regexp = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])(\\/([0-9]|[1-2][0-9]|3[0-2]))$", message = "The subnet field should contain a valid CIDR definition.")
    var subnet: String? = null
    @ApiModelProperty(value = ModelDescriptions.SecurityRuleModelDescription.PORTS, required = true)
    @Pattern(regexp = "^[0-9]+(,[0-9]+)*$", message = "The ports field should contain a comma separated list of port numbers, for example: 8080,9090,5555")
    var ports: String? = null
    @ApiModelProperty(value = ModelDescriptions.SecurityRuleModelDescription.PROTOCOL, required = true)
    var protocol: String? = null
    @ApiModelProperty(value = ModelDescriptions.SecurityRuleModelDescription.MODIFIABLE, required = false)
    var isModifiable: Boolean = false

    constructor() {
    }

    constructor(subnet: String) {
        this.subnet = subnet
    }
}

package com.sequenceiq.cloudbreak.api.model

import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

import java.util.LinkedList

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.sequenceiq.cloudbreak.doc.ModelDescriptions
import io.swagger.annotations.ApiModelProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class SecurityGroupJson {
    @ApiModelProperty(value = ModelDescriptions.ID, required = false)
    var id: Long? = null
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    @Size(max = 100, min = 1, message = "The length of the security group's name has to be in range of 1 to 100")
    @Pattern(regexp = "([a-z][-a-z0-9]*[a-z0-9])", message = "The security group's name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    var name: String? = null
    @ApiModelProperty(value = ModelDescriptions.OWNER, required = false)
    var owner: String? = null
    @ApiModelProperty(value = ModelDescriptions.ACCOUNT, required = false)
    var account: String? = null
    @ApiModelProperty(value = ModelDescriptions.PUBLIC_IN_ACCOUNT, required = false)
    @NotNull
    var isPublicInAccount: Boolean = false
    @ApiModelProperty(value = ModelDescriptions.DESCRIPTION, required = false)
    @Size(max = 1000)
    var description: String? = null
    @Valid
    @ApiModelProperty(value = ModelDescriptions.SecurityGroupModelDescription.SECURITY_RULES, required = true)
    var securityRules: List<SecurityRuleJson> = LinkedList()
}

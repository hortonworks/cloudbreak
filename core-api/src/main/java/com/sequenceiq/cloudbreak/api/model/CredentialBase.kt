package com.sequenceiq.cloudbreak.api.model

import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

import java.util.HashMap

import com.sequenceiq.cloudbreak.doc.ModelDescriptions
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel
open class CredentialBase : JsonEntity {
    @Size(max = 100, min = 5, message = "The length of the credential's name has to be in range of 5 to 100")
    @Pattern(regexp = "([a-z][-a-z0-9]*[a-z0-9])", message = "The name of the credential can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    var name: String? = null
    @ApiModelProperty(value = ModelDescriptions.CLOUD_PLATFORM, required = true)
    var cloudPlatform: String? = null
    @ApiModelProperty(value = ModelDescriptions.CredentialModelDescription.PARAMETERS, required = true)
    var parameters: Map<String, Any> = HashMap()
    @Size(max = 1000)
    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    var description: String? = null
    @ApiModelProperty(ModelDescriptions.CredentialModelDescription.LOGIN_USERNAME)
    var loginUserName: String? = null
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.CredentialModelDescription.PUBLIC_KEY, required = true)
    var publicKey: String? = null

    @ApiModelProperty(value = ModelDescriptions.TOPOLOGY_ID)
    var topologyId: Long? = null
}

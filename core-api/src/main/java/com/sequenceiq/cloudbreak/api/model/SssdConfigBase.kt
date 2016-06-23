package com.sequenceiq.cloudbreak.api.model

import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

import com.sequenceiq.cloudbreak.doc.ModelDescriptions

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel
internal abstract class SssdConfigBase : JsonEntity {

    @Size(max = 100, min = 1, message = "The length of the config's name has to be in range of 1 to 100")
    @Pattern(regexp = "([a-z][-a-z0-9]*[a-z0-9])", message = "The config's name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    var name: String? = null
    @Size(max = 1000, message = "The length of the config's description has to be less than 1000")
    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    var description: String? = null
    @ApiModelProperty(value = ModelDescriptions.SssdConfigModelDescription.PROVIDER_TYPE, required = true)
    var providerType: SssdProviderType? = null
    @Size(min = 10, max = 255, message = "The length of the config's url has to be in range of 10 to 255")
    @ApiModelProperty(value = ModelDescriptions.SssdConfigModelDescription.URL, required = true)
    var url: String? = null
    @ApiModelProperty(value = ModelDescriptions.SssdConfigModelDescription.SCHEMA, required = true)
    var schema: SssdSchemaType? = null
    @Size(min = 10, max = 255, message = "The length of the config's search base has to be in range of 10 to 255")
    @ApiModelProperty(value = ModelDescriptions.SssdConfigModelDescription.BASE_SEARCH, required = true)
    var baseSearch: String? = null
    @ApiModelProperty(value = ModelDescriptions.SssdConfigModelDescription.TLS_REQUCERT, required = true)
    var tlsReqcert: SssdTlsReqcertType? = null
    @Size(max = 255, message = "The length of the active directory server has to be less than 255")
    @ApiModelProperty(value = ModelDescriptions.SssdConfigModelDescription.AD_SERVER)
    var adServer: String? = null
    @Size(max = 255, message = "The length of the kerberos server(s) has to be less than 255")
    @ApiModelProperty(value = ModelDescriptions.SssdConfigModelDescription.KERBEROS_SERVER)
    var kerberosServer: String? = null
    @Size(max = 255, message = "The length of the kerberos realm has to be less than 255")
    @ApiModelProperty(value = ModelDescriptions.SssdConfigModelDescription.KERBEROS_REALM)
    var kerberosRealm: String? = null
    @ApiModelProperty(value = ModelDescriptions.SssdConfigModelDescription.CONFIGURATION)
    var configuration: String? = null
}

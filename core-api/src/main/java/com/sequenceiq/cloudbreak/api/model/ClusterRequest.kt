package com.sequenceiq.cloudbreak.api.model

import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

import com.sequenceiq.cloudbreak.doc.ModelDescriptions
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription

import io.swagger.annotations.ApiModelProperty

class ClusterRequest {

    @Size(max = 40, min = 5, message = "The length of the cluster's name has to be in range of 5 to 40")
    @Pattern(regexp = "([a-z][-a-z0-9]*[a-z0-9])", message = "The name of the cluster can only contain lowercase alphanumeric characters and hyphens and has to start with an alphanumeric character")
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    var name: String? = null
    @NotNull
    @ApiModelProperty(value = ClusterModelDescription.BLUEPRINT_ID, required = true)
    var blueprintId: Long? = null
    @Size(max = 1000)
    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    var description: String? = null
    @Valid
    @NotNull
    @ApiModelProperty(required = true)
    var hostGroups: Set<HostGroupJson>? = null
    @ApiModelProperty(ClusterModelDescription.EMAIL_NEEDED)
    var emailNeeded = java.lang.Boolean.FALSE
    var enableSecurity = java.lang.Boolean.FALSE
    @Size(max = 15, min = 5, message = "The length of the username has to be in range of 5 to 15")
    @Pattern(regexp = "([a-z][-a-z0-9]*[a-z0-9])", message = "The username can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.StackModelDescription.USERNAME, required = true)
    var userName: String? = null
    @NotNull
    @Size(max = 50, min = 5, message = "The length of the password has to be in range of 5 to 50")
    @ApiModelProperty(value = ModelDescriptions.StackModelDescription.PASSWORD, required = true)
    var password: String? = null
    @Size(max = 50, min = 3, message = "The length of the Kerberos password has to be in range of 3 to 50")
    var kerberosMasterKey: String? = null
    @Size(max = 15, min = 5, message = "The length of the Kerberos admin has to be in range of 5 to 15")
    var kerberosAdmin: String? = null
    @Size(max = 50, min = 5, message = "The length of the Kerberos password has to be in range of 5 to 50")
    var kerberosPassword: String? = null
    @ApiModelProperty(value = ClusterModelDescription.LDAP_REQUIRED, required = false)
    var ldapRequired: Boolean? = false
    @ApiModelProperty(value = ClusterModelDescription.SSSDCONFIG_ID, required = false)
    var sssdConfigId: Long? = null
    private var validateBlueprint: Boolean? = true
    @Valid
    var ambariStackDetails: AmbariStackDetailsJson? = null
    @Valid
    var rdsConfigJson: RDSConfigJson? = null
    @Valid
    var fileSystem: FileSystemRequest? = null
    @ApiModelProperty(ClusterModelDescription.CONFIG_STRATEGY)
    var configStrategy = ConfigStrategy.ALWAYS_APPLY_DONT_OVERRIDE_CUSTOM_VALUES
    @ApiModelProperty(value = ClusterModelDescription.ENABLE_SHIPYARD, required = false)
    var enableShipyard = java.lang.Boolean.FALSE

    val validateBlueprint: Boolean
        get() = if (validateBlueprint == null) false else validateBlueprint

    fun setValidateBlueprint(validateBlueprint: Boolean?) {
        this.validateBlueprint = validateBlueprint
    }
}

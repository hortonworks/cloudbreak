package com.sequenceiq.cloudbreak.api.model

import com.sequenceiq.cloudbreak.doc.ModelDescriptions
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("UserNamePasswordJson")
class UserNamePasswordJson {

    @ApiModelProperty(value = ModelDescriptions.UserNamePasswordModelDescription.NEW_USER_NAME, required = true)
    var userName: String? = null
    @ApiModelProperty(value = ModelDescriptions.UserNamePasswordModelDescription.OLD_PASSWORD, required = true)
    var oldPassword: String? = null
    @ApiModelProperty(value = ModelDescriptions.UserNamePasswordModelDescription.NEW_PASSWORD, required = true)
    var password: String? = null
}

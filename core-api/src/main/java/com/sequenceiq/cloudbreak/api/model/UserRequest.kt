package com.sequenceiq.cloudbreak.api.model

import io.swagger.annotations.ApiModel

@ApiModel("User")
class UserRequest {

    var username: String? = null
}

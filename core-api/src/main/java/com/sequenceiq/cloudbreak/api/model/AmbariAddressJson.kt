package com.sequenceiq.cloudbreak.api.model


import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("AmbariAddress")
class AmbariAddressJson : JsonEntity {

    @ApiModelProperty(required = true)
    var ambariAddress: String? = null
}

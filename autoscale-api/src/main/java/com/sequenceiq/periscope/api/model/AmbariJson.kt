package com.sequenceiq.periscope.api.model

import com.sequenceiq.periscope.doc.ApiDescription.AmbariJsonProperties

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("AmbariJson")
class AmbariJson : Json {

    @ApiModelProperty(AmbariJsonProperties.HOST)
    var host: String? = null
    @ApiModelProperty(AmbariJsonProperties.PORT)
    var port: String? = null
    @ApiModelProperty(AmbariJsonProperties.USERNAME)
    var user: String? = null
    @ApiModelProperty(AmbariJsonProperties.PASSWORD)
    var pass: String? = null

    constructor() {
    }

    constructor(host: String, port: String, user: String, pass: String) {
        this.host = host
        this.port = port
        this.user = user
        this.pass = pass
    }
}

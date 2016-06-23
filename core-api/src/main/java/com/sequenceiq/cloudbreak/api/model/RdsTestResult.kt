package com.sequenceiq.cloudbreak.api.model

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("RdsTestResult")
class RdsTestResult : JsonEntity {

    @ApiModelProperty(required = true)
    var connectionResult: String? = null

    constructor() {

    }

    constructor(connectionResult: String) {
        this.connectionResult = connectionResult
    }
}

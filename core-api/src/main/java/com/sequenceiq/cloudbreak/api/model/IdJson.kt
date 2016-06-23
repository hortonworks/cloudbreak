package com.sequenceiq.cloudbreak.api.model

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("Id")
class IdJson : JsonEntity {

    @ApiModelProperty(required = true)
    var id: Long? = null

    constructor() {

    }

    constructor(id: Long?) {
        this.id = id
    }
}

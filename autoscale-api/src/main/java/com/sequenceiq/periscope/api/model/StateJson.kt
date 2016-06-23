package com.sequenceiq.periscope.api.model

import com.sequenceiq.periscope.doc.ApiDescription.StateJsonProperties

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("AmbariJson")
class StateJson : Json {

    @ApiModelProperty(StateJsonProperties.STATE)
    var state: ClusterState? = null
}

package com.sequenceiq.periscope.api.model

import com.sequenceiq.periscope.doc.ApiDescription.ClusterJsonProperties

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("ClusterJson")
class ClusterJson : Json {

    @ApiModelProperty(ClusterJsonProperties.ID)
    var id: Long = 0
    @ApiModelProperty(ClusterJsonProperties.HOST)
    var host: String? = null
    @ApiModelProperty(ClusterJsonProperties.PORT)
    var port: String? = null
    @ApiModelProperty(ClusterJsonProperties.STATE)
    var state: String? = null
    @ApiModelProperty(ClusterJsonProperties.STACKID)
    var stackId: Long? = null
}

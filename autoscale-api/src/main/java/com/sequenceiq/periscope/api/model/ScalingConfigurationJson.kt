package com.sequenceiq.periscope.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.sequenceiq.periscope.doc.ApiDescription.ScalingConfigurationJsonProperties

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("ScalingConfigurationJson")
class ScalingConfigurationJson : Json {

    @ApiModelProperty(ScalingConfigurationJsonProperties.MINSIZE)
    var minSize: Int = 0
    @ApiModelProperty(ScalingConfigurationJsonProperties.MAXSIZE)
    var maxSize: Int = 0
    @ApiModelProperty(ScalingConfigurationJsonProperties.COOLDOWN)
    @JsonProperty("cooldown")
    var coolDown: Int = 0
}

package com.sequenceiq.cloudbreak.api.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ImageModelDescription

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
class ImageJson : JsonEntity {
    @ApiModelProperty(ImageModelDescription.IMAGE_NAME)
    var imageName: String? = null
    @ApiModelProperty(ImageModelDescription.HDPVERSION)
    var hdpVersion: String? = null
}

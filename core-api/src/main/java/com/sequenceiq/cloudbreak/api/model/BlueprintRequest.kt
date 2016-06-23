package com.sequenceiq.cloudbreak.api.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.BlueprintModelDescription
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
class BlueprintRequest : BlueprintBase() {
    @ApiModelProperty(value = BlueprintModelDescription.URL)
    var url: String? = null
}

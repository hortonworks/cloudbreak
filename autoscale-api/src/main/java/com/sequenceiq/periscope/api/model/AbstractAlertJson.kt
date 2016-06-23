package com.sequenceiq.periscope.api.model

import javax.validation.constraints.Pattern

import com.sequenceiq.periscope.doc.ApiDescription.BaseAlertJsonProperties

import io.swagger.annotations.ApiModelProperty

abstract class AbstractAlertJson : Json {

    @ApiModelProperty(BaseAlertJsonProperties.ID)
    var id: Long? = null
    @Pattern(regexp = "([a-zA-Z][-a-zA-Z0-9]*)", message = "The name can only contain alphanumeric characters and hyphens and has start with an alphanumeric character")
    @ApiModelProperty(BaseAlertJsonProperties.ALERTNAME)
    var alertName: String? = null
    @ApiModelProperty(BaseAlertJsonProperties.DESCRIPTION)
    var description: String? = null
    @ApiModelProperty(BaseAlertJsonProperties.SCALINGPOLICYID)
    var scalingPolicyId: Long? = null

}

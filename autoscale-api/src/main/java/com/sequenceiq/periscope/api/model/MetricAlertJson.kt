package com.sequenceiq.periscope.api.model

import com.sequenceiq.periscope.doc.ApiDescription.MetricAlertJsonProperties

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("MetricAlertJson")
class MetricAlertJson : AbstractAlertJson() {

    @ApiModelProperty(MetricAlertJsonProperties.ALERTDEFINITION)
    var alertDefinition: String? = null
    @ApiModelProperty(MetricAlertJsonProperties.PERIOD)
    var period: Int = 0
    @ApiModelProperty(MetricAlertJsonProperties.ALERTSTATE)
    var alertState: AlertState? = null
}

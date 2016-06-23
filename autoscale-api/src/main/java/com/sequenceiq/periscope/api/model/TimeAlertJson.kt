package com.sequenceiq.periscope.api.model

import com.sequenceiq.periscope.doc.ApiDescription.TimeAlertJsonProperties

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("MetricAlertJson")
class TimeAlertJson : AbstractAlertJson() {

    @ApiModelProperty(TimeAlertJsonProperties.TIMEZONE)
    var timeZone: String? = null
    @ApiModelProperty(TimeAlertJsonProperties.CRON)
    var cron: String? = null

}

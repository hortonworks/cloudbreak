package com.sequenceiq.periscope.api.model

import java.util.HashMap

import com.sequenceiq.periscope.doc.ApiDescription.HistoryJsonProperties

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("HistoryJson")
class HistoryJson : Json {

    @ApiModelProperty(HistoryJsonProperties.ID)
    var id: Long = 0
    @ApiModelProperty(HistoryJsonProperties.CLUSTERID)
    var clusterId: Long = 0
    @ApiModelProperty(HistoryJsonProperties.CBSTACKID)
    var cbStackId: Long? = null
    @ApiModelProperty(HistoryJsonProperties.ORIGINALNODECOUNT)
    var originalNodeCount: Int = 0
    @ApiModelProperty(HistoryJsonProperties.ADJUSTMENT)
    var adjustment: Int = 0
    @ApiModelProperty(HistoryJsonProperties.ADJUSTMENTTYPE)
    var adjustmentType: AdjustmentType? = null
    @ApiModelProperty(HistoryJsonProperties.SCALINGSTATUS)
    var scalingStatus: ScalingStatus? = null
    @ApiModelProperty(HistoryJsonProperties.STATUSREASON)
    var statusReason: String? = null
    @ApiModelProperty(HistoryJsonProperties.TIMESTAMP)
    var timestamp: Long = 0
    @ApiModelProperty(HistoryJsonProperties.HOSTGROUP)
    var hostGroup: String? = null
    @ApiModelProperty(HistoryJsonProperties.ALERTTYPE)
    var alertType: AlertType? = null
    @ApiModelProperty(HistoryJsonProperties.PROPERTIES)
    var properties: Map<String, String> = HashMap()
}

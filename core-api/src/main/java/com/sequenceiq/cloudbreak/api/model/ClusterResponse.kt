package com.sequenceiq.cloudbreak.api.model

import java.util.HashMap

import com.fasterxml.jackson.annotation.JsonRawValue
import com.fasterxml.jackson.databind.JsonNode
import com.sequenceiq.cloudbreak.doc.ModelDescriptions
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription
import io.swagger.annotations.ApiModelProperty

class ClusterResponse {

    @ApiModelProperty(ModelDescriptions.ID)
    var id: Long? = null
    @ApiModelProperty(ModelDescriptions.NAME)
    var name: String? = null
    @ApiModelProperty(ClusterModelDescription.STATUS)
    var status: String? = null
    @ApiModelProperty(ClusterModelDescription.HOURS)
    var hoursUp: Int = 0
    @ApiModelProperty(ClusterModelDescription.MINUTES)
    var minutesUp: Int = 0
    @ApiModelProperty(ClusterModelDescription.CLUSTER_NAME)
    var cluster: String? = null
        private set
    @ApiModelProperty(ClusterModelDescription.BLUEPRINT_ID)
    var blueprintId: Long? = null
    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    var description: String? = null
    @ApiModelProperty(ClusterModelDescription.STATUS_REASON)
    var statusReason: String? = null
    @ApiModelProperty(ModelDescriptions.StackModelDescription.AMBARI_IP)
    var ambariServerIp: String? = null
    @ApiModelProperty(value = ModelDescriptions.StackModelDescription.USERNAME, required = true)
    var userName: String? = null
    @ApiModelProperty(value = ModelDescriptions.StackModelDescription.PASSWORD, required = true)
    var password: String? = null
    var isSecure: Boolean = false
    @ApiModelProperty(value = ClusterModelDescription.LDAP_REQUIRED, required = false)
    var ldapRequired: Boolean? = false
    @ApiModelProperty(value = ClusterModelDescription.SSSDCONFIG_ID, required = false)
    var sssdConfigId: Long? = null
    var hostGroups: Set<HostGroupJson>? = null
    var ambariStackDetails: AmbariStackDetailsJson? = null
    var rdsConfigJson: RDSConfigJson? = null
    @ApiModelProperty(ClusterModelDescription.SERVICE_ENDPOINT_MAP)
    var serviceEndPoints: Map<String, String> = HashMap()
    @ApiModelProperty(ClusterModelDescription.CONFIG_STRATEGY)
    var configStrategy: ConfigStrategy? = null
    @ApiModelProperty(ClusterModelDescription.ENABLE_SHIPYARD)
    var enableShipyard: Boolean? = null

    fun setCluster(node: JsonNode) {
        this.cluster = node.toString()
    }
}

package com.sequenceiq.cloudbreak.service.notification

import java.util.Date

import com.sequenceiq.cloudbreak.api.model.Status

class Notification {

    var eventType: String? = null
    var eventTimestamp: Date? = null
    var eventMessage: String? = null
    var owner: String? = null
    var account: String? = null
    var cloud: String? = null
    var region: String? = null
    var blueprintName: String? = null
    var blueprintId: Long? = null
    var stackId: Long? = null
    var stackName: String? = null
    var clusterId: Long? = null
    var clusterName: String? = null
    var stackStatus: Status? = null
    var nodeCount: Int? = null
    var instanceGroup: String? = null
    var clusterStatus: Status? = null
}

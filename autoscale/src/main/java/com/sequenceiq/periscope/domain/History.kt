package com.sequenceiq.periscope.domain

import java.util.HashMap

import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.MapKeyColumn
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery
import javax.persistence.SequenceGenerator

import com.sequenceiq.periscope.api.model.AdjustmentType
import com.sequenceiq.periscope.api.model.AlertType
import com.sequenceiq.periscope.api.model.ScalingStatus

@Entity
@NamedQueries(@NamedQuery(name = "History.findAllByCluster", query = "SELECT c FROM History c WHERE c.clusterId= :id"), @NamedQuery(name = "History.findByCluster", query = "SELECT c FROM History c WHERE c.clusterId= :clusterId AND c.id= :historyId"))
class History {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "history_generator")
    @SequenceGenerator(name = "history_generator", sequenceName = "sequence_table")
    var id: Long = 0

    @Column(name = "cluster_id")
    var clusterId: Long = 0
        private set

    @Column(name = "cb_stack_id")
    var cbStackId: Long? = null

    @Column(name = "original_node_count")
    var originalNodeCount: Int = 0

    var adjustment: Int = 0

    @Column(name = "adjustment_type")
    @Enumerated(EnumType.STRING)
    var adjustmentType: AdjustmentType? = null

    @Column(name = "user_id")
    var userId: String? = null

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    var scalingStatus: ScalingStatus? = null

    @Column(name = "status_reason")
    var statusReason: String? = null

    var timestamp: Long = 0

    @Column(name = "host_group")
    var hostGroup: String? = null

    @Column(name = "alert_type")
    @Enumerated(EnumType.STRING)
    var alertType: AlertType? = null

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "key")
    @Column(name = "value", columnDefinition = "TEXT", length = 100000)
    var properties: MutableMap<String, String> = HashMap()
        get() = properties

    constructor() {
    }

    constructor(status: ScalingStatus, statusReason: String, originalNodeCount: Int) {
        this.scalingStatus = status
        this.statusReason = statusReason
        this.originalNodeCount = originalNodeCount
        this.timestamp = System.currentTimeMillis()
    }

    fun withScalingPolicy(policy: ScalingPolicy): History {
        this.adjustment = policy.scalingAdjustment
        this.adjustmentType = policy.adjustmentType
        this.hostGroup = policy.hostGroup
        return this
    }

    fun withAlert(alert: BaseAlert): History {
        if (alert is MetricAlert) {
            this.properties.put(ALERT_DEFINITION, alert.definitionName)
            this.properties.put(PERIOD, "" + alert.period)
            this.properties.put(ALERT_STATE, alert.alertState.name)
            this.alertType = AlertType.METRIC
        } else if (alert is TimeAlert) {
            this.properties.put(TIME_ZONE, alert.timeZone)
            this.properties.put(CRON, alert.cron)
            this.alertType = AlertType.TIME
        }
        this.properties.put(ALERT_DESCRIPTION, alert.description)
        return this
    }

    fun withCluster(cluster: Cluster): History {
        this.clusterId = cluster.id
        this.cbStackId = cluster.stackId
        this.userId = cluster.user.id
        return this
    }

    fun setClusterId(clusterId: Int) {
        this.clusterId = clusterId.toLong()
    }

    companion object {

        val ALERT_DEFINITION = "alertDefinition"
        val PERIOD = "period"
        val ALERT_STATE = "alertState"
        val ALERT_DESCRIPTION = "alertDescription"
        val TIME_ZONE = "timeZone"
        val CRON = "cron"
    }
}

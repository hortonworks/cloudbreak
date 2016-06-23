package com.sequenceiq.periscope.domain

import java.util.HashSet

import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.SequenceGenerator

import com.sequenceiq.periscope.api.model.ClusterState
import com.sequenceiq.periscope.model.AmbariStack

@Entity
@NamedQueries(@NamedQuery(name = "Cluster.findAllByUser", query = "SELECT c FROM Cluster c WHERE c.user.id= :id"), @NamedQuery(name = "Cluster.find", query = "SELECT c FROM Cluster c WHERE c.id= :id"), @NamedQuery(name = "Cluster.findAllByState", query = "SELECT c FROM Cluster c WHERE c.state= :state"))
class Cluster {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cluster_generator")
    @SequenceGenerator(name = "cluster_generator", sequenceName = "sequence_table")
    var id: Long = 0

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    var ambari: Ambari? = null

    @OneToOne(mappedBy = "cluster", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
    var securityConfig: SecurityConfig? = null

    @ManyToOne
    @JoinColumn(name = "user_id")
    var user: PeriscopeUser? = null

    @Enumerated(EnumType.STRING)
    var state = ClusterState.RUNNING

    @OneToMany(mappedBy = "cluster", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    var metricAlerts: MutableSet<MetricAlert> = HashSet()
        get() = metricAlerts

    @OneToMany(mappedBy = "cluster", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    var timeAlerts: MutableSet<TimeAlert> = HashSet()
        get() = timeAlerts

    @Column(name = "min_size")
    var minSize = DEFAULT_MIN_SIZE

    @Column(name = "max_size")
    var maxSize = DEFAULT_MAX_SIZE

    @Column(name = "cooldown")
    var coolDown = DEFAULT_COOLDOWN

    @Column(name = "cb_stack_id")
    var stackId: Long? = null

    @Column(name = "last_scaling_activity")
    @Volatile var lastScalingActivity: Long = 0

    constructor() {
    }

    constructor(user: PeriscopeUser, ambariStack: AmbariStack) {
        this.user = user
        this.stackId = ambariStack.stackId
        this.ambari = ambariStack.ambari
    }

    fun update(ambariStack: AmbariStack) {
        val ambari = ambariStack.ambari
        this.ambari!!.host = ambari.host
        this.ambari!!.port = ambari.port
        this.ambari!!.user = ambari.user
        this.ambari!!.pass = ambari.pass
    }

    val host: String
        get() = ambari!!.host

    val port: String
        get() = ambari!!.port

    val ambariUser: String
        get() = ambari!!.user

    val ambariPass: String
        get() = ambari!!.pass

    @Synchronized fun setLastScalingActivityCurrent() {
        this.lastScalingActivity = System.currentTimeMillis()
    }

    val isRunning: Boolean
        get() = ClusterState.RUNNING == state

    fun addMetricAlert(alert: MetricAlert) {
        this.metricAlerts.add(alert)
    }

    fun addTimeAlert(alert: TimeAlert) {
        this.timeAlerts.add(alert)
    }

    companion object {

        private val DEFAULT_MIN_SIZE = 3
        private val DEFAULT_MAX_SIZE = 100
        private val DEFAULT_COOLDOWN = 30
    }
}



package com.sequenceiq.periscope.domain

import javax.persistence.Column
import javax.persistence.DiscriminatorValue
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.ManyToOne
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery

import com.sequenceiq.periscope.api.model.AlertState

@Entity
@DiscriminatorValue("METRIC")
@NamedQueries(@NamedQuery(name = "MetricAlert.findByCluster", query = "SELECT c FROM MetricAlert c WHERE c.cluster.id= :clusterId AND c.id= :alertId"), @NamedQuery(name = "MetricAlert.findAllByCluster", query = "SELECT c FROM MetricAlert c WHERE c.cluster.id= :clusterId"))
class MetricAlert : BaseAlert() {

    @ManyToOne
    override var cluster: Cluster? = null

    @Column(name = "definition_name")
    var definitionName: String? = null

    var period: Int = 0

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_state")
    var alertState: AlertState? = null
}
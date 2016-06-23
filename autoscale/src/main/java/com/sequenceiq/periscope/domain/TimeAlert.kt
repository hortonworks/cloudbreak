package com.sequenceiq.periscope.domain

import javax.persistence.Column
import javax.persistence.DiscriminatorValue
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery

@Entity
@DiscriminatorValue("TIME")
@NamedQueries(@NamedQuery(name = "TimeAlert.findByCluster", query = "SELECT c FROM TimeAlert c WHERE c.cluster.id= :clusterId AND c.id= :alertId"), @NamedQuery(name = "TimeAlert.findAllByCluster", query = "SELECT c FROM TimeAlert c WHERE c.cluster.id= :clusterId"))
class TimeAlert : BaseAlert() {

    @ManyToOne
    override var cluster: Cluster? = null

    @Column(name = "time_zone")
    var timeZone: String? = null

    var cron: String? = null
}

package com.sequenceiq.periscope.domain

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery
import javax.persistence.OneToOne
import javax.persistence.SequenceGenerator

import com.sequenceiq.periscope.api.model.AdjustmentType

@Entity
@NamedQueries(@NamedQuery(name = "ScalingPolicy.findByCluster", query = "SELECT c FROM ScalingPolicy c WHERE c.alert.cluster.id= :clusterId AND c.id= :policyId"), @NamedQuery(name = "ScalingPolicy.findAllByCluster", query = "SELECT c FROM ScalingPolicy c WHERE c.alert.cluster.id= :id"))
class ScalingPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "policy_generator")
    @SequenceGenerator(name = "policy_generator", sequenceName = "sequence_table")
    var id: Long = 0

    var name: String? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_type")
    var adjustmentType: AdjustmentType? = null

    @Column(name = "scaling_adjustment")
    var scalingAdjustment: Int = 0

    @OneToOne(mappedBy = "scalingPolicy")
    var alert: BaseAlert? = null

    @Column(name = "host_group")
    var hostGroup: String? = null

    val alertId: Long
        get() = this.alert!!.id
}

package com.sequenceiq.periscope.domain

import javax.persistence.CascadeType
import javax.persistence.DiscriminatorColumn
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Inheritance
import javax.persistence.InheritanceType
import javax.persistence.OneToOne
import javax.persistence.SequenceGenerator

@Entity
@DiscriminatorColumn(name = "alert_type")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
abstract class BaseAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "alert_generator")
    @SequenceGenerator(name = "alert_generator", sequenceName = "sequence_table")
    var id: Long = 0

    var name: String? = null

    var description: String? = null

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    var scalingPolicy: ScalingPolicy? = null

    val scalingPolicyId: Long?
        get() = if (scalingPolicy == null) null else scalingPolicy!!.id

    abstract val cluster: Cluster

}

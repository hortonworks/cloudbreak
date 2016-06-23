package com.sequenceiq.cloudbreak.domain

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@Entity
@Table(name = "hostgroup_constraint")
class Constraint {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "constraint_template_generator")
    @SequenceGenerator(name = "constraint_template_generator", sequenceName = "hostgroup_constraint_id_seq", allocationSize = 1)
    var id: Long? = null

    @ManyToOne
    var instanceGroup: InstanceGroup? = null

    @ManyToOne
    var constraintTemplate: ConstraintTemplate? = null

    var hostCount: Int? = null
}

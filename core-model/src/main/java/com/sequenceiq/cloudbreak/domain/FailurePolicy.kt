package com.sequenceiq.cloudbreak.domain

import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.SequenceGenerator

import com.sequenceiq.cloudbreak.api.model.AdjustmentType

@Entity
class FailurePolicy : ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "failurepolicy_generator")
    @SequenceGenerator(name = "failurepolicy_generator", sequenceName = "failurepolicy_id_seq", allocationSize = 1)
    var id: Long? = null
    var threshold: Long? = null
    @Enumerated(EnumType.STRING)
    var adjustmentType: AdjustmentType? = null
}

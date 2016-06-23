package com.sequenceiq.cloudbreak.domain

import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.SequenceGenerator

import com.sequenceiq.cloudbreak.api.model.RDSDatabase

@Entity
class RDSConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "rdsconfig_generator")
    @SequenceGenerator(name = "rdsconfig_generator", sequenceName = "rdsconfig_id_seq", allocationSize = 1)
    var id: Long? = null

    var connectionURL: String? = null
    @Enumerated(EnumType.STRING)
    var databaseType: RDSDatabase? = null
    var connectionUserName: String? = null
    var connectionPassword: String? = null
}
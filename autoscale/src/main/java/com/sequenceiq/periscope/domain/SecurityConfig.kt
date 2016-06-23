package com.sequenceiq.periscope.domain

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery
import javax.persistence.OneToOne
import javax.persistence.SequenceGenerator
import javax.persistence.Table


@Entity
@Table(name = "SecurityConfig")
@NamedQueries(@NamedQuery(name = "SecurityConfig.findByClusterId", query = "SELECT s FROM SecurityConfig s WHERE s.cluster.id= :id"))
class SecurityConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "securityconfig_generator")
    @SequenceGenerator(name = "securityconfig_generator", sequenceName = "securityconfig_table")
    var id: Long? = null
    @Column
    var clientKey: ByteArray? = null
    @Column
    var clientCert: ByteArray? = null
    @Column
    var serverCert: ByteArray? = null

    @OneToOne
    var cluster: Cluster? = null

    constructor() {
    }

    constructor(clientKey: ByteArray, clientCert: ByteArray, serverCert: ByteArray) {
        this.clientKey = clientKey
        this.clientCert = clientCert
        this.serverCert = serverCert
    }

    fun update(updatedConfig: SecurityConfig) {
        this.clientCert = updatedConfig.clientCert
        this.clientKey = updatedConfig.clientKey
        this.serverCert = updatedConfig.serverCert
    }
}

package com.sequenceiq.cloudbreak.domain

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.NamedQuery
import javax.persistence.OneToOne
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@Entity
@Table(name = "SecurityConfig")
@NamedQuery(name = "SecurityConfig.getServerCertByStackId", query = "SELECT s.serverCert FROM SecurityConfig s " + "WHERE s.stack.id= :stackId")
class SecurityConfig : ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "securityconfig_generator")
    @SequenceGenerator(name = "securityconfig_generator", sequenceName = "securityconfig_id_seq", allocationSize = 1)
    var id: Long? = null
    @Column(columnDefinition = "TEXT")
    var clientKey: String? = null
    @Column(columnDefinition = "TEXT")
    var clientCert: String? = null
    @Column(columnDefinition = "TEXT")
    var serverCert: String? = null
    @Column(columnDefinition = "TEXT")
    var temporarySshPublicKey: String? = null
    @Column(columnDefinition = "TEXT")
    var temporarySshPrivateKey: String? = null

    @OneToOne(fetch = FetchType.LAZY)
    var stack: Stack? = null
}

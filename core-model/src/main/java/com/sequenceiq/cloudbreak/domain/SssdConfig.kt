package com.sequenceiq.cloudbreak.domain

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery
import javax.persistence.SequenceGenerator
import javax.persistence.Table
import javax.persistence.UniqueConstraint

import com.sequenceiq.cloudbreak.api.model.SssdProviderType
import com.sequenceiq.cloudbreak.api.model.SssdSchemaType
import com.sequenceiq.cloudbreak.api.model.SssdTlsReqcertType

@Entity
@Table(name = "sssdconfig", uniqueConstraints = arrayOf(@UniqueConstraint(columnNames = { "account", "name" })))
@NamedQueries(@NamedQuery(
        name = "SssdConfig.findForUser",
        query = "SELECT c FROM SssdConfig c "
                + "WHERE c.owner= :owner"), @NamedQuery(
        name = "SssdConfig.findPublicInAccountForUser",
        query = "SELECT c FROM SssdConfig c "
                + "WHERE (c.account= :account AND c.publicInAccount= true) "
                + "OR c.owner= :owner"), @NamedQuery(
        name = "SssdConfig.findAllInAccount",
        query = "SELECT c FROM SssdConfig c "
                + "WHERE c.account= :account "), @NamedQuery(
        name = "SssdConfig.findByNameForUser",
        query = "SELECT c FROM SssdConfig c "
                + "WHERE c.name= :name and c.owner= :owner "), @NamedQuery(
        name = "SssdConfig.findByNameInAccount",
        query = "SELECT c FROM SssdConfig c WHERE c.name= :name and c.account= :account"))
class SssdConfig : ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "sssdconfig_generator")
    @SequenceGenerator(name = "sssdconfig_generator", sequenceName = "sssdconfig_id_seq", allocationSize = 1)
    var id: Long? = null

    var name: String? = null

    @Column(length = 1000, columnDefinition = "TEXT")
    var description: String? = null

    var account: String? = null

    var owner: String? = null

    var isPublicInAccount: Boolean = false

    @Enumerated(EnumType.STRING)
    var providerType: SssdProviderType? = null

    var url: String? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "ldapschema")
    var schema: SssdSchemaType? = null

    @Column(length = 500, columnDefinition = "TEXT")
    var baseSearch: String? = null

    @Enumerated(EnumType.STRING)
    var tlsReqcert = SssdTlsReqcertType.HARD

    var adServer: String? = null

    var kerberosServer: String? = null

    var kerberosRealm: String? = null

    @Column(length = 1000, columnDefinition = "TEXT")
    var configuration: String? = null
}

package com.sequenceiq.cloudbreak.domain

import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery
import javax.persistence.SequenceGenerator
import javax.persistence.Table
import javax.persistence.UniqueConstraint

import com.sequenceiq.cloudbreak.common.type.ResourceStatus
import com.sequenceiq.cloudbreak.domain.json.Json
import com.sequenceiq.cloudbreak.domain.json.JsonToString

@Entity
@Table(uniqueConstraints = arrayOf(@UniqueConstraint(columnNames = { "account", "name" })))
@NamedQueries(@NamedQuery(
        name = "Network.findByTopology",
        query = "SELECT r FROM Network r "
                + "WHERE r.topology.id= :topologyId"), @NamedQuery(
        name = "Network.findOneById",
        query = "SELECT r FROM Network r "
                + "WHERE r.id= :id"), @NamedQuery(
        name = "Network.findOneByName",
        query = "SELECT r FROM Network r "
                + "WHERE r.name= :name "
                + "AND r.status <> 'DEFAULT_DELETED' "), @NamedQuery(
        name = "Network.findByNameForUser",
        query = "SELECT r FROM Network r "
                + "WHERE r.name= :name "
                + "AND r.owner= :owner "
                + "AND r.status <> 'DEFAULT_DELETED' "), @NamedQuery(
        name = "Network.findByNameInAccount",
        query = "SELECT r FROM Network r "
                + "WHERE r.name= :name "
                + "AND r.account= :account "
                + "AND r.status <> 'DEFAULT_DELETED' "), @NamedQuery(
        name = "Network.findByName",
        query = "SELECT r FROM Network r "
                + "WHERE r.name= :name "), @NamedQuery(
        name = "Network.findForUser",
        query = "SELECT r FROM Network r "
                + "WHERE r.owner= :owner "
                + "AND r.status <> 'DEFAULT_DELETED' "), @NamedQuery(
        name = "Network.findPublicInAccountForUser",
        query = "SELECT r FROM Network r "
                + "WHERE ((r.account= :account AND r.publicInAccount= true) OR r.owner= :owner) "
                + "AND r.status <> 'DEFAULT_DELETED' "), @NamedQuery(
        name = "Network.findAllInAccount",
        query = "SELECT r FROM Network r "
                + "WHERE r.account= :account "
                + "AND r.status <> 'DEFAULT_DELETED' "), @NamedQuery(
        name = "Network.findAllDefaultInAccount",
        query = "SELECT r FROM Network r "
                + "WHERE r.account= :account "
                + "AND (r.status = 'DEFAULT_DELETED' OR r.status = 'DEFAULT') "))
class Network {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "network_generator")
    @SequenceGenerator(name = "network_generator", sequenceName = "network_id_seq", allocationSize = 1)
    var id: Long? = null

    @Column(nullable = false)
    var name: String? = null

    var subnetCIDR: String? = null

    @Column(length = 1000000, columnDefinition = "TEXT")
    var description: String? = null

    var owner: String? = null

    var account: String? = null

    var isPublicInAccount: Boolean = false

    @Enumerated(EnumType.STRING)
    var status: ResourceStatus? = null

    @Column(nullable = false)
    private var cloudPlatform: String? = null

    @Convert(converter = JsonToString::class)
    @Column(columnDefinition = "TEXT")
    var attributes: Json? = null

    @ManyToOne
    var topology: Topology? = null

    fun cloudPlatform(): String {
        return cloudPlatform
    }

    fun setCloudPlatform(cloudPlatform: String) {
        this.cloudPlatform = cloudPlatform
    }
}

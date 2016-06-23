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
        name = "Template.findByTopology",
        query = "SELECT t FROM Template t "
                + "WHERE t.topology.id = :topologyId"), @NamedQuery(
        name = "Template.findForUser",
        query = "SELECT t FROM Template t "
                + "WHERE t.owner= :user AND deleted IS NOT TRUE "
                + "AND t.status <> 'DEFAULT_DELETED' "), @NamedQuery(
        name = "Template.findPublicInAccountForUser",
        query = "SELECT t FROM Template t "
                + "WHERE ((t.account= :account AND t.publicInAccount= true) "
                + "OR t.owner= :user) AND deleted IS NOT TRUE "
                + "AND t.status <> 'DEFAULT_DELETED' "), @NamedQuery(
        name = "Template.findAllInAccount",
        query = "SELECT t FROM Template t "
                + "WHERE t.account= :account AND deleted IS NOT TRUE "
                + "AND t.status <> 'DEFAULT_DELETED' "), @NamedQuery(
        name = "Template.findOneByName",
        query = "SELECT t FROM Template t "
                + "WHERE t.name= :name and t.account= :account AND deleted IS NOT TRUE "
                + "AND t.status <> 'DEFAULT_DELETED' "), @NamedQuery(
        name = "Template.findByIdInAccount",
        query = "SELECT t FROM Template t "
                + "WHERE t.id= :id and t.account= :account AND deleted IS NOT TRUE "
                + "AND t.status <> 'DEFAULT_DELETED' "), @NamedQuery(
        name = "Template.findByNameInAccount",
        query = "SELECT t FROM Template t "
                + "WHERE t.name= :name and ((t.account= :account and t.publicInAccount=true) or t.owner= :owner) "
                + "AND deleted IS NOT TRUE "
                + "AND t.status <> 'DEFAULT_DELETED' "), @NamedQuery(
        name = "Template.findByNameInUser",
        query = "SELECT t FROM Template t "
                + "WHERE t.owner= :owner and t.name= :name AND deleted IS NOT TRUE "
                + "AND t.status <> 'DEFAULT_DELETED' "), @NamedQuery(
        name = "Template.findAllDefaultInAccount",
        query = "SELECT t FROM Template t "
                + "WHERE t.account= :account "
                + "AND (t.status = 'DEFAULT_DELETED' OR t.status = 'DEFAULT') "))
class Template {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "template_generator")
    @SequenceGenerator(name = "template_generator", sequenceName = "template_id_seq", allocationSize = 1)
    var id: Long? = null

    @Column(nullable = false)
    var name: String? = null
    @Column(length = 1000000, columnDefinition = "TEXT")
    var description: String? = null

    var instanceType: String? = null

    var owner: String? = null
    var account: String? = null

    var isPublicInAccount: Boolean = false

    var volumeCount: Int? = null
    var volumeSize: Int? = null
    var volumeType: String? = null

    var isDeleted: Boolean = false

    @Enumerated(EnumType.STRING)
    var status: ResourceStatus? = null

    @Column(nullable = false)
    private var cloudPlatform: String? = null

    @ManyToOne
    var topology: Topology? = null

    @Convert(converter = JsonToString::class)
    @Column(columnDefinition = "TEXT")
    var attributes: Json? = null

    init {
        isDeleted = false
    }

    fun cloudPlatform(): String {
        return cloudPlatform
    }

    fun setCloudPlatform(cloudPlatform: String) {
        this.cloudPlatform = cloudPlatform
    }
}

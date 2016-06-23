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

import com.sequenceiq.cloudbreak.common.type.ResourceStatus

@Entity
@Table(name = "Blueprint", uniqueConstraints = arrayOf(@UniqueConstraint(columnNames = { "account", "name" })))
@NamedQueries(@NamedQuery(
        name = "Blueprint.findForUser",
        query = "SELECT b FROM Blueprint b "
                + "WHERE b.owner= :user "
                + "AND b.status <> 'DEFAULT_DELETED' "), @NamedQuery(
        name = "Blueprint.findPublicInAccountForUser",
        query = "SELECT b FROM Blueprint b "
                + "WHERE ((b.account= :account AND b.publicInAccount= true) "
                + "OR b.owner= :user) "
                + "AND b.status <> 'DEFAULT_DELETED' "), @NamedQuery(
        name = "Blueprint.findAllInAccount",
        query = "SELECT b FROM Blueprint b "
                + "WHERE b.account= :account "
                + "AND b.status <> 'DEFAULT_DELETED' "), @NamedQuery(
        name = "Blueprint.findOneByName",
        query = "SELECT b FROM Blueprint b "
                + "WHERE b.name= :name and b.account= :account "
                + "AND b.status <> 'DEFAULT_DELETED' "), @NamedQuery(
        name = "Blueprint.findByIdInAccount",
        query = "SELECT b FROM Blueprint b "
                + "WHERE  b.id= :id and b.account= :account "
                + "AND b.status <> 'DEFAULT_DELETED' "), @NamedQuery(
        name = "Blueprint.findByNameInAccount",
        query = "SELECT b FROM Blueprint b "
                + "WHERE  b.name= :name and ((b.publicInAccount=true and b.account= :account) or b.owner= :owner) "
                + "AND b.status <> 'DEFAULT_DELETED' "), @NamedQuery(
        name = "Blueprint.findByNameInUser",
        query = "SELECT b FROM Blueprint b "
                + "WHERE b.owner= :owner and b.name= :name "
                + "AND b.status <> 'DEFAULT_DELETED' "), @NamedQuery(
        name = "Blueprint.findAllDefaultInAccount",
        query = "SELECT b FROM Blueprint b "
                + "WHERE b.account= :account "
                + "AND (b.status = 'DEFAULT_DELETED' OR b.status = 'DEFAULT') "))
class Blueprint : ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "blueprint_generator")
    @SequenceGenerator(name = "blueprint_generator", sequenceName = "blueprint_id_seq", allocationSize = 1)
    var id: Long? = null

    @Column(nullable = false)
    var name: String? = null

    @Column(length = 1000000, columnDefinition = "TEXT")
    var blueprintText: String? = null

    var blueprintName: String? = null
    @Column(length = 1000000, columnDefinition = "TEXT")
    var description: String? = null

    var hostGroupCount: Int = 0

    var owner: String? = null
    var account: String? = null

    var isPublicInAccount: Boolean = false

    @Enumerated(EnumType.STRING)
    var status: ResourceStatus? = null
}

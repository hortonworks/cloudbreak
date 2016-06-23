package com.sequenceiq.cloudbreak.domain

import java.util.HashSet

import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery
import javax.persistence.OneToMany
import javax.persistence.SequenceGenerator
import javax.persistence.Table
import javax.persistence.UniqueConstraint

import com.sequenceiq.cloudbreak.common.type.ResourceStatus

@Entity
@Table(uniqueConstraints = arrayOf(@UniqueConstraint(columnNames = { "account", "name" })))
@NamedQueries(@NamedQuery(
        name = "SecurityGroup.findById",
        query = "SELECT r FROM SecurityGroup r "
                + "LEFT JOIN FETCH r.securityRules "
                + "WHERE r.id= :id"), @NamedQuery(
        name = "SecurityGroup.findOneById",
        query = "SELECT r FROM SecurityGroup r "
                + "LEFT JOIN FETCH r.securityRules "
                + "WHERE r.id= :id"), @NamedQuery(
        name = "SecurityGroup.findByNameForUser",
        query = "SELECT r FROM SecurityGroup r "
                + "LEFT JOIN FETCH r.securityRules "
                + "WHERE r.name= :name "
                + "AND r.owner= :owner "
                + "AND r.status <> 'DEFAULT_DELETED' "), @NamedQuery(
        name = "SecurityGroup.findByNameInAccount",
        query = "SELECT r FROM SecurityGroup r "
                + "LEFT JOIN FETCH r.securityRules "
                + "WHERE r.name= :name "
                + "AND r.account= :account "
                + "AND r.status <> 'DEFAULT_DELETED' "), @NamedQuery(
        name = "SecurityGroup.findByName",
        query = "SELECT r FROM SecurityGroup r "
                + "LEFT JOIN FETCH r.securityRules "
                + "WHERE r.name= :name "), @NamedQuery(
        name = "SecurityGroup.findForUser",
        query = "SELECT r FROM SecurityGroup r "
                + "LEFT JOIN FETCH r.securityRules "
                + "WHERE r.owner= :owner "
                + "AND r.status <> 'DEFAULT_DELETED' "), @NamedQuery(
        name = "SecurityGroup.findPublicInAccountForUser",
        query = "SELECT r FROM SecurityGroup r "
                + "LEFT JOIN FETCH r.securityRules "
                + "WHERE ((r.account= :account AND r.publicInAccount= true) OR r.owner= :owner) "
                + "AND r.status <> 'DEFAULT_DELETED' "), @NamedQuery(
        name = "SecurityGroup.findAllInAccount",
        query = "SELECT r FROM SecurityGroup r "
                + "LEFT JOIN FETCH r.securityRules "
                + "WHERE r.account= :account "
                + "AND r.status <> 'DEFAULT_DELETED' "), @NamedQuery(
        name = "SecurityGroup.findAllDefaultInAccount",
        query = "SELECT r FROM SecurityGroup r "
                + "LEFT JOIN FETCH r.securityRules "
                + "WHERE r.account= :account "
                + "AND (r.status = 'DEFAULT_DELETED' OR r.status = 'DEFAULT') "))
class SecurityGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "securitygroup_generator")
    @SequenceGenerator(name = "securitygroup_generator", sequenceName = "securitygroup_id_seq", allocationSize = 1)
    var id: Long? = null

    @Column(nullable = false)
    var name: String? = null

    var owner: String? = null

    var account: String? = null

    var isPublicInAccount: Boolean = false

    @Column(length = 1000000, columnDefinition = "TEXT")
    var description: String? = null

    @Enumerated(EnumType.STRING)
    var status: ResourceStatus? = null

    @OneToMany(mappedBy = "securityGroup", cascade = arrayOf(CascadeType.REMOVE, CascadeType.PERSIST), orphanRemoval = true)
    var securityRules: Set<SecurityRule> = HashSet()
}

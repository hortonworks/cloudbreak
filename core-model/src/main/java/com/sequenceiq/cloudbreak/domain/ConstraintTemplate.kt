package com.sequenceiq.cloudbreak.domain

import com.sequenceiq.cloudbreak.common.type.ResourceStatus

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

@Entity
@Table(uniqueConstraints = arrayOf(@UniqueConstraint(columnNames = { "account", "name" })))
@NamedQueries(@NamedQuery(
        name = "ConstraintTemplate.findForUser",
        query = "SELECT t FROM ConstraintTemplate t "
                + "WHERE t.owner= :user AND deleted IS NOT TRUE "
                + "AND t.status <> 'DEFAULT_DELETED' "), @NamedQuery(
        name = "ConstraintTemplate.findPublicInAccountForUser",
        query = "SELECT t FROM ConstraintTemplate t "
                + "WHERE ((t.account= :account AND t.publicInAccount= true) "
                + "OR t.owner= :user) AND deleted IS NOT TRUE "
                + "AND t.status <> 'DEFAULT_DELETED' "), @NamedQuery(
        name = "ConstraintTemplate.findAllInAccount",
        query = "SELECT t FROM ConstraintTemplate t "
                + "WHERE t.account= :account AND deleted IS NOT TRUE "
                + "AND t.status <> 'DEFAULT_DELETED' "), @NamedQuery(
        name = "ConstraintTemplate.findOneByName",
        query = "SELECT t FROM ConstraintTemplate t "
                + "WHERE t.name= :name and t.account= :account AND deleted IS NOT TRUE "
                + "AND t.status <> 'DEFAULT_DELETED' "), @NamedQuery(
        name = "ConstraintTemplate.findByIdInAccount",
        query = "SELECT t FROM ConstraintTemplate t "
                + "WHERE t.id= :id and t.account= :account AND deleted IS NOT TRUE "
                + "AND t.status <> 'DEFAULT_DELETED' "), @NamedQuery(
        name = "ConstraintTemplate.findByNameInAccount",
        query = "SELECT t FROM ConstraintTemplate t "
                + "WHERE t.name= :name and ((t.account= :account and t.publicInAccount=true) or t.owner= :owner) "
                + "AND deleted IS NOT TRUE "
                + "AND t.status <> 'DEFAULT_DELETED' "), @NamedQuery(
        name = "ConstraintTemplate.findByNameInUser",
        query = "SELECT t FROM ConstraintTemplate t "
                + "WHERE t.owner= :owner and t.name= :name AND deleted IS NOT TRUE "
                + "AND t.status <> 'DEFAULT_DELETED' "), @NamedQuery(
        name = "ConstraintTemplate.findAllDefaultInAccount",
        query = "SELECT t FROM ConstraintTemplate t "
                + "WHERE t.account= :account "
                + "AND (t.status = 'DEFAULT_DELETED' OR t.status = 'DEFAULT') "))
class ConstraintTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "constraint_template_generator")
    @SequenceGenerator(name = "constraint_template_generator", sequenceName = "constrainttemplate_id_seq", allocationSize = 1)
    var id: Long? = null

    @Column(nullable = false)
    var name: String? = null
    @Column(length = 1000, columnDefinition = "TEXT")
    var description: String? = null

    var owner: String? = null
    var account: String? = null

    var isPublicInAccount: Boolean = false

    var cpu: Double? = null
    var memory: Double? = null
    var disk: Double? = null

    @Column(nullable = false)
    var orchestratorType: String? = null

    var isDeleted: Boolean = false

    @Enumerated(EnumType.STRING)
    var status: ResourceStatus? = null
}

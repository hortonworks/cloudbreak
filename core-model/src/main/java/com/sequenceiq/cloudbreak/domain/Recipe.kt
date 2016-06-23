package com.sequenceiq.cloudbreak.domain

import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.MapKeyColumn
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery
import javax.persistence.SequenceGenerator
import javax.persistence.Table
import javax.persistence.UniqueConstraint

import com.sequenceiq.cloudbreak.api.model.ExecutionType

@Entity
@Table(name = "recipe", uniqueConstraints = arrayOf(@UniqueConstraint(columnNames = { "account", "name" })))
@NamedQueries(@NamedQuery(
        name = "Recipe.findForUser",
        query = "SELECT r FROM Recipe r "
                + "WHERE r.owner= :owner"), @NamedQuery(
        name = "Recipe.findPublicInAccountForUser",
        query = "SELECT r FROM Recipe r "
                + "WHERE (r.account= :account AND r.publicInAccount= true) "
                + "OR r.owner= :owner"), @NamedQuery(
        name = "Recipe.findAllInAccount",
        query = "SELECT r FROM Recipe r "
                + "WHERE r.account= :account "), @NamedQuery(
        name = "Recipe.findByNameForUser",
        query = "SELECT r FROM Recipe r "
                + "WHERE r.name= :name and r.owner= :owner "), @NamedQuery(
        name = "Recipe.findByNameInAccount",
        query = "SELECT r FROM Recipe r WHERE r.name= :name and r.account= :account"))
class Recipe : ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "recipe_generator")
    @SequenceGenerator(name = "recipe_generator", sequenceName = "recipe_id_seq", allocationSize = 1)
    var id: Long? = null

    var name: String? = null

    var description: String? = null

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "plugin")
    @Column(name = "execution_type")
    @Enumerated(EnumType.STRING)
    var plugins: Map<String, ExecutionType>? = null
    var timeout: Int? = null

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "key")
    @Column(name = "value", columnDefinition = "TEXT", length = 100000)
    var keyValues: Map<String, String>? = null

    var account: String? = null

    var owner: String? = null

    var isPublicInAccount: Boolean = false
}

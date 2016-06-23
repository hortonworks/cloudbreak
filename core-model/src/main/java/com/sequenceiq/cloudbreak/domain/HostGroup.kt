package com.sequenceiq.cloudbreak.domain

import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToMany
import javax.persistence.ManyToOne
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.SequenceGenerator

import java.util.HashSet

@Entity
@NamedQueries(@NamedQuery(
        name = "HostGroup.findHostGroupsInCluster",
        query = "SELECT h FROM HostGroup h "
                + "LEFT JOIN FETCH h.hostMetadata "
                + "LEFT JOIN FETCH h.recipes "
                + "WHERE h.cluster.id= :clusterId"), @NamedQuery(
        name = "HostGroup.findHostGroupInClusterByName",
        query = "SELECT h FROM HostGroup h "
                + "LEFT JOIN FETCH h.hostMetadata "
                + "LEFT JOIN FETCH h.recipes "
                + "WHERE h.cluster.id= :clusterId "
                + "AND h.name= :hostGroupName"), @NamedQuery(
        name = "HostGroup.findAllHostGroupsByRecipe",
        query = "SELECT h FROM HostGroup h "
                + "JOIN h.recipes r "
                + "WHERE r.id= :recipeId"), @NamedQuery(
        name = "HostGroup.findHostGroupsByInstanceGroupName",
        query = "SELECT h FROM HostGroup h "
                + "WHERE h.cluster.id= :clusterId "
                + "AND h.constraint.instanceGroup.groupName= :instanceGroupName"))
class HostGroup {


    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "hostgroup_generator")
    @SequenceGenerator(name = "hostgroup_generator", sequenceName = "hostgroup_id_seq", allocationSize = 1)
    var id: Long? = null

    var name: String? = null

    @ManyToOne
    var cluster: Cluster? = null

    @OneToOne
    var constraint: Constraint? = null

    @OneToMany(mappedBy = "hostGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    var hostMetadata: Set<HostMetadata> = HashSet()

    @ManyToMany
    var recipes: MutableSet<Recipe> = HashSet()
        get() = recipes

    fun addRecipe(recipe: Recipe) {
        this.recipes.add(recipe)
    }

    val hostNames: Set<String>
        get() {
            val hostNames = HashSet<String>(hostMetadata.size)
            for (metadata in hostMetadata) {
                hostNames.add(metadata.hostName)
            }
            return hostNames
        }
}

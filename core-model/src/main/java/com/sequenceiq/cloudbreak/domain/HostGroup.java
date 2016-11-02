package com.sequenceiq.cloudbreak.domain;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

@Entity
@NamedQueries({
        @NamedQuery(
                name = "HostGroup.findHostGroupsInCluster",
                query = "SELECT h FROM HostGroup h "
                        + "LEFT JOIN FETCH h.hostMetadata "
                        + "LEFT JOIN FETCH h.recipes "
                        + "WHERE h.cluster.id= :clusterId"),
        @NamedQuery(
                name = "HostGroup.findHostGroupInClusterByName",
                query = "SELECT h FROM HostGroup h "
                        + "LEFT JOIN FETCH h.hostMetadata "
                        + "LEFT JOIN FETCH h.recipes "
                        + "WHERE h.cluster.id= :clusterId "
                        + "AND h.name= :hostGroupName"),
        @NamedQuery(
                name = "HostGroup.findAllHostGroupsByRecipe",
                query = "SELECT h FROM HostGroup h "
                        + "JOIN h.recipes r "
                        + "WHERE r.id= :recipeId"),
        @NamedQuery(
                name = "HostGroup.findHostGroupsByInstanceGroupName",
                query = "SELECT h FROM HostGroup h "
                        + "WHERE h.cluster.id= :clusterId "
                        + "AND h.constraint.instanceGroup.groupName= :instanceGroupName")
})
public class HostGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "hostgroup_generator")
    @SequenceGenerator(name = "hostgroup_generator", sequenceName = "hostgroup_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    private Cluster cluster;

    @OneToOne
    private Constraint constraint;

    @OneToMany(mappedBy = "hostGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<HostMetadata> hostMetadata = new HashSet<>();

    @ManyToMany
    private Set<Recipe> recipes = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public Constraint getConstraint() {
        return constraint;
    }

    public void setConstraint(Constraint constraint) {
        this.constraint = constraint;
    }

    public Set<HostMetadata> getHostMetadata() {
        return hostMetadata;
    }

    public void setHostMetadata(Set<HostMetadata> hostMetadata) {
        this.hostMetadata = hostMetadata;
    }

    public Set<Recipe> getRecipes() {
        return recipes;
    }

    public void setRecipes(Set<Recipe> recipes) {
        this.recipes = recipes;
    }

    public void addRecipe(Recipe recipe) {
        this.recipes.add(recipe);
    }

    public Set<String> getHostNames() {
        Set<String> hostNames = new HashSet<>(hostMetadata.size());
        for (HostMetadata metadata : hostMetadata) {
            hostNames.add(metadata.getHostName());
        }
        return hostNames;
    }
}

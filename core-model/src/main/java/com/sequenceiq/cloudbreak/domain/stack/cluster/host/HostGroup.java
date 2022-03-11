package com.sequenceiq.cloudbreak.domain.stack.cluster.host;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedEntityGraphs;
import javax.persistence.NamedSubgraph;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.converter.RecoveryModeConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;

@NamedEntityGraphs({
        @NamedEntityGraph(name = "HostGroup.instanceGroup.instanceMetaData",
                attributeNodes = @NamedAttributeNode(value = "instanceGroup", subgraph = "instanceMetaData"),
                subgraphs = {
                        @NamedSubgraph(name = "instanceMetaData", attributeNodes = @NamedAttributeNode("instanceMetaData"))
                }
        ),
})
@Entity
public class HostGroup implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "hostgroup_generator")
    @SequenceGenerator(name = "hostgroup_generator", sequenceName = "hostgroup_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    private Cluster cluster;

    @OneToOne
    private InstanceGroup instanceGroup;

    @OneToMany(mappedBy = "hostGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<GeneratedRecipe> generatedRecipes = new HashSet<>();

    @ManyToMany
    private Set<Recipe> recipes = new HashSet<>();

    @Column(nullable = false)
    @Convert(converter = RecoveryModeConverter.class)
    private RecoveryMode recoveryMode;

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

    public InstanceGroup getInstanceGroup() {
        return instanceGroup;
    }

    public void setInstanceGroup(InstanceGroup instanceGroup) {
        this.instanceGroup = instanceGroup;
    }

    public Set<Recipe> getRecipes() {
        return recipes;
    }

    public void setRecipes(Set<Recipe> recipes) {
        this.recipes = recipes;
    }

    public void addRecipe(Recipe recipe) {
        recipes.add(recipe);
    }

    public RecoveryMode getRecoveryMode() {
        return recoveryMode;
    }

    public void setRecoveryMode(RecoveryMode recoveryMode) {
        this.recoveryMode = recoveryMode;
    }

    public Set<GeneratedRecipe> getGeneratedRecipes() {
        return generatedRecipes;
    }

    public void setGeneratedRecipes(Set<GeneratedRecipe> generatedRecipes) {
        this.generatedRecipes = generatedRecipes;
    }

    @Override
    public String toString() {
        return "HostGroup{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", recoveryMode=" + recoveryMode +
                '}';
    }
}

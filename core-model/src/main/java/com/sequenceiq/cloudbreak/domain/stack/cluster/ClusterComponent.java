package com.sequenceiq.cloudbreak.domain.stack.cluster;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedSubgraph;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;

@NamedEntityGraph(name = "ClusterComponent.cluster.rdsConfig",
        attributeNodes = @NamedAttributeNode(value = "cluster", subgraph = "rdsConfig"),
        subgraphs = @NamedSubgraph(name = "rdsConfig", attributeNodes = @NamedAttributeNode("rdsConfigs")))
@Entity
public class ClusterComponent implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "component_generator")
    @SequenceGenerator(name = "component_generator", sequenceName = "component_id_seq", allocationSize = 20)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ComponentType componentType;

    @Column(nullable = false)
    private String name;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json attributes;

    @ManyToOne
    private Cluster cluster;

    public ClusterComponent() {
    }

    public ClusterComponent(ComponentType componentType, Json attributes, Cluster cluster) {
        this(componentType, componentType.name(), attributes, cluster);
    }

    public ClusterComponent(ComponentType componentType, String name, Json attributes, Cluster cluster) {
        this.componentType = componentType;
        this.name = name;
        this.attributes = attributes;
        this.cluster = cluster;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ComponentType getComponentType() {
        return componentType;
    }

    public void setComponentType(ComponentType componentType) {
        this.componentType = componentType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Json getAttributes() {
        return attributes;
    }

    public void setAttributes(Json attributes) {
        this.attributes = attributes;
    }

    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    @Override
    public String toString() {
        return "Component{"
                + "id=" + id
                + ", componentType=" + componentType
                + ", name='" + name + '\''
                + '}';
    }
}

package com.sequenceiq.cloudbreak.domain.stack.cluster;

import static org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Transient;

import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.converter.ComponentTypeConverter;

@NamedEntityGraph(name = "ClusterComponent.cluster.rdsConfig",
        attributeNodes = @NamedAttributeNode(value = "cluster", subgraph = "rdsConfig"),
        subgraphs = @NamedSubgraph(name = "rdsConfig", attributeNodes = @NamedAttributeNode("rdsConfigs")))
@Entity
@Audited
@AuditTable("clustercomponent_history")
public class ClusterComponent implements ProvisionEntity {

    @Transient
    public static final String CB_VERSION_KEY = "CB_VERSION";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "component_generator")
    @SequenceGenerator(name = "component_generator", sequenceName = "component_id_seq", allocationSize = 20)
    private Long id;

    @Convert(converter = ComponentTypeConverter.class)
    private ComponentType componentType;

    @Column(nullable = false)
    private String name;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json attributes;

    @ManyToOne
    @Audited(targetAuditMode = NOT_AUDITED)
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

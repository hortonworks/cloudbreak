package com.sequenceiq.cloudbreak.domain.stack.cluster;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.converter.ComponentTypeConverter;

@Entity
public class ArchivedClusterComponent implements ProvisionEntity {

    @Transient
    public static final String CB_VERSION_KEY = "CB_VERSION";

    @Id
    private Long id;

    @Convert(converter = ComponentTypeConverter.class)
    private ComponentType componentType;

    private String name;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json attributes;

    private int cluster_id;

    public ArchivedClusterComponent() {
    }

    public ArchivedClusterComponent(ComponentType componentType, Json attributes, int cluster_id) {
        this(componentType, componentType.name(), attributes, cluster_id);
    }

    public ArchivedClusterComponent(ComponentType componentType, String name, Json attributes, int cluster_id) {
        this.componentType = componentType;
        this.name = name;
        this.attributes = attributes;
        this.cluster_id = cluster_id;
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

    public int getCluster() {
        return cluster_id;
    }

    public void setCluster(int cluster_id) {
        this.cluster_id = cluster_id;
    }

    @Override public String toString() {
        return "ArchivedClusterComponent{" +
                "componentType=" + componentType +
                ", name='" + name + '\'' +
                ", attributes=" + attributes +
                ", cluster_id=" + cluster_id +
                '}';
    }

}

package com.sequenceiq.cloudbreak.domain.view;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.domain.converter.ComponentTypeConverter;

@Entity
@Table(name = "ClusterComponent")
public class ClusterComponentView implements ProvisionEntity {
    @Id
    private Long id;

    @Convert(converter = ComponentTypeConverter.class)
    private ComponentType componentType;

    private String name;

    @Column(name = "cluster_id")
    private Long clusterId;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json attributes;

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

    public Long getClusterId() {
        return clusterId;
    }

    public void setClusterId(Long clusterId) {
        this.clusterId = clusterId;
    }

    @Override
    public String toString() {
        return "ClusterComponentView{" +
                "id=" + id +
                ", componentType=" + componentType +
                ", name='" + name + '\'' +
                ", clusterId=" + clusterId +
                '}';
    }
}

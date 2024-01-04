package com.sequenceiq.cloudbreak.domain.stack.cluster;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.converter.ComponentTypeConverter;

@Entity
@Table(name = "clustercomponent_history")
public class ClusterComponentHistory implements ProvisionEntity {

    @Id
    private Long id;

    @Column(name = "componenttype")
    @Convert(converter = ComponentTypeConverter.class)
    private ComponentType componentType;

    @Column(nullable = false)
    private String name;

    @Column(name = "cluster_id")
    private Long clusterId;

    public ClusterComponentHistory() {
    }

    public ClusterComponentHistory(ComponentType componentType, Long clusterId) {
        this(componentType, componentType.name(), clusterId);
    }

    public ClusterComponentHistory(ComponentType componentType, String name, Long clusterId) {
        this.componentType = componentType;
        this.name = name;
        this.clusterId = clusterId;
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

    public Long getClusterId() {
        return clusterId;
    }

    public void setClusterId(Long clusterId) {
        this.clusterId = clusterId;
    }

    @Override
    public String toString() {
        return "Component{"
                + "id=" + id
                + ", componentType=" + componentType
                + ", name='" + name + '\''
                + ", clusterId='" + clusterId + '\''
                + '}';
    }

}

package com.sequenceiq.cloudbreak.domain.view;

import java.util.Objects;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.domain.converter.StackTypeConverter;

@Entity
@Table(name = "Stack")
public class BlueprintClusterView implements BaseBlueprintClusterView {

    @Id
    private Long id;

    private String name;

    @Convert(converter = StackTypeConverter.class)
    private StackType type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public StackType getType() {
        return type;
    }

    public void setType(StackType type) {
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BlueprintClusterView that = (BlueprintClusterView) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, type);
    }

    @Override
    public String toString() {
        return "BlueprintClusterView{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type=" + type +
                '}';
    }
}

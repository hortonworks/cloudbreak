package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.json.JsonToString;

@Entity
@NamedQueries({
        @NamedQuery(
                name = "Component.findComponentByStackIdComponentTypeName",
                query = "SELECT cv FROM Component cv "
                        + "WHERE cv.stack.id = :stackId AND cv.componentType = :componentType AND cv.name = :name"),
        @NamedQuery(
                name = "Component.findComponentByStackId",
                query = "SELECT cv FROM Component cv WHERE cv.stack.id = :stackId"
        )
})
public class Component {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "component_generator")
    @SequenceGenerator(name = "component_generator", sequenceName = "component_id_seq", allocationSize = 1)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ComponentType componentType;

    private String name;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json attributes;

    @ManyToOne
    private Stack stack;

    public Component() {

    }

    public Component(ComponentType componentType, String name, Json attributes, Stack stack) {
        this.componentType = componentType;
        this.name = name;
        this.attributes = attributes;
        this.stack = stack;
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

    public Stack getStack() {
        return stack;
    }

    public void setStack(Stack stack) {
        this.stack = stack;
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

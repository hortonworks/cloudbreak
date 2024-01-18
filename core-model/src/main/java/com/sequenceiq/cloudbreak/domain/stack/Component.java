package com.sequenceiq.cloudbreak.domain.stack;

import static com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil.anonymize;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.converter.ComponentTypeConverter;

@Entity
@Audited
@AuditTable("component_history")
public class Component implements ProvisionEntity {

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

    @Column(name = "stack_id")
    private Long stackId;

    public Component() {

    }

    public Component(ComponentType componentType, String name, Json attributes, Stack stack) {
        this.componentType = componentType;
        this.name = name;
        this.attributes = attributes;
        if (stack != null) {
            this.stackId = stack.getId();
        }
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

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

    @Override
    public String toString() {
        return "Component{"
                + "id=" + id
                + ", componentType=" + componentType
                + ", name='" + name + '\''
                + ", attributes=" + anonymize(attributes.toString())
                + ", stackId=" + stackId
                + '}';
    }
}

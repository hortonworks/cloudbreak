package com.sequenceiq.cloudbreak.domain.stack;

import static com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil.anonymize;
import static org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;

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

    @ManyToOne
    @Audited(targetAuditMode = NOT_AUDITED)
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
                + ", attributes=" + anonymize(attributes)
                + '}';
    }
}

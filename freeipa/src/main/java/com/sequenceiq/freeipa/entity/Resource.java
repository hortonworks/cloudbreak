package com.sequenceiq.freeipa.entity;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.common.type.CommonStatus;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.freeipa.entity.json.Json;
import com.sequenceiq.freeipa.entity.json.JsonToString;

@Entity
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "resource_generator")
    @SequenceGenerator(name = "resource_generator", sequenceName = "resource_id_seq", allocationSize = 1)
    private Long id;

    private String instanceGroup;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ResourceType resourceType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CommonStatus resourceStatus;

    private String resourceName;

    private String resourceReference;

    @ManyToOne
    @JoinColumn(name = "resource_stack")
    private Stack stack;

    private String instanceId;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json attributes;

    public Resource() {

    }

    public Resource(ResourceType resourceType, String resourceName, Stack stack) {
        this(resourceType, resourceName, null, CommonStatus.CREATED, stack, null);
    }

    public Resource(ResourceType resourceType, String resourceName, Stack stack, String instanceGroup) {
        this(resourceType, resourceName, null, CommonStatus.CREATED, stack, instanceGroup);
    }

    public Resource(ResourceType resourceType, String resourceName, String resourceReference, CommonStatus status, Stack stack, String instanceGroup) {
        this.resourceType = resourceType;
        this.resourceName = resourceName;
        this.resourceReference = resourceReference;
        resourceStatus = status;
        this.instanceGroup = instanceGroup;
        this.stack = stack;
    }

    public String getInstanceGroup() {
        return instanceGroup;
    }

    public void setInstanceGroup(String instanceGroup) {
        this.instanceGroup = instanceGroup;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public CommonStatus getResourceStatus() {
        return resourceStatus;
    }

    public void setResourceStatus(CommonStatus resourceStatus) {
        this.resourceStatus = resourceStatus;
    }

    public String getResourceReference() {
        return resourceReference;
    }

    public void setResourceReference(String resourceReference) {
        this.resourceReference = resourceReference;
    }

    public Stack getStack() {
        return stack;
    }

    public void setStack(Stack stack) {
        this.stack = stack;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public Json getAttributes() {
        return attributes;
    }

    public void setAttributes(Json attributes) {
        this.attributes = attributes;
    }
}

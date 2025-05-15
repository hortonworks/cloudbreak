package com.sequenceiq.cloudbreak.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.converter.CommonStatusConverter;
import com.sequenceiq.cloudbreak.converter.ResourceTypeConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@Entity
public class Resource implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "resource_generator")
    @SequenceGenerator(name = "resource_generator", sequenceName = "resource_id_seq", allocationSize = 1)
    private Long id;

    private String instanceGroup;

    @Column(nullable = false)
    @Convert(converter = ResourceTypeConverter.class)
    private ResourceType resourceType;

    @Column(nullable = false)
    @Convert(converter = CommonStatusConverter.class)
    private CommonStatus resourceStatus;

    private String resourceName;

    private String resourceReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_stack")
    private Stack stack;

    private String instanceId;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json attributes;

    private String availabilityZone;

    private Long privateId;

    public Resource() {

    }

    public Resource(ResourceType resourceType, String resourceName, Stack stack, String availabilityZone) {
        this(resourceType, resourceName, null, CommonStatus.CREATED, stack, null, availabilityZone, null);
    }

    public Resource(ResourceType resourceType, String resourceName, Stack stack, String instanceGroup, String availabilityZone) {
        this(resourceType, resourceName, null, CommonStatus.CREATED, stack, instanceGroup, availabilityZone, null);
    }

    // CHECKSTYLE:OFF
    public Resource(
            ResourceType resourceType,
            String resourceName,
            String resourceReference,
            CommonStatus status,
            Stack stack,
            String instanceGroup,
            String availabilityZone,
            Long privateId) {
        this.resourceType = resourceType;
        this.resourceName = resourceName;
        this.resourceReference = resourceReference;
        resourceStatus = status;
        this.instanceGroup = instanceGroup;
        this.stack = stack;
        this.availabilityZone = availabilityZone;
        this.privateId = privateId;
    }
    // CHECKSTYLE:ON

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

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    public Long getPrivateId() {
        return privateId;
    }

    public void setPrivateId(Long privateId) {
        this.privateId = privateId;
    }

    @Override
    public String toString() {
        return "Resource{" +
                "id=" + id +
                ", instanceGroup='" + instanceGroup + '\'' +
                ", resourceType=" + resourceType +
                ", resourceStatus=" + resourceStatus +
                ", resourceName='" + resourceName + '\'' +
                ", resourceReference='" + resourceReference + '\'' +
                ", instanceId='" + instanceId + '\'' +
                ", attributes=" + attributes +
                ", availabilityZone='" + availabilityZone + '\'' +
                ", privateId=" + privateId +
                '}';
    }
}

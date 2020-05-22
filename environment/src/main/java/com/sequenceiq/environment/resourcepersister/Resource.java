package com.sequenceiq.environment.resourcepersister;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.converter.CommonStatusConverter;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.environment.parameters.dao.converter.ResourceTypeConverter;

@Entity
public class Resource implements Serializable {

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

    private String crn;

    private String instanceId;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json attributes;

    public Resource() {

    }

    public Resource(ResourceType resourceType, String resourceName, String crn) {
        this(resourceType, resourceName, null, CommonStatus.CREATED, crn, null);
    }

    public Resource(ResourceType resourceType, String resourceName, String crn, String instanceGroup) {
        this(resourceType, resourceName, null, CommonStatus.CREATED, crn, instanceGroup);
    }

    public Resource(ResourceType resourceType, String resourceName, String resourceReference, CommonStatus status, String crn, String instanceGroup) {
        this.resourceType = resourceType;
        this.resourceName = resourceName;
        this.resourceReference = resourceReference;
        resourceStatus = status;
        this.instanceGroup = instanceGroup;
        this.crn = crn;
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

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }
}

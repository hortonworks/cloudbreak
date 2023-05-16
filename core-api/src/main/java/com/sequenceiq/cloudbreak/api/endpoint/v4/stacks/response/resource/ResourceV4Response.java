package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceGroupModelDescription;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class ResourceV4Response {

    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    @ApiModelProperty(InstanceGroupModelDescription.INSTANCE_GROUP_NAME)
    private String instanceGroup;

    @ApiModelProperty("Resource Type")
    private ResourceType resourceType;

    @ApiModelProperty("Common Status")
    private CommonStatus resourceStatus;

    @ApiModelProperty("Resource Name")
    private String resourceName;

    @ApiModelProperty("Resource Reference")
    private String resourceReference;

    @ApiModelProperty("Stack ID of the resource")
    private Long resourceStack;

    @ApiModelProperty("Instance ID of the resource")
    private String instanceId;

    @ApiModelProperty("JSON string of all attached volumes")
    private String attributes;

    @ApiModelProperty("Availability zone of the resource")
    private String availabilityZone;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInstanceGroup() {
        return instanceGroup;
    }

    public void setInstanceGroup(String instanceGroup) {
        this.instanceGroup = instanceGroup;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public CommonStatus getResourceStatus() {
        return resourceStatus;
    }

    public void setResourceStatus(CommonStatus resourceStatus) {
        this.resourceStatus = resourceStatus;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getResourceReference() {
        return resourceReference;
    }

    public void setResourceReference(String resourceReference) {
        this.resourceReference = resourceReference;
    }

    public Long getResourceStack() {
        return resourceStack;
    }

    public void setResourceStack(Long resourceStack) {
        this.resourceStack = resourceStack;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getAttributes() {
        return attributes;
    }

    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }
}

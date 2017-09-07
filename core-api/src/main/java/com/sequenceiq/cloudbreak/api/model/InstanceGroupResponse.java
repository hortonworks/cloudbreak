package com.sequenceiq.cloudbreak.api.model;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceGroupModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class InstanceGroupResponse extends InstanceGroupBase {

    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    @ApiModelProperty(InstanceGroupModelDescription.METADATA)
    private Set<InstanceMetaDataJson> metadata = new HashSet<>();

    @ApiModelProperty(InstanceGroupModelDescription.TEMPLATE)
    private TemplateResponse template;

    @ApiModelProperty(InstanceGroupModelDescription.SECURITYGROUP)
    private SecurityGroupResponse securityGroup;

    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    @JsonIgnore
    public void setId(Long id) {
        this.id = id;
    }

    @JsonProperty("metadata")
    public Set<InstanceMetaDataJson> getMetadata() {
        return metadata;
    }

    @JsonIgnore
    public void setMetadata(Set<InstanceMetaDataJson> metadata) {
        this.metadata = metadata;
    }

    public TemplateResponse getTemplate() {
        return template;
    }

    public void setTemplate(TemplateResponse template) {
        this.template = template;
    }

    public SecurityGroupResponse getSecurityGroup() {
        return securityGroup;
    }

    public void setSecurityGroup(SecurityGroupResponse securityGroup) {
        this.securityGroup = securityGroup;
    }
}

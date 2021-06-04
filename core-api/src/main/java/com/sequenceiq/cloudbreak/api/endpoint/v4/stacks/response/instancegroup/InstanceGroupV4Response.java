package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.network.InstanceGroupNetworkV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.securitygroup.SecurityGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.InstanceTemplateV4Response;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceGroupModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class InstanceGroupV4Response extends InstanceGroupV4Base {

    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    @ApiModelProperty(InstanceGroupModelDescription.METADATA)
    private Set<InstanceMetaDataV4Response> metadata = new HashSet<>();

    @ApiModelProperty(InstanceGroupModelDescription.TEMPLATE)
    private InstanceTemplateV4Response template;

    @ApiModelProperty(InstanceGroupModelDescription.SECURITYGROUP)
    private SecurityGroupV4Response securityGroup;

    private List<RecipeV4Response> recipes;

    private Set<String> availabilityZones;

    private InstanceGroupNetworkV4Response network;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Set<InstanceMetaDataV4Response> getMetadata() {
        return metadata;
    }

    public void setMetadata(Set<InstanceMetaDataV4Response> metadata) {
        this.metadata = metadata;
    }

    public InstanceTemplateV4Response getTemplate() {
        return template;
    }

    public void setTemplate(InstanceTemplateV4Response template) {
        this.template = template;
    }

    public SecurityGroupV4Response getSecurityGroup() {
        return securityGroup;
    }

    public void setSecurityGroup(SecurityGroupV4Response securityGroup) {
        this.securityGroup = securityGroup;
    }

    public List<RecipeV4Response> getRecipes() {
        return recipes;
    }

    public void setRecipes(List<RecipeV4Response> recipes) {
        this.recipes = recipes;
    }

    public InstanceGroupNetworkV4Response getNetwork() {
        return network;
    }

    public void setNetwork(InstanceGroupNetworkV4Response network) {
        this.network = network;
    }

    public Set<String>  getAvailabilityZones() {
        return availabilityZones;
    }

    public void setAvailabilityZones(Set<String>  availabilityZones) {
        this.availabilityZones = availabilityZones;
    }
}

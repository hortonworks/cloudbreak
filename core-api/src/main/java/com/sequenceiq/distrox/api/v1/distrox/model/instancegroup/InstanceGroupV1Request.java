package com.sequenceiq.distrox.api.v1.distrox.model.instancegroup;

import java.util.HashSet;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostGroupModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceGroupModelDescription;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.InstanceTemplateV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.network.InstanceGroupNetworkV1Request;
import com.sequenceiq.distrox.api.v1.distrox.validation.ValidRootVolumeSize;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@ValidRootVolumeSize
public class InstanceGroupV1Request extends InstanceGroupV1Base {

    @Valid
    @NotNull
    @ApiModelProperty(InstanceGroupModelDescription.TEMPLATE)
    private InstanceTemplateV1Request template;

    @ApiModelProperty(HostGroupModelDescription.RECIPE_NAMES)
    private Set<String> recipeNames = new HashSet<>();

    @ApiModelProperty(HostGroupModelDescription.NETWORK)
    private InstanceGroupNetworkV1Request network;

    public InstanceTemplateV1Request getTemplate() {
        return template;
    }

    public void setTemplate(InstanceTemplateV1Request template) {
        this.template = template;
    }

    public Set<String> getRecipeNames() {
        return recipeNames;
    }

    public void setRecipeNames(Set<String> recipeNames) {
        this.recipeNames = recipeNames;
    }

    public InstanceGroupNetworkV1Request getNetwork() {
        return network;
    }

    public void setNetwork(InstanceGroupNetworkV1Request network) {
        this.network = network;
    }
}

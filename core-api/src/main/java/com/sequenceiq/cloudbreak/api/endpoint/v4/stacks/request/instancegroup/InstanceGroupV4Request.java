package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup;

import java.util.HashSet;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.network.InstanceGroupNetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.securitygroup.SecurityGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostGroupModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceGroupModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class InstanceGroupV4Request extends InstanceGroupV4Base {

    @Valid
    @NotNull
    @Schema(description = InstanceGroupModelDescription.TEMPLATE)
    private InstanceTemplateV4Request template;

    @Valid
    @Schema(description = InstanceGroupModelDescription.SECURITYGROUP)
    private SecurityGroupV4Request securityGroup;

    @Schema(description = HostGroupModelDescription.RECIPE_NAMES)
    private Set<String> recipeNames = new HashSet<>();

    @Schema(description = HostGroupModelDescription.RECOVERY_MODE)
    private RecoveryMode recoveryMode = RecoveryMode.MANUAL;

    @Schema(description = InstanceGroupModelDescription.NETWORK)
    private InstanceGroupNetworkV4Request network;

    public InstanceTemplateV4Request getTemplate() {
        return template;
    }

    public void setTemplate(InstanceTemplateV4Request template) {
        this.template = template;
    }

    public SecurityGroupV4Request getSecurityGroup() {
        return securityGroup;
    }

    public void setSecurityGroup(SecurityGroupV4Request securityGroup) {
        this.securityGroup = securityGroup;
    }

    public Set<String> getRecipeNames() {
        return recipeNames;
    }

    public void setRecipeNames(Set<String> recipeNames) {
        this.recipeNames = recipeNames;
    }

    public RecoveryMode getRecoveryMode() {
        return recoveryMode;
    }

    public void setRecoveryMode(RecoveryMode recoveryMode) {
        this.recoveryMode = recoveryMode;
    }

    public InstanceGroupNetworkV4Request getNetwork() {
        return network;
    }

    public void setNetwork(InstanceGroupNetworkV4Request network) {
        this.network = network;
    }
}

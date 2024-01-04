package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance;

import jakarta.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.InstanceGroupModelDescription;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.SecurityGroupRequest;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "InstanceGroupV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InstanceGroupRequest extends InstanceGroupBase {

    @Valid
    @Schema(description = InstanceGroupModelDescription.TEMPLATE)
    private InstanceTemplateRequest instanceTemplate;

    @Valid
    @Schema(description = InstanceGroupModelDescription.SECURITYGROUP)
    private SecurityGroupRequest securityGroup;

    @Schema(description = InstanceGroupModelDescription.NETWORK)
    private InstanceGroupNetworkRequest network;

    public InstanceTemplateRequest getInstanceTemplate() {
        return instanceTemplate;
    }

    public void setInstanceTemplateRequest(InstanceTemplateRequest instanceTemplate) {
        this.instanceTemplate = instanceTemplate;
    }

    public SecurityGroupRequest getSecurityGroup() {
        return securityGroup;
    }

    public void setSecurityGroup(SecurityGroupRequest securityGroup) {
        this.securityGroup = securityGroup;
    }

    public InstanceGroupNetworkRequest getNetwork() {
        return network;
    }

    public void setNetwork(InstanceGroupNetworkRequest network) {
        this.network = network;
    }

    @Override
    public String toString() {
        return super.toString() + "InstanceGroupRequest{"
                + "instanceGroupName=" + getName()
                + ", nodeCount=" + getNodeCount()
                + ", type=" + getType()
                + ", instanceTemplate=" + instanceTemplate
                + ", securityGroup=" + securityGroup
                + ", network=" + network
                + '}';
    }
}

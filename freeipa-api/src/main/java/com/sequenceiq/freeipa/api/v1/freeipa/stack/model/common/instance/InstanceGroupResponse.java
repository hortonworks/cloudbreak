package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance;

import java.util.HashSet;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.InstanceGroupModelDescription;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.SecurityGroupResponse;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("InstanceGroupV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InstanceGroupResponse extends InstanceGroupBase {
    @NotNull
    @ApiModelProperty(InstanceGroupModelDescription.TEMPLATE)
    private InstanceTemplateResponse instanceTemplate;

    @Valid
    @ApiModelProperty(InstanceGroupModelDescription.SECURITYGROUP)
    private SecurityGroupResponse securityGroup;

    @NotNull
    @ApiModelProperty(InstanceGroupModelDescription.NETWORK)
    private InstanceGroupNetworkResponse network;

    private Set<InstanceMetaDataResponse> metaData = new HashSet<>();

    public InstanceTemplateResponse getInstanceTemplate() {
        return instanceTemplate;
    }

    public void setInstanceTemplate(InstanceTemplateResponse instanceTemplate) {
        this.instanceTemplate = instanceTemplate;
    }

    public SecurityGroupResponse getSecurityGroup() {
        return securityGroup;
    }

    public void setSecurityGroup(SecurityGroupResponse securityGroup) {
        this.securityGroup = securityGroup;
    }

    public Set<InstanceMetaDataResponse> getMetaData() {
        return metaData;
    }

    public void setMetaData(Set<InstanceMetaDataResponse> metaData) {
        this.metaData = metaData;
    }

    public InstanceGroupNetworkResponse getNetwork() {
        return network;
    }

    public void setNetwork(InstanceGroupNetworkResponse network) {
        this.network = network;
    }

    @Override
    public String toString() {
        return "InstanceGroupResponse{" +
                "InstanceGroupBase=" + super.toString() +
                ", instanceTemplate=" + instanceTemplate +
                ", securityGroup=" + securityGroup +
                ", network=" + network +
                ", metaData=" + metaData +
                '}';
    }
}

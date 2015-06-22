package com.sequenceiq.cloudbreak.controller.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@ApiModel("SecurityRule")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecurityRuleJson implements JsonEntity {

    @ApiModelProperty(value = ModelDescriptions.ID, required = false)
    private Long id;
    @ApiModelProperty(value = ModelDescriptions.SecurityRuleModelDescription.SUBNET, required = true)
    private String subnet;
    @ApiModelProperty(value = ModelDescriptions.SecurityRuleModelDescription.PORTS, required = true)
    private String ports;
    @ApiModelProperty(value = ModelDescriptions.SecurityRuleModelDescription.PROTOCOL, required = true)
    private String protocol;
    @ApiModelProperty(value = ModelDescriptions.SecurityRuleModelDescription.MODIFIABLE, required = false)
    private boolean modifiable;

    public SecurityRuleJson() {
    }

    public SecurityRuleJson(String subnet) {
        this.subnet = subnet;
    }

    public String getSubnet() {
        return subnet;
    }

    public void setSubnet(String subnet) {
        this.subnet = subnet;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPorts() {
        return ports;
    }

    public void setPorts(String ports) {
        this.ports = ports;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public boolean isModifiable() {
        return modifiable;
    }

    public void setModifiable(boolean modifiable) {
        this.modifiable = modifiable;
    }
}

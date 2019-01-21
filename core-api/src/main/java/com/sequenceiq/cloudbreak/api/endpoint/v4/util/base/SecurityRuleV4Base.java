package com.sequenceiq.cloudbreak.api.endpoint.v4.util.base;

import java.util.List;

import javax.validation.constraints.Pattern;

import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.SecurityRuleModelDescription;

import io.swagger.annotations.ApiModelProperty;

public class SecurityRuleV4Base implements JsonEntity {

    @ApiModelProperty(value = ModelDescriptions.SecurityRuleModelDescription.SUBNET, required = true)
    @Pattern(regexp =
            "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])(\\/([0-9]|[1-2][0-9]|3[0-2]))$",
            message = "The subnet field should contain a valid CIDR definition.")
    private String subnet;

    @ApiModelProperty(value = ModelDescriptions.SecurityRuleModelDescription.PORTS, required = true)
    @Pattern(regexp = "^[1-9][0-9]{0,4}(-[1-9][0-9]{0,4}){0,1}(,[1-9][0-9]{0,4}(-[1-9][0-9]{0,4}){0,1})*$",
            message = "The ports field should contain a comma separated list of port numbers, for example: 8080,9090,5555")
    private List<String> ports;

    @ApiModelProperty(value = SecurityRuleModelDescription.PROTOCOL, required = true)
    private String protocol;

    @ApiModelProperty(SecurityRuleModelDescription.MODIFIABLE)
    private Boolean modifiable;

    public String getSubnet() {
        return subnet;
    }

    public void setSubnet(String subnet) {
        this.subnet = subnet;
    }

    public List<String> getPorts() {
        return ports;
    }

    public void setPorts(List<String> ports) {
        this.ports = ports;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Boolean isModifiable() {
        return modifiable;
    }

    public void setModifiable(Boolean modifiable) {
        this.modifiable = modifiable;
    }
}

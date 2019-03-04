package com.sequenceiq.cloudbreak.api.endpoint.v4.util.base;

import java.util.List;

import javax.validation.constraints.Pattern;

import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.SecurityRuleModelDescription;
import com.sequenceiq.cloudbreak.validation.SubnetType;
import com.sequenceiq.cloudbreak.validation.ValidSubnet;

import io.swagger.annotations.ApiModelProperty;

public class SecurityRuleV4Base implements JsonEntity {

    @ValidSubnet(SubnetType.CUSTOM)
    @ApiModelProperty(value = SecurityRuleModelDescription.SUBNET, required = true)
    private String subnet;

    @ApiModelProperty(value = SecurityRuleModelDescription.PORTS, required = true)
    private List<@Pattern(regexp = "^([1-9][0-9]{0,3}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])"
            + "(-([1-9][0-9]{0,3}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5]))?$",
            message = "The ports field should contain a list of port numbers (1..65535) & ranges (two ports separated with a hyphen, both ends inclusive), "
                    + "for example: 8080 or 9090-9092") String> ports;

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

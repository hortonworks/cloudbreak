package com.sequenceiq.redbeams.api.endpoint.v4.stacks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.redbeams.doc.ModelDescriptions.DatabaseServerModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class DatabaseServerV4Request extends DatabaseServerV4Base {

    @ApiModelProperty(DatabaseServerModelDescriptions.SECURITY_GROUP)
    private SecurityGroupV4Request securityGroup;

    public SecurityGroupV4Request getSecurityGroup() {
        return securityGroup;
    }

    public void setSecurityGroup(SecurityGroupV4Request securityGroup) {
        this.securityGroup = securityGroup;
    }

}

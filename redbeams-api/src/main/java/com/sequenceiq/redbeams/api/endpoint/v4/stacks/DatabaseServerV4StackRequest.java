package com.sequenceiq.redbeams.api.endpoint.v4.stacks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.redbeams.doc.ModelDescriptions.DatabaseServerModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class DatabaseServerV4StackRequest extends DatabaseServerV4StackBase {

    @Schema(description = DatabaseServerModelDescriptions.SECURITY_GROUP)
    private SecurityGroupV4StackRequest securityGroup;

    public SecurityGroupV4StackRequest getSecurityGroup() {
        return securityGroup;
    }

    public void setSecurityGroup(SecurityGroupV4StackRequest securityGroup) {
        this.securityGroup = securityGroup;
    }

    @Override
    public String toString() {
        return "DatabaseServerV4StackRequest{" +
                "securityGroup=" + securityGroup +
                '}';
    }
}

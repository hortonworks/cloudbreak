package com.sequenceiq.freeipa.api.v1.kerberosmgmt.model;

import java.util.Set;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.doc.KeytabModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("RoleV1Request")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleRequest {

    @NotNull
    @ApiModelProperty(value = KeytabModelDescription.ROLE_NAME)
    private String roleName;

    @ApiModelProperty(value = KeytabModelDescription.PRIVILEGES)
    private Set<String> privileges = Set.of();

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public Set<String> getPrivileges() {
        return privileges;
    }

    public void setPrivileges(Set<String> privileges) {
        this.privileges = privileges;
    }

    @Override
    public String toString() {
        return "RoleRequest{"
                + "roleName='" + roleName + '\''
                + ", privileges=" + privileges
                + '}';
    }
}

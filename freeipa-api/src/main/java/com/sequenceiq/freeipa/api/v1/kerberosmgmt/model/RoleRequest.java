package com.sequenceiq.freeipa.api.v1.kerberosmgmt.model;

import java.util.Set;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.doc.KeytabModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RoleV1Request")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleRequest {

    @NotNull
    @Schema(description = KeytabModelDescription.ROLE_NAME)
    private String roleName;

    @Schema(description = KeytabModelDescription.PRIVILEGES)
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

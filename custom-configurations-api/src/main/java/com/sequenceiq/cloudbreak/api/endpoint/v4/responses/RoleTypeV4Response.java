package com.sequenceiq.cloudbreak.api.endpoint.v4.responses;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class RoleTypeV4Response {

    @Schema
    private List<String> roleTypes;

    public RoleTypeV4Response() {
    }

    public RoleTypeV4Response(List<String> roleTypes) {
        this.roleTypes = roleTypes;
    }

    public List<String> getRoleTypes() {
        return roleTypes;
    }

    public void setRoleTypes(List<String> roleTypes) {
        this.roleTypes = roleTypes;
    }

    @Override
    public String toString() {
        return "RoleTypeV4Response{" +
                "roleTypes=" + roleTypes +
                '}';
    }
}

package com.sequenceiq.cloudbreak.api.endpoint.v4.responses;

import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class RoleTypeV4Response {

    @ApiModelProperty
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

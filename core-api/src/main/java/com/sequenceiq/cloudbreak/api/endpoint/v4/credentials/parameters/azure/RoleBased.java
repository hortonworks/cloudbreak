package com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.azure;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.Mappable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleBased implements Mappable {

    @NotNull
    @ApiModelProperty(required = true, allowableValues = "CONTRIBUTOR, REUSE_EXISTING, CREATE_CUSTOM")
    private RoleType roleType;

    @ApiModelProperty(required = true)
    private String roleName;

    public RoleType getType() {
        return roleType;
    }

    public void setType(RoleType type) {
        this.roleType = type;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("roleType", roleType.name());
        map.put("roleName", roleName != null ? roleName : "");
        return map;
    }

}

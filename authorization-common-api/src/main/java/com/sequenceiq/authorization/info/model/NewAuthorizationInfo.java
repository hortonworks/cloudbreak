package com.sequenceiq.authorization.info.model;

import java.util.Set;

import com.google.common.collect.Sets;

import io.swagger.v3.oas.annotations.media.Schema;

public class NewAuthorizationInfo {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<String> permissionsNeededForApi = Sets.newHashSet();

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<FieldAuthorizationInfo> permissionsNeededForRequestObject = Sets.newHashSet();

    public Set<String> getPermissionsNeededForApi() {
        return permissionsNeededForApi;
    }

    public void setPermissionsNeededForApi(Set<String> permissionsNeededForApi) {
        this.permissionsNeededForApi = permissionsNeededForApi;
    }

    public Set<FieldAuthorizationInfo> getPermissionsNeededForRequestObject() {
        return permissionsNeededForRequestObject;
    }

    public void setPermissionsNeededForRequestObject(Set<FieldAuthorizationInfo> permissionsNeededForRequestObject) {
        this.permissionsNeededForRequestObject = permissionsNeededForRequestObject;
    }
}

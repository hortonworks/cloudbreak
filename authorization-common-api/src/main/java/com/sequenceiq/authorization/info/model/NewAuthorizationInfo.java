package com.sequenceiq.authorization.info.model;

import java.util.Set;

import com.google.common.collect.Sets;

public class NewAuthorizationInfo {

    private Set<String> permissionsNeededForApi = Sets.newHashSet();

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

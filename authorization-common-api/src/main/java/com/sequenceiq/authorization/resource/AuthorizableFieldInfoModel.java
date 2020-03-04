package com.sequenceiq.authorization.resource;

public class AuthorizableFieldInfoModel {

    private AuthorizationResourceType resourceType;

    private AuthorizationResourceAction action;

    private AuthorizationVariableType fieldVariableType;

    public AuthorizableFieldInfoModel(AuthorizationResourceType resourceType, AuthorizationResourceAction action, AuthorizationVariableType fieldVariableType) {
        this.resourceType = resourceType;
        this.action = action;
        this.fieldVariableType = fieldVariableType;
    }

    public AuthorizationResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(AuthorizationResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public AuthorizationResourceAction getAction() {
        return action;
    }

    public void setAction(AuthorizationResourceAction action) {
        this.action = action;
    }

    public AuthorizationVariableType getFieldVariableType() {
        return fieldVariableType;
    }

    public void setFieldVariableType(AuthorizationVariableType fieldVariableType) {
        this.fieldVariableType = fieldVariableType;
    }
}

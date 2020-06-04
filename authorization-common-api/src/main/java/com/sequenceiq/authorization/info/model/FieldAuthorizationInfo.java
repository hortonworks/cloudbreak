package com.sequenceiq.authorization.info.model;

public class FieldAuthorizationInfo {

    private String fieldName;

    private String permission;

    public FieldAuthorizationInfo(String fieldName, String permission) {
        this.fieldName = fieldName;
        this.permission = permission;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }
}

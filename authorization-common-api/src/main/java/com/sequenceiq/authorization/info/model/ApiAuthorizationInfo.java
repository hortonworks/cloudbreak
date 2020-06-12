package com.sequenceiq.authorization.info.model;

public class ApiAuthorizationInfo {

    private String path;

    private String httpMethod;

    private NewAuthorizationInfo newAuthorization;

    private LegacyAuthorizationInfo legacyAuthorization;

    private String message;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public NewAuthorizationInfo getNewAuthorization() {
        return newAuthorization;
    }

    public void setNewAuthorization(NewAuthorizationInfo newAuthorization) {
        this.newAuthorization = newAuthorization;
    }

    public LegacyAuthorizationInfo getLegacyAuthorization() {
        return legacyAuthorization;
    }

    public void setLegacyAuthorization(LegacyAuthorizationInfo legacyAuthorization) {
        this.legacyAuthorization = legacyAuthorization;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}

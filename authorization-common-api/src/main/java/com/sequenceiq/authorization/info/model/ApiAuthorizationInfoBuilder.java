package com.sequenceiq.authorization.info.model;

public final class ApiAuthorizationInfoBuilder {

    private String path;

    private String httpMethod;

    private NewAuthorizationInfo newAuthorization;

    private LegacyAuthorizationInfo legacyAuthorization;

    private String message;

    private ApiAuthorizationInfoBuilder() {
    }

    public static ApiAuthorizationInfoBuilder anApiAuthorizationInfo() {
        return new ApiAuthorizationInfoBuilder();
    }

    public ApiAuthorizationInfoBuilder withPath(String path) {
        this.path = path;
        return this;
    }

    public ApiAuthorizationInfoBuilder withHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
        return this;
    }

    public ApiAuthorizationInfoBuilder withNewAuthorization(NewAuthorizationInfo newAuthorization) {
        this.newAuthorization = newAuthorization;
        return this;
    }

    public ApiAuthorizationInfoBuilder withLegacyAuthorization(LegacyAuthorizationInfo legacyAuthorization) {
        this.legacyAuthorization = legacyAuthorization;
        return this;
    }

    public ApiAuthorizationInfoBuilder withMessage(String message) {
        this.message = message;
        return this;
    }

    public ApiAuthorizationInfo build() {
        ApiAuthorizationInfo apiAuthorizationInfo = new ApiAuthorizationInfo();
        apiAuthorizationInfo.setPath(path);
        apiAuthorizationInfo.setHttpMethod(httpMethod);
        apiAuthorizationInfo.setNewAuthorization(newAuthorization);
        apiAuthorizationInfo.setLegacyAuthorization(legacyAuthorization);
        apiAuthorizationInfo.setMessage(message);
        return apiAuthorizationInfo;
    }
}

package com.sequenceiq.environment.credential.attributes.azure;

public class CodeGrantFlowAttributes extends AppBasedAttributes {

    private String appReplyUrl;

    private String appLoginUrl;

    private String deploymentAddress;

    private String spDisplayName;

    private String appObjectId;

    private String authorizationCode;

    private String codeGrantFlowState;

    private String refreshToken;

    public String getDeploymentAddress() {
        return deploymentAddress;
    }

    public void setDeploymentAddress(String deploymentAddress) {
        this.deploymentAddress = deploymentAddress;
    }

    public String getSpDisplayName() {
        return spDisplayName;
    }

    public void setSpDisplayName(String spDisplayName) {
        this.spDisplayName = spDisplayName;
    }

    public String getAppObjectId() {
        return appObjectId;
    }

    public void setAppObjectId(String appObjectId) {
        this.appObjectId = appObjectId;
    }

    public String getAppReplyUrl() {
        return appReplyUrl;
    }

    public void setAppReplyUrl(String appReplyUrl) {
        this.appReplyUrl = appReplyUrl;
    }

    public String getAppLoginUrl() {
        return appLoginUrl;
    }

    public void setAppLoginUrl(String appLoginUrl) {
        this.appLoginUrl = appLoginUrl;
    }

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public void setAuthorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
    }

    public String getCodeGrantFlowState() {
        return codeGrantFlowState;
    }

    public void setCodeGrantFlowState(String codeGrantFlowState) {
        this.codeGrantFlowState = codeGrantFlowState;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}

package com.sequenceiq.cloudbreak.cloud.azure.view;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public class AzureCredentialView {

    private static final String APP_BASED = "appBased";

    private static final String ROLE_BASED = "roleBased";

    private final CloudCredential cloudCredential;

    private final Map<String, Object> parameters;

    private final Map<String, String> appBasedParameters;

    private final Map<String, String> codeGrantFlowParameters;

    public AzureCredentialView(CloudCredential cloudCredential) {
        this.cloudCredential = cloudCredential;

        this.parameters = cloudCredential.getParameters().containsKey("azure")
                ? (Map<String, Object>) cloudCredential.getParameter("azure", Map.class) : new HashMap<>();

        this.appBasedParameters = parameters.containsKey(APP_BASED)
                ? (Map<String, String>) parameters.get(APP_BASED) : new HashMap<>();

        this.codeGrantFlowParameters = parameters.containsKey(ROLE_BASED)
                ? (Map<String, String>) parameters.get(ROLE_BASED) : new HashMap<>();
    }

    public String getCredentialCrn() {
        return cloudCredential.getId();
    }

    public String getName() {
        return cloudCredential.getName();
    }

    public String getSubscriptionId() {
        return (String) parameters.get("subscriptionId");
    }

    public String getTenantId() {
        return (String) parameters.get("tenantId");
    }

    public String getAccessKey() {
        return appBasedParameters.get("accessKey");
    }

    public String getSecretKey() {
        return appBasedParameters.get("secretKey");
    }

    public String getRoleName() {
        return cloudCredential.getParameter("roleName", String.class);
    }

    public String getRoleType() {
        return cloudCredential.getParameter("roleType", String.class);
    }

    public Boolean getCodeGrantFlow() {
        return cloudCredential.getParameter("codeGrantFlow", Boolean.class);
    }

    public String getAppLoginUrl() {
        return cloudCredential.getParameter("appLoginUrl", String.class);
    }

    public String getCodeGrantFlowState() {
        return cloudCredential.getParameter("codeGrantFlowState", String.class);
    }

    public String getAuthorizationCode() {
        return cloudCredential.getParameter("authorizationCode", String.class);
    }

    public String getRefreshToken() {
        return cloudCredential.getParameter("refreshToken", String.class);
    }

    public String getAppReplyUrl() {
        return cloudCredential.getParameter("appReplyUrl", String.class);
    }

    public String getDeploymentAddress() {
        return cloudCredential.getParameter("deploymentAddress", String.class);
    }
}

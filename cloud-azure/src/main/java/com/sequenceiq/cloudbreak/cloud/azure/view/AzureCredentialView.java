package com.sequenceiq.cloudbreak.cloud.azure.view;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public class AzureCredentialView {

    public static final String PROVIDER_KEY = "azure";

    public static final String APP_BASED = "appBased";

    public static final String LIGHTHOUSE_BASED = "lightHouseBased";

    public static final String CODE_GRANT_FLOW_BASED = "codeGrantFlowBased";

    public static final String CODE_GRANT_FLOW_STATE_KEY = "codeGrantFlowState";

    private final CloudCredential cloudCredential;

    private final Map<String, Object> parameters;

    private final Map<String, String> appBasedParameters;

    private final Map<String, String> lightHouseBasedParameters;

    private final Map<String, String> codeGrantFlowParameters;

    public AzureCredentialView(CloudCredential cloudCredential) {
        this.cloudCredential = cloudCredential;

        this.parameters = cloudCredential.getParameters().get(PROVIDER_KEY) != null
                ? (Map<String, Object>) cloudCredential.getParameter(PROVIDER_KEY, Map.class) : new HashMap<>();

        this.appBasedParameters = parameters.get(APP_BASED) != null
                ? (Map<String, String>) parameters.get(APP_BASED) : new HashMap<>();

        this.lightHouseBasedParameters = parameters.get(LIGHTHOUSE_BASED) != null
                ? (Map<String, String>) parameters.get(LIGHTHOUSE_BASED) : new HashMap<>();

        this.codeGrantFlowParameters = parameters.get(CODE_GRANT_FLOW_BASED) != null
                ? (Map<String, String>) parameters.get(CODE_GRANT_FLOW_BASED) : new HashMap<>();
    }

    public AzureCredentialView(String subscriptionId, String tenantId, String accessKey, String secretKey) {
        this.appBasedParameters = Map.of(
                "accessKey", accessKey,
                "secretKey", secretKey
        );
        this.parameters = Map.of(
                "subscriptionId", subscriptionId,
                "tenantId", tenantId
        );
        this.cloudCredential = null;
        this.lightHouseBasedParameters = new HashMap<>();
        this.codeGrantFlowParameters = new HashMap<>();
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
        return Optional.ofNullable(appBasedParameters.get("accessKey"))
                .orElse(codeGrantFlowParameters.get("accessKey"));
    }

    public String getSecretKey() {
        return Optional.ofNullable(appBasedParameters.get("secretKey"))
                .orElse(codeGrantFlowParameters.get("secretKey"));
    }

    public boolean codeGrantFlow() {
        return !codeGrantFlowParameters.isEmpty();
    }

    public String getAppLoginUrl() {
        return codeGrantFlowParameters.get("appLoginUrl");
    }

    public String getCodeGrantFlowState() {
        return codeGrantFlowParameters.get(CODE_GRANT_FLOW_STATE_KEY);
    }

    public String getAuthorizationCode() {
        return codeGrantFlowParameters.get("authorizationCode");
    }

    public String getRefreshToken() {
        return codeGrantFlowParameters.get("refreshToken");
    }

    public String getAppReplyUrl() {
        return codeGrantFlowParameters.get("appReplyUrl");
    }

    public String getDeploymentAddress() {
        return codeGrantFlowParameters.get("deploymentAddress");
    }
}

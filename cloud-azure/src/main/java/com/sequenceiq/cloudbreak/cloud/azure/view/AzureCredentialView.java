package com.sequenceiq.cloudbreak.cloud.azure.view;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public class AzureCredentialView {

    public static final String PROVIDER_KEY = "azure";

    public static final String APP_BASED = "appBased";

    public static final String CERTIFICATE = "certificate";

    public static final String CODE_GRANT_FLOW_BASED = "codeGrantFlowBased";

    public static final String CODE_GRANT_FLOW_STATE_KEY = "codeGrantFlowState";

    private final CloudCredential cloudCredential;

    private final Map<String, Object> parameters;

    private final Map<String, Object> appBasedParameters;

    private final Map<String, String> appBasedParametersCertificate;

    private final Map<String, String> codeGrantFlowParameters;

    public AzureCredentialView(CloudCredential cloudCredential) {
        this.cloudCredential = cloudCredential;

        this.parameters = cloudCredential.getParameters().get(PROVIDER_KEY) != null
                ? (Map<String, Object>) cloudCredential.getParameter(PROVIDER_KEY, Map.class) : new HashMap<>();

        this.appBasedParameters = parameters.get(APP_BASED) != null
                ? (Map<String, Object>) parameters.get(APP_BASED) : new HashMap<>();

        this.appBasedParametersCertificate = appBasedParameters.get(CERTIFICATE) != null
                ? (Map<String, String>) appBasedParameters.get(CERTIFICATE) : new HashMap<>();

        this.codeGrantFlowParameters = parameters.get(CODE_GRANT_FLOW_BASED) != null
                ? (Map<String, String>) parameters.get(CODE_GRANT_FLOW_BASED) : new HashMap<>();
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
        return Optional.ofNullable((String) appBasedParameters.get("accessKey"))
                .orElse(codeGrantFlowParameters.get("accessKey"));
    }

    public String getAuthenticationType() {
        return Optional.ofNullable((String) appBasedParameters.get("authenticationType"))
                .orElse("SECRET");
    }

    public String getPrivateKeyForCertificate() {
        return appBasedParametersCertificate.get("privateKey");
    }

    public String getCertificate() {
        return appBasedParametersCertificate.get("certificate");
    }

    public String getStatus() {
        return appBasedParametersCertificate.get("status");
    }

    public String getSecretKey() {
        return Optional.ofNullable((String) appBasedParameters.get("secretKey"))
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

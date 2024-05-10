package com.sequenceiq.cloudbreak.cloud.azure.view;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public class AzureCredentialView {

    public static final String PROVIDER_KEY = "azure";

    public static final String APP_BASED = "appBased";

    public static final String CERTIFICATE = "certificate";

    private final CloudCredential cloudCredential;

    private final Map<String, Object> parameters;

    private final Map<String, Object> appBasedParameters;

    private final Map<String, String> appBasedParametersCertificate;

    public AzureCredentialView(CloudCredential cloudCredential) {
        this.cloudCredential = cloudCredential;

        parameters = cloudCredential.getParameters().get(PROVIDER_KEY) != null
                ? (Map<String, Object>) cloudCredential.getParameter(PROVIDER_KEY, Map.class) : new HashMap<>();

        appBasedParameters = parameters.get(APP_BASED) != null
                ? (Map<String, Object>) parameters.get(APP_BASED) : new HashMap<>();

        appBasedParametersCertificate = appBasedParameters.get(CERTIFICATE) != null
                ? (Map<String, String>) appBasedParameters.get(CERTIFICATE) : new HashMap<>();
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
        return Optional.ofNullable((String) appBasedParameters.get("accessKey")).orElseThrow();
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
        return Optional.ofNullable((String) appBasedParameters.get("secretKey")).orElseThrow();
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }
}

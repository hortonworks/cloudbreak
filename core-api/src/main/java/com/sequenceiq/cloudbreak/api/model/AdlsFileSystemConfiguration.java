package com.sequenceiq.cloudbreak.api.model;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AdlsFileSystemConfiguration extends FileSystemConfiguration {

    public static final String CREDENTIAL_SECRET_KEY = "secretKey";

    public static final String SUBSCRIPTION_ID = "subscriptionId";

    public static final String TENANT_ID = "tenantId";

    public static final String ACCESS_KEY = "accessKey";

    public static final String ADLS_TRACKING_CLUSTERTYPE_KEY = "adl.events.tracking.clustertype";

    public static final String ADLS_TRACKING_CLUSTERNAME_KEY = "adl.events.tracking.clustername";

    public static final String ADLS_TRACKING_CLUSTERNAME_VALUE = "CLOUDBREAK";

    @NotNull
    @Pattern(regexp = "^[a-z0-9]{3,24}$",
            message = "Must contain only numbers and lowercase letters and must be between 3 and 24 characters long.")
    private String accountName;

    @NotNull
    private String tenantId;

    private String clientId;

    private String credential;

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
    }
}

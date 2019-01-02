package com.sequenceiq.cloudbreak.services.filesystem;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdlsFileSystem extends BaseFileSystem {

    private String accountName;

    private String clientId;

    private String credential;

    private String tenantId;

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
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

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AdlsFileSystem)) {
            return false;
        }
        AdlsFileSystem that = (AdlsFileSystem) o;
        return Objects.equals(getAccountName(), that.getAccountName())
                && Objects.equals(getClientId(), that.getClientId())
                && Objects.equals(getCredential(), that.getCredential())
                && Objects.equals(getTenantId(), that.getTenantId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAccountName(), getClientId(), getCredential(), getTenantId());
    }
}

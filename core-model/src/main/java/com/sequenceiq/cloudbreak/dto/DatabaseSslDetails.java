package com.sequenceiq.cloudbreak.dto;

import static java.util.Objects.requireNonNull;

import java.util.Set;

public class DatabaseSslDetails {
    private Set<String> sslCerts;

    private boolean sslEnabledForStack;

    public DatabaseSslDetails(Set<String> sslCerts, boolean sslEnabledForStack) {
        this.sslCerts = requireNonNull(sslCerts);
        this.sslEnabledForStack = sslEnabledForStack;
    }

    public Set<String> getSslCerts() {
        return sslCerts;
    }

    public void setSslCerts(Set<String> sslCerts) {
        this.sslCerts = requireNonNull(sslCerts);
    }

    public String getSslCertBundle() {
        return String.join("\n", getSslCerts());
    }

    public boolean isSslEnabledForStack() {
        return sslEnabledForStack;
    }

    public void setSslEnabledForStack(boolean sslEnabledForStack) {
        this.sslEnabledForStack = sslEnabledForStack;
    }

    @Override
    public String toString() {
        return "DatabaseSslDetails{" +
                "sslCerts=" + sslCerts +
                ", sslEnabledForStack=" + sslEnabledForStack +
                '}';
    }
}

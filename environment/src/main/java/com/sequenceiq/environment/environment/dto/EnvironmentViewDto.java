package com.sequenceiq.environment.environment.dto;

import com.sequenceiq.environment.credential.domain.CredentialView;
import com.sequenceiq.environment.proxy.domain.ProxyConfigView;

public class EnvironmentViewDto extends EnvironmentDtoBase {

    private CredentialView credentialView;

    private ProxyConfigView proxyConfig;

    public CredentialView getCredentialView() {
        return credentialView;
    }

    public void setCredentialView(CredentialView credentialView) {
        this.credentialView = credentialView;
    }

    public ProxyConfigView getProxyConfig() {
        return proxyConfig;
    }

    public void setProxyConfig(ProxyConfigView proxyConfig) {
        this.proxyConfig = proxyConfig;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public static final class Builder extends EnvironmentDtoBaseBuilder<EnvironmentViewDto, Builder> {

        private CredentialView credentialView;

        private ProxyConfigView proxyConfig;

        private Builder() {
        }

        public Builder withCredentialView(CredentialView credential) {
            credentialView = credential;
            return this;
        }

        public Builder withProxyConfig(ProxyConfigView proxyConfig) {
            this.proxyConfig = proxyConfig;
            return this;
        }

        @Override
        public EnvironmentViewDto build() {
            EnvironmentViewDto environmentDto = new EnvironmentViewDto();
            environmentDto.setCredentialView(credentialView);
            environmentDto.setProxyConfig(proxyConfig);
            build(environmentDto);
            return environmentDto;
        }
    }
}

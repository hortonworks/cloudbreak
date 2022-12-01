package com.sequenceiq.environment.environment.dto;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.EnvironmentDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential.CredentialDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.proxy.ProxyDetails;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;

@JsonDeserialize(builder = EnvironmentDto.EnvironmentDtoBuilder.class)
public class EnvironmentDto extends EnvironmentDtoBase implements EnvironmentDetails {

    private Credential credential;

    private ProxyConfig proxyConfig;

    private CredentialDetails credentialDetails;

    public Credential getCredential() {
        return credential;
    }

    public void setCredential(Credential credential) {
        this.credential = credential;
    }

    public ProxyDetails getProxyDetails() {
        ProxyDetails.Builder builder = ProxyDetails.Builder.builder();
        if (proxyConfig != null) {
            builder = builder.withEnabled(true)
                    .withProtocol(proxyConfig.getProtocol())
                    .withAuthentication(StringUtils.isNoneEmpty(proxyConfig.getUserName()));
        }
        return builder.build();
    }

    public ProxyConfig getProxyConfig() {
        return proxyConfig;
    }

    public void setProxyConfig(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
    }

    @Override
    public CredentialDetails getCredentialDetails() {
        return credentialDetails;
    }

    public void setCredentialDetails(CredentialDetails credentialDetails) {
        this.credentialDetails = credentialDetails;
    }

    public static EnvironmentDtoBuilder builder() {
        return new EnvironmentDtoBuilder();
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @JsonPOJOBuilder
    public static final class EnvironmentDtoBuilder extends EnvironmentDtoBase.EnvironmentDtoBaseBuilder<EnvironmentDto, EnvironmentDtoBuilder> {

        private Credential credential;

        private ProxyConfig proxyConfig;

        private CredentialDetails credentialDetails;

        private EnvironmentDtoBuilder() {
        }

        public EnvironmentDtoBuilder withCredential(Credential credential) {
            this.credential = credential;
            return this;
        }

        public EnvironmentDtoBuilder withProxyConfig(ProxyConfig proxyConfig) {
            this.proxyConfig = proxyConfig;
            return this;
        }

        public EnvironmentDtoBuilder withCredentialDetails(CredentialDetails credentialDetails) {
            this.credentialDetails = credentialDetails;
            return this;
        }

        public EnvironmentDto build() {
            EnvironmentDto environmentDto = new EnvironmentDto();
            environmentDto.setCredential(credential);
            environmentDto.setProxyConfig(proxyConfig);
            environmentDto.setCredentialDetails(credentialDetails);
            super.build(environmentDto);
            return environmentDto;
        }
    }
}

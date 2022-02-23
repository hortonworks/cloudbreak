package com.sequenceiq.environment.environment.dto;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.EnvironmentDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.proxy.ProxyDetails;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;

public class EnvironmentDto extends EnvironmentDtoBase implements EnvironmentDetails {

    private Credential credential;

    private ProxyConfig proxyConfig;

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

    public static EnvironmentDtoBuilder builder() {
        return new EnvironmentDtoBuilder();
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public static final class EnvironmentDtoBuilder extends EnvironmentDtoBase.EnvironmentDtoBaseBuilder<EnvironmentDto, EnvironmentDtoBuilder> {

        private Credential credential;

        private ProxyConfig proxyConfig;

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

        public EnvironmentDto build() {
            EnvironmentDto environmentDto = new EnvironmentDto();
            environmentDto.setCredential(credential);
            environmentDto.setProxyConfig(proxyConfig);
            super.build(environmentDto);
            return environmentDto;
        }
    }
}

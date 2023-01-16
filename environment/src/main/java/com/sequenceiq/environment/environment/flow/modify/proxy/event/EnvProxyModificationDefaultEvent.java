package com.sequenceiq.environment.environment.flow.modify.proxy.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;

public class EnvProxyModificationDefaultEvent extends BaseFlowEvent implements ProxyConfigModificationEvent {

    private final EnvironmentDto environmentDto;

    private final ProxyConfig proxyConfig;

    private final ProxyConfig previousProxyConfig;

    @JsonCreator
    public EnvProxyModificationDefaultEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("environmentDto") EnvironmentDto environmentDto,
            @JsonProperty("proxyConfig") ProxyConfig proxyConfig,
            @JsonProperty("previousProxyConfig") ProxyConfig previousProxyConfig,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, environmentDto.getId(), environmentDto.getResourceCrn(), accepted);
        this.environmentDto = environmentDto;
        this.proxyConfig = proxyConfig;
        this.previousProxyConfig = previousProxyConfig;
    }

    @Override
    public EnvironmentDto getEnvironmentDto() {
        return environmentDto;
    }

    @Override
    public ProxyConfig getProxyConfig() {
        return proxyConfig;
    }

    @Override
    public ProxyConfig getPreviousProxyConfig() {
        return previousProxyConfig;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EnvProxyModificationDefaultEvent)) {
            return false;
        }
        EnvProxyModificationDefaultEvent that = (EnvProxyModificationDefaultEvent) o;
        return Objects.equals(environmentDto, that.environmentDto)
                && Objects.equals(proxyConfig, that.proxyConfig)
                && Objects.equals(previousProxyConfig, that.previousProxyConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(environmentDto, proxyConfig);
    }

    public static EnvProxyModificationDefaultEventBuilder builder() {
        return new EnvProxyModificationDefaultEventBuilder();
    }

    public static final class EnvProxyModificationDefaultEventBuilder {
        private EnvironmentDto environmentDto;

        private ProxyConfig proxyConfig;

        private ProxyConfig previousProxyConfig;

        private String selector;

        private Promise<AcceptResult> accepted;

        private EnvProxyModificationDefaultEventBuilder() {
        }

        public EnvProxyModificationDefaultEventBuilder withEnvironmentDto(EnvironmentDto environmentDto) {
            this.environmentDto = environmentDto;
            return this;
        }

        public EnvProxyModificationDefaultEventBuilder withProxyConfig(ProxyConfig proxyConfig) {
            this.proxyConfig = proxyConfig;
            return this;
        }

        public EnvProxyModificationDefaultEventBuilder withPreviousProxyConfig(ProxyConfig previousProxyConfig) {
            this.previousProxyConfig = previousProxyConfig;
            return this;
        }

        public EnvProxyModificationDefaultEventBuilder withSelector(String selector) {
            this.selector = selector;
            return this;
        }

        public EnvProxyModificationDefaultEventBuilder withAccepted(Promise<AcceptResult> accepted) {
            this.accepted = accepted;
            return this;
        }

        public EnvProxyModificationDefaultEvent build() {
            return new EnvProxyModificationDefaultEvent(selector, environmentDto, proxyConfig, previousProxyConfig, accepted);
        }
    }
}

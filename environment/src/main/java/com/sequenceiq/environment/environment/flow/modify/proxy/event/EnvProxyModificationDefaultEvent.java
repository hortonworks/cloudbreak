package com.sequenceiq.environment.environment.flow.modify.proxy.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;

public class EnvProxyModificationDefaultEvent extends BaseFlowEvent implements ProxyConfigModificationEvent {

    private final EnvironmentDto environmentDto;

    private final ProxyConfig proxyConfig;

    @JsonCreator
    public EnvProxyModificationDefaultEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("environmentDto") EnvironmentDto environmentDto,
            @JsonProperty("proxyConfig") ProxyConfig proxyConfig) {
        super(selector, environmentDto.getId(), environmentDto.getResourceCrn());
        this.environmentDto = environmentDto;
        this.proxyConfig = proxyConfig;
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EnvProxyModificationDefaultEvent)) {
            return false;
        }
        EnvProxyModificationDefaultEvent that = (EnvProxyModificationDefaultEvent) o;
        return Objects.equals(environmentDto, that.environmentDto) && Objects.equals(proxyConfig, that.proxyConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(environmentDto, proxyConfig);
    }
}

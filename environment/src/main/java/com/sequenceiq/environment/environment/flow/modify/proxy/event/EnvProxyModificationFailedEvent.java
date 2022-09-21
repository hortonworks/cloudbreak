package com.sequenceiq.environment.environment.flow.modify.proxy.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;

public class EnvProxyModificationFailedEvent extends BaseFailedFlowEvent implements ProxyConfigModificationEvent {

    private final EnvironmentDto environmentDto;

    private final ProxyConfig proxyConfig;

    private final EnvironmentStatus environmentStatus;

    @JsonCreator
    public EnvProxyModificationFailedEvent(
            @JsonProperty("environmentDto") EnvironmentDto environmentDto,
            @JsonProperty("proxyConfig") ProxyConfig proxyConfig,
            @JsonProperty("environmentStatus") EnvironmentStatus environmentStatus,
            @JsonProperty("exception") Exception exception) {
        super(EnvProxyModificationStateSelectors.FAILED_MODIFY_PROXY_EVENT.selector(),
                environmentDto.getResourceId(), environmentDto.getName(), environmentDto.getResourceCrn(), exception);
        this.environmentDto = environmentDto;
        this.proxyConfig = proxyConfig;
        this.environmentStatus = environmentStatus;
    }

    @Override
    public EnvironmentDto getEnvironmentDto() {
        return environmentDto;
    }

    @Override
    public ProxyConfig getProxyConfig() {
        return proxyConfig;
    }

    public EnvironmentStatus getEnvironmentStatus() {
        return environmentStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EnvProxyModificationFailedEvent)) {
            return false;
        }
        EnvProxyModificationFailedEvent that = (EnvProxyModificationFailedEvent) o;
        return Objects.equals(environmentDto, that.environmentDto) && environmentStatus == that.environmentStatus;
    }

    @Override
    public int hashCode() {
        return Objects.hash(environmentDto, environmentStatus);
    }
}

package com.sequenceiq.environment.environment.flow.modify.proxy.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;

public class EnvProxyModificationFailedEvent extends BaseFailedFlowEvent implements ProxyConfigModificationEvent {

    private final EnvironmentDto environmentDto;

    private final ProxyConfig proxyConfig;

    private final ProxyConfig previousProxyConfig;

    private final EnvironmentStatus environmentStatus;

    @JsonCreator
    public EnvProxyModificationFailedEvent(
            @JsonProperty("environmentDto") EnvironmentDto environmentDto,
            @JsonProperty("proxyConfig") ProxyConfig proxyConfig,
            @JsonProperty("previousProxyConfig") ProxyConfig previousProxyConfig,
            @JsonProperty("environmentStatus") EnvironmentStatus environmentStatus,
            @JsonProperty("exception") Exception exception,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(EnvProxyModificationStateSelectors.FAILED_MODIFY_PROXY_EVENT.selector(),
                environmentDto.getResourceId(), accepted, environmentDto.getName(), environmentDto.getResourceCrn(), exception);
        this.environmentDto = environmentDto;
        this.proxyConfig = proxyConfig;
        this.previousProxyConfig = previousProxyConfig;
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

    @Override
    public ProxyConfig getPreviousProxyConfig() {
        return previousProxyConfig;
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
        return Objects.equals(environmentDto, that.environmentDto)
                && Objects.equals(proxyConfig, that.proxyConfig)
                && Objects.equals(previousProxyConfig, that.previousProxyConfig)
                && environmentStatus == that.environmentStatus;
    }

    @Override
    public int hashCode() {
        return Objects.hash(environmentDto, proxyConfig, previousProxyConfig, environmentStatus);
    }

    public static EnvProxyModificationFailedEventBuilder builder() {
        return new EnvProxyModificationFailedEventBuilder();
    }

    public static final class EnvProxyModificationFailedEventBuilder {
        private EnvironmentDto environmentDto;

        private ProxyConfig proxyConfig;

        private ProxyConfig previousProxyConfig;

        private EnvironmentStatus environmentStatus;

        private Exception exception;

        private Promise<AcceptResult> accepted;

        private EnvProxyModificationFailedEventBuilder() {
        }

        public EnvProxyModificationFailedEventBuilder withEnvironmentDto(EnvironmentDto environmentDto) {
            this.environmentDto = environmentDto;
            return this;
        }

        public EnvProxyModificationFailedEventBuilder withProxyConfig(ProxyConfig proxyConfig) {
            this.proxyConfig = proxyConfig;
            return this;
        }

        public EnvProxyModificationFailedEventBuilder withPreviousProxyConfig(ProxyConfig previousProxyConfig) {
            this.previousProxyConfig = previousProxyConfig;
            return this;
        }

        public EnvProxyModificationFailedEventBuilder withEnvironmentStatus(EnvironmentStatus environmentStatus) {
            this.environmentStatus = environmentStatus;
            return this;
        }

        public EnvProxyModificationFailedEventBuilder withException(Exception exception) {
            this.exception = exception;
            return this;
        }

        public EnvProxyModificationFailedEventBuilder withAccepted(Promise<AcceptResult> accepted) {
            this.accepted = accepted;
            return this;
        }

        public EnvProxyModificationFailedEvent build() {
            return new EnvProxyModificationFailedEvent(environmentDto, proxyConfig, previousProxyConfig, environmentStatus, exception, accepted);
        }
    }
}

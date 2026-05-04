package com.sequenceiq.cloudbreak.converter.v4.stacks;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.ConfigStalenessV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Responses;
import com.sequenceiq.cloudbreak.domain.projection.StackClusterStatusView;
import com.sequenceiq.common.api.type.ConfigStalenessState;

@Component
public class StackClusterStatusViewToStatusConverter {

    public StackStatusV4Response convert(StackClusterStatusView source) {
        StackStatusV4Response response = new StackStatusV4Response();
        response.setId(source.getId());
        response.setCrn(source.getCrn());
        response.setStatus(source.getStatus());
        response.setStatusReason(source.getStatusReason());
        response.setClusterStatus(source.getClusterStatus());
        response.setClusterStatusReason(source.getClusterStatusReason());
        response.setCertExpirationState(source.getCertExpirationState());
        response.setCertExpirationDetails(source.getCertExpirationDetails());
        response.setConfigStaleness(convertConfigStaleness(source));
        response.setProviderSyncStates(source.getProviderSyncStates());
        return response;
    }

    private ConfigStalenessV4Response convertConfigStaleness(StackClusterStatusView source) {
        ConfigStalenessV4Response configStaleness = new ConfigStalenessV4Response();
        configStaleness.setState(Objects.requireNonNullElse(source.getConfigStalenessState(), ConfigStalenessState.UP_TO_DATE).name());
        configStaleness.setDetails(source.getConfigStalenessDetails());
        return configStaleness;
    }

    public StackStatusV4Responses convert(Iterable<StackClusterStatusView> sources) {
        Set<StackStatusV4Response> jsons = new HashSet<>();
        for (StackClusterStatusView source : sources) {
            jsons.add(convert(source));
        }
        return new StackStatusV4Responses(jsons);
    }
}
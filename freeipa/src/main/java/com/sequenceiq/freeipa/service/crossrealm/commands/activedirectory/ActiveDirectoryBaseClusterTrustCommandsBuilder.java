package com.sequenceiq.freeipa.service.crossrealm.commands.activedirectory;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.service.crossrealm.TrustCommandType;
import com.sequenceiq.freeipa.service.crossrealm.commands.BaseClusterTrustCommandsBuilder;

@Component
public class ActiveDirectoryBaseClusterTrustCommandsBuilder extends BaseClusterTrustCommandsBuilder {
    @Inject
    private ActiveDirectoryBaseClusterKrb5ConfBuilder activeDirectoryBaseClusterKrb5ConfBuilder;

    @Override
    protected String buildKrb5Conf(String resourceName, TrustCommandType trustCommandType, FreeIpa freeIpa, CrossRealmTrust crossRealmTrust,
            LoadBalancer loadBalancer) {
        return activeDirectoryBaseClusterKrb5ConfBuilder.buildCommands(resourceName, trustCommandType, freeIpa, crossRealmTrust);
    }
}

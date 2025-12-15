package com.sequenceiq.freeipa.service.crossrealm.commands.mit;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.service.crossrealm.TrustCommandType;
import com.sequenceiq.freeipa.service.crossrealm.commands.BaseClusterTrustCommandsBuilder;

@Component
public class MitBaseClusterTrustCommandsBuilder extends BaseClusterTrustCommandsBuilder {
    @Inject
    private MitBaseClusterKrb5ConfBuilder mitBaseClusterKrb5ConfBuilder;

    @Override
    protected String buildKrb5Conf(String resourceName, TrustCommandType trustCommandType, FreeIpa freeIpa, CrossRealmTrust crossRealmTrust,
            LoadBalancer loadBalancer) {
        return mitBaseClusterKrb5ConfBuilder.buildCommands(resourceName, trustCommandType, freeIpa, crossRealmTrust, loadBalancer);
    }
}

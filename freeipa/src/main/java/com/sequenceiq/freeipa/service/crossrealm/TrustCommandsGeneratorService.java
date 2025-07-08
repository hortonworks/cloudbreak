package com.sequenceiq.freeipa.service.crossrealm;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands.ActiveDirectoryTrustSetupCommands;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands.BaseClusterTrustSetupCommands;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands.TrustSetupCommandsRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands.TrustSetupCommandsResponse;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;

@Component
public class TrustCommandsGeneratorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrustCommandsGeneratorService.class);

    @Inject
    private ActiveDirectoryCommandsBuilder activeDirectoryCommandsBuilder;

    @Inject
    private BaseClusterKrb5ConfBuilder baseClusterKrb5ConfBuilder;

    public TrustSetupCommandsResponse getTrustSetupCommands(TrustSetupCommandsRequest request, Stack stack, FreeIpa freeIpa, CrossRealmTrust crossRealmTrust) {
        LOGGER.info("Retrieving commands for cross-realm trust setup for active directory: {}", crossRealmTrust.getFqdn());
        TrustSetupCommandsResponse response = new TrustSetupCommandsResponse();
        response.setEnvironmentCrn(request.getEnvironmentCrn());
        ActiveDirectoryTrustSetupCommands adCommands = new ActiveDirectoryTrustSetupCommands();
        adCommands.setCommands(activeDirectoryCommandsBuilder.buildCommands(stack, freeIpa, crossRealmTrust));
        response.setActiveDirectoryCommands(adCommands);
        BaseClusterTrustSetupCommands baseClusterTrustSetupCommands = new BaseClusterTrustSetupCommands();
        baseClusterTrustSetupCommands.setKrb5Conf(baseClusterKrb5ConfBuilder.buildCommands(freeIpa, crossRealmTrust));
        response.setBaseClusterCommands(baseClusterTrustSetupCommands);
        return response;
    }
}

package com.sequenceiq.freeipa.service.crossrealm;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.type.KdcType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands.ActiveDirectoryTrustSetupCommands;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands.BaseClusterTrustSetupCommands;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands.MitTrustSetupCommands;
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
    private MitKdcCommandsBuilder mitKdcCommandsBuilder;

    @Inject
    private MitDnsInstructionsBuilder mitDnsInstructionsBuilder;

    @Inject
    private BaseClusterKrb5ConfBuilder baseClusterKrb5ConfBuilder;

    public TrustSetupCommandsResponse getTrustCommands(
            TrustCommandType trustCommandType, String environmentCrn, Stack stack, FreeIpa freeIpa, CrossRealmTrust crossRealmTrust) {
        LOGGER.debug("Retrieving {} commands for cross-realm trust setup on premises: {}, KDC type: {}",
                trustCommandType.name(), crossRealmTrust.getKdcFqdn(), crossRealmTrust.getKdcType());
        TrustSetupCommandsResponse response = new TrustSetupCommandsResponse();
        response.setEnvironmentCrn(environmentCrn);
        response.setKdcType(crossRealmTrust.getKdcType() != null ? crossRealmTrust.getKdcType().name() : KdcType.UNKNOWN.name());
        switch (crossRealmTrust.getKdcType()) {
            case ACTIVE_DIRECTORY -> {
                ActiveDirectoryTrustSetupCommands adCommands = new ActiveDirectoryTrustSetupCommands();
                adCommands.setCommands(activeDirectoryCommandsBuilder.buildCommands(trustCommandType, stack, freeIpa, crossRealmTrust));
                response.setActiveDirectoryCommands(adCommands);
            }
            case MIT -> {
                MitTrustSetupCommands mitCommands = new MitTrustSetupCommands();
                mitCommands.setKdcCommands(mitKdcCommandsBuilder.buildCommands(trustCommandType, freeIpa, crossRealmTrust));
                mitCommands.setDnsSetupInstructions(mitDnsInstructionsBuilder.buildCommands(trustCommandType, stack, freeIpa));
                response.setMitCommands(mitCommands);
            }
            case null, default -> throw new BadRequestException("Unsupported KDC type: " + crossRealmTrust.getKdcType());
        }
        BaseClusterTrustSetupCommands baseClusterTrustSetupCommands = new BaseClusterTrustSetupCommands();
        baseClusterTrustSetupCommands.setKrb5Conf(baseClusterKrb5ConfBuilder.buildCommands(trustCommandType, freeIpa, crossRealmTrust));
        response.setBaseClusterCommands(baseClusterTrustSetupCommands);
        return response;
    }
}

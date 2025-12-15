package com.sequenceiq.freeipa.service.crossrealm.commands.activedirectory;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands.ActiveDirectoryTrustSetupCommands;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.crossrealm.TrustCommandType;

@Component
public class ActiveDirectoryTrustInstructionsBuilder {
    @Inject
    private ActiveDirectoryKdcCommandsBuilder activeDirectoryKdcCommandsBuilder;

    public ActiveDirectoryTrustSetupCommands buildInstructions(TrustCommandType trustCommandType, Stack stack, FreeIpa freeIpa,
            CrossRealmTrust crossRealmTrust) {
        ActiveDirectoryTrustSetupCommands adCommands = new ActiveDirectoryTrustSetupCommands();
        adCommands.setCommands(activeDirectoryKdcCommandsBuilder.buildCommands(trustCommandType, stack, freeIpa, crossRealmTrust));
        return adCommands;
    }
}

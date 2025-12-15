package com.sequenceiq.freeipa.service.crossrealm.commands.mit;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands.MitTrustSetupCommands;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.crossrealm.TrustCommandType;

@Component
public class MitTrustInstructionsBuilder {
    @Inject
    private MitKdcCommandsBuilder mitKdcCommandsBuilder;

    @Inject
    private MitDnsInstructionsBuilder mitDnsInstructionsBuilder;

    public MitTrustSetupCommands buildInstructions(TrustCommandType trustCommandType, Stack stack, FreeIpa freeIpa,
            CrossRealmTrust crossRealmTrust) {
        MitTrustSetupCommands mitCommands = new MitTrustSetupCommands();
        mitCommands.setKdcCommands(mitKdcCommandsBuilder.buildCommands(trustCommandType, freeIpa, crossRealmTrust));
        mitCommands.setDnsSetupInstructions(mitDnsInstructionsBuilder.buildCommands(trustCommandType, stack, freeIpa));
        return mitCommands;
    }
}

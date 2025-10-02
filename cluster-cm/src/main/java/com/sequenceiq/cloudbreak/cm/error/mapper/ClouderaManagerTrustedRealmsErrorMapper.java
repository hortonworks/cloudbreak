package com.sequenceiq.cloudbreak.cm.error.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cm.exception.CommandDetails;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.util.DocumentationLinkProvider;

@Component
public class ClouderaManagerTrustedRealmsErrorMapper implements ClouderaManagerErrorMapper {

    @Override
    public boolean canHandle(StackDtoDelegate stack, List<CommandDetails> failedCommands) {
        return stack.getBlueprint().getHybridOption() != null
                && failedCommands.stream().anyMatch(command -> "RangerAdmin".equals(command.getName()));
    }

    @Override
    public String map(StackDtoDelegate stack, List<CommandDetails> failedCommands, String originalMessage) {
        return "Failed to create Ranger resource policies. "
                + "Please verify that trusted realms are configured correctly in On-Premises Environment: "
                + DocumentationLinkProvider.hybridSetupTrustedRealmsLink(stack.getStackVersion());
    }
}

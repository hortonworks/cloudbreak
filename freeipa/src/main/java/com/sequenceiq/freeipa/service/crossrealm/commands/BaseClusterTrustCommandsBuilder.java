package com.sequenceiq.freeipa.service.crossrealm.commands;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.util.DocumentationLinkProvider;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands.BaseClusterTrustSetupCommands;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands.ClouderaManagerSetupInstructions;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.crossrealm.TrustCommandType;

@Component
public abstract class BaseClusterTrustCommandsBuilder {
    private static final String ADD_EXPLANATION = "trust.addrealm.explanation";

    private static final String REMOVE_EXPLANATION = "trust.removerealm.explanation";

    @Inject
    private CloudbreakMessagesService messagesService;

    public BaseClusterTrustSetupCommands buildBaseClusterCommands(Stack stack, TrustCommandType trustCommandType,
            FreeIpa freeIpa, CrossRealmTrust crossRealmTrust, LoadBalancer loadBalancer) {
        BaseClusterTrustSetupCommands base = new BaseClusterTrustSetupCommands();
        base.setKrb5Conf(buildKrb5Conf(stack.getResourceName(), trustCommandType, freeIpa, crossRealmTrust, loadBalancer));
        ClouderaManagerSetupInstructions cmInstructions = new ClouderaManagerSetupInstructions();
        cmInstructions.setExplanation(getExplanation(trustCommandType));
        cmInstructions.setDocs(getDocLink(trustCommandType));
        base.setClouderaManagerSetupInstructions(cmInstructions);
        return base;
    }

    protected abstract String buildKrb5Conf(String resourceName, TrustCommandType trustCommandType, FreeIpa freeIpa, CrossRealmTrust crossRealmTrust,
            LoadBalancer loadBalancer);

    private String getExplanation(TrustCommandType trustCommandType) {
        return switch (trustCommandType) {
            case SETUP -> messagesService.getMessage(ADD_EXPLANATION);
            case CLEANUP -> messagesService.getMessage(REMOVE_EXPLANATION);
            case VALIDATION -> null;
            case null -> null;
        };
    }

    private String getDocLink(TrustCommandType trustCommandType) {
        return switch (trustCommandType) {
            case SETUP -> DocumentationLinkProvider.onPremisesTrustedRealmsLink();
            case CLEANUP -> null;
            case VALIDATION -> null;
            case null -> null;
        };
    }
}

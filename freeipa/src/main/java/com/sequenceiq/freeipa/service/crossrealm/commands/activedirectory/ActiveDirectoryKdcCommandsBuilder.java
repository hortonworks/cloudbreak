package com.sequenceiq.freeipa.service.crossrealm.commands.activedirectory;

import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.crossrealm.StackHelper;
import com.sequenceiq.freeipa.service.crossrealm.TrustCommandType;
import com.sequenceiq.freeipa.service.crossrealm.TrustDirection;
import com.sequenceiq.freeipa.service.crossrealm.commands.AbstractFreemarkerTemplateBuilder;

@Component
public class ActiveDirectoryKdcCommandsBuilder extends AbstractFreemarkerTemplateBuilder {
    @Inject
    private StackHelper stackHelper;

    public String buildCommands(TrustCommandType trustCommandType, Stack stack, FreeIpa freeIpa, CrossRealmTrust crossRealmTrust) {
        Map<String, Object> model = buildModel(trustCommandType, stack, freeIpa, crossRealmTrust);
        return build(trustCommandType, model);
    }

    public String buildCommands(TrustCommandType trustCommandType, Stack stack, FreeIpa freeIpa, CrossRealmTrust crossRealmTrust,
            TrustDirection trustDirection) {
        Map<String, Object> model = buildModel(trustCommandType, stack, freeIpa, crossRealmTrust);
        return build(String.format("crossrealmtrust/ad/activedirectory_%s_commands_%s.ftl",
                trustCommandType.name().toLowerCase(), trustDirection.name().toLowerCase()), model);
    }

    private Map<String, Object> buildModel(TrustCommandType trustCommandType, Stack stack, FreeIpa freeIpa, CrossRealmTrust crossRealmTrust) {
        Map<String, Object> model = new HashMap<>();
        model.put("trustCommandType", trustCommandType);
        model.put("adDomain", crossRealmTrust.getKdcRealm().toLowerCase());
        model.put("ipaDomain", freeIpa.getDomain());
        if (trustCommandType == TrustCommandType.SETUP) {
            model.put("ipaIpAddresses", stackHelper.getServerIps(stack));
            model.put("trustSecret", crossRealmTrust.getTrustSecret());
        }
        return model;
    }

    private String build(TrustCommandType trustCommandType, Map<String, Object> model) {
        return build(String.format("crossrealmtrust/ad/activedirectory_%s_commands.ftl", trustCommandType.name().toLowerCase()), model);
    }
}

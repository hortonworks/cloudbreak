package com.sequenceiq.freeipa.service.crossrealm;

import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;

@Component
public class MitDnsInstructionsBuilder extends AbstractFreemarkerTemplateBuilder {
    @Inject
    private StackHelper stackHelper;

    public String buildCommands(TrustCommandType trustCommandType, Stack stack, FreeIpa freeIpa) {
        Map<String, Object> model = new HashMap<>();
        model.put("ipaDomain", freeIpa.getDomain());
        if (trustCommandType == TrustCommandType.SETUP) {
            model.put("ipaIpAddresses", stackHelper.getServerIps(stack));
        }
        return build(trustCommandType, model);
    }

    private String build(TrustCommandType trustCommandType, Map<String, Object> model) {
        return build(String.format("crossrealmtrust/mit/dns_%s_instructions.ftl", trustCommandType.name().toLowerCase()), model);
    }
}

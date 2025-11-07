package com.sequenceiq.freeipa.service.crossrealm;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.FreeIpa;

@Component
public class MitKdcCommandsBuilder extends AbstractFreemarkerTemplateBuilder {
    public String buildCommands(TrustCommandType trustCommandType, FreeIpa freeIpa, CrossRealmTrust crossRealmTrust) {
        Map<String, Object> model = new HashMap<>();
        model.put("kdcRealm", crossRealmTrust.getKdcRealm().toUpperCase(Locale.ROOT));
        model.put("ipaRealm", freeIpa.getDomain().toUpperCase(Locale.ROOT));
        if (trustCommandType == TrustCommandType.SETUP) {
            model.put("trustSecret", crossRealmTrust.getTrustSecret());
        }
        return build(trustCommandType, model);
    }

    private String build(TrustCommandType trustCommandType, Map<String, Object> model) {
        return build(String.format("crossrealmtrust/mit/mit_kdc_%s_commands.ftl", trustCommandType.name().toLowerCase()), model);
    }
}

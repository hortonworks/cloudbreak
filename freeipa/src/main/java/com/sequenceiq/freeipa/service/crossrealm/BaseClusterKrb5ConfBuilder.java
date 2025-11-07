package com.sequenceiq.freeipa.service.crossrealm;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.FreeIpa;

@Component
public class BaseClusterKrb5ConfBuilder extends AbstractFreemarkerTemplateBuilder {
    public String buildCommands(TrustCommandType trustCommandType, FreeIpa freeIpa, CrossRealmTrust crossRealmTrust) {
        Map<String, Object> model = new HashMap<>();
        model.put("comment", getComment(trustCommandType));
        model.put("trustCommandType", trustCommandType);
        model.put("adDomain", crossRealmTrust.getKdcRealm());
        model.put("ipaDomain", freeIpa.getDomain());
        return build(model);
    }

    private String getComment(TrustCommandType trustCommandType) {
        return switch (trustCommandType) {
            case SETUP -> "Extend krb5.conf with the following content";
            case CLEANUP -> "Remove the following content from krb5.conf";
        };
    }

    private String build(Map<String, Object> model) {
        return build("crossrealmtrust/basecluster/basecluster_krb5conf.ftl", model);
    }
}

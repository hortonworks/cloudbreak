package com.sequenceiq.freeipa.service.crossrealm.commands.activedirectory;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.service.crossrealm.TrustCommandType;
import com.sequenceiq.freeipa.service.crossrealm.commands.AbstractFreemarkerTemplateBuilder;

@Component
public class ActiveDirectoryBaseClusterKrb5ConfBuilder extends AbstractFreemarkerTemplateBuilder {
    public String buildCommands(String resourceName, TrustCommandType trustCommandType, FreeIpa freeIpa, CrossRealmTrust crossRealmTrust) {
        Map<String, Object> model = new HashMap<>();
        model.put("filename", String.format("cdp_%s_krb5.conf", resourceName));
        model.put("comment", getComment(resourceName, trustCommandType));
        model.put("trustCommandType", trustCommandType);
        model.put("adDomain", crossRealmTrust.getKdcRealm());
        model.put("ipaDomain", freeIpa.getDomain());
        model.put("type", trustCommandType.name());
        return build(model);
    }

    private String getComment(String resourceName, TrustCommandType trustCommandType) {
        return switch (trustCommandType) {
            case SETUP -> String.format("""
                    Create a new file called cdp_%s_krb5.conf under /etc/krb5.conf.d with the following content
                    # Make sure the line "includedir /etc/krb5.conf.d/" is included by default at the top of /etc/krb5.conf
                    """, resourceName);
            case CLEANUP -> String.format("Delete the following Kerberos configuration file /etc/krb5.conf.d/cdp_%s_krb5.conf", resourceName);
            case VALIDATION -> "Execute the following commands to validate the cross-realm trust setup towards the trusted domain";
        };
    }

    private String build(Map<String, Object> model) {
        return build("crossrealmtrust/basecluster/ad_basecluster_krb5conf.ftl", model);
    }
}

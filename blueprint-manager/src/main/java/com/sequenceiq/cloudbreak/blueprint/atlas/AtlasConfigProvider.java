package com.sequenceiq.cloudbreak.blueprint.atlas;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.blueprint.BlueprintComponentConfigProvider;
import com.sequenceiq.cloudbreak.blueprint.BlueprintConfigurationEntry;
import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject;
import com.sequenceiq.cloudbreak.blueprint.template.views.LdapView;

@Component
public class AtlasConfigProvider implements BlueprintComponentConfigProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(AtlasConfigProvider.class);

    @Override
    public boolean ldapConfigShouldApply(BlueprintPreparationObject source, String blueprintText) {
        return source.getLdapView().isPresent();
    }

    @Override
    public List<BlueprintConfigurationEntry> ldapConfigs(BlueprintPreparationObject source, String blueprintText) {
        LOGGER.info("ranger-admin-site exists in Blueprint");
        String configFile = "application-properties";

        LdapView ldapView = source.getLdapView().get();

        List<BlueprintConfigurationEntry> configs = new ArrayList<>();
        configs.add(new BlueprintConfigurationEntry(configFile, "atlas.authentication.method.ldap.type", ldapView.getDirectoryTypeShort()));
        configs.add(new BlueprintConfigurationEntry(configFile, "atlas.authentication.method.ldap", "true"));
        configs.add(new BlueprintConfigurationEntry(configFile, "atlas.authentication.method.ldap.ad.domain", ldapView.getDomain()));
        configs.add(new BlueprintConfigurationEntry(configFile, "atlas.authentication.method.ldap.ad.url", ldapView.getConnectionURL()));
        configs.add(new BlueprintConfigurationEntry(configFile, "atlas.authentication.method.ldap.ad.base.dn", ldapView.getUserSearchBase()));
        configs.add(new BlueprintConfigurationEntry(configFile, "atlas.authentication.method.ldap.ad.bind.dn", ldapView.getBindDn()));
        configs.add(new BlueprintConfigurationEntry(configFile, "atlas.authentication.method.ldap.ad.bind.password", ldapView.getBindPassword()));
        configs.add(new BlueprintConfigurationEntry(configFile, "atlas.authentication.method.ldap.ugi-groups", "false"));
        configs.add(new BlueprintConfigurationEntry(configFile, "atlas.authorizer.impl", "ranger"));

        return configs;
    }

    @Override
    public Set<String> components() {
        return Sets.newHashSet("ATLAS_SERVER");
    }
}

package com.sequenceiq.cloudbreak.blueprint.ranger;

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
public class RangerAdminConfigProvider implements BlueprintComponentConfigProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(RangerAdminConfigProvider.class);

    @Override
    public boolean ldapConfigShouldApply(BlueprintPreparationObject source, String blueprintText) {
        return source.getLdapView().isPresent();
    }

    @Override
    public List<BlueprintConfigurationEntry> ldapConfigs(BlueprintPreparationObject source, String blueprintText) {
        LOGGER.info("ranger-admin-site exists in Blueprint");
        String configFile = "ranger-ugsync-site";

        LdapView ldapView = source.getLdapView().get();

        List<BlueprintConfigurationEntry> configs = new ArrayList<>();
        configs.add(new BlueprintConfigurationEntry(configFile, "ranger.usersync.enabled", "true"));
        configs.add(new BlueprintConfigurationEntry(configFile, "ranger.usersync.source.impl.class", "org.apache.ranger.ldapusersync.process.LdapUserGroupBuilder"));
        configs.add(new BlueprintConfigurationEntry(configFile, "ranger.usersync.ldap.url", ldapView.getConnectionURL()));
        configs.add(new BlueprintConfigurationEntry(configFile, "ranger.usersync.ldap.binddn", ldapView.getBindDn()));
        configs.add(new BlueprintConfigurationEntry(configFile, "ranger.usersync.ldap.ldapbindpassword", ldapView.getBindPassword()));
        configs.add(new BlueprintConfigurationEntry(configFile, "ranger.usersync.ldap.user.nameattribute", ldapView.getUserNameAttribute()));
        configs.add(new BlueprintConfigurationEntry(configFile, "ranger.usersync.ldap.user.searchbase", ldapView.getUserSearchBase()));
        configs.add(new BlueprintConfigurationEntry(configFile, "ranger.usersync.ldap.user.objectclass", ldapView.getUserObjectClass()));
        configs.add(new BlueprintConfigurationEntry(configFile, "ranger.usersync.ldap.deltasync", "false"));
        configs.add(new BlueprintConfigurationEntry(configFile, "ranger.usersync.group.searchenabled", "true"));
        configs.add(new BlueprintConfigurationEntry(configFile, "ranger.usersync.group.memberattributename", ldapView.getGroupMemberAttribute()));
        configs.add(new BlueprintConfigurationEntry(configFile, "ranger.usersync.group.nameattribute", ldapView.getGroupNameAttribute()));
        configs.add(new BlueprintConfigurationEntry(configFile, "ranger.usersync.group.objectclass", ldapView.getGroupObjectClass()));
        configs.add(new BlueprintConfigurationEntry(configFile, "ranger.usersync.group.searchbase", ldapView.getGroupSearchBase()));
        configs.add(new BlueprintConfigurationEntry(configFile, "ranger.usersync.group.searchfilter", " "));


        String rangerAdminSite = "ranger-admin-site";
        configs.add(new BlueprintConfigurationEntry(rangerAdminSite, "ranger.jpa.jdbc.driver", "org.postgresql.Driver"));
        configs.add(new BlueprintConfigurationEntry(rangerAdminSite, "ranger.audit.source.type", "solr"));
        configs.add(new BlueprintConfigurationEntry(rangerAdminSite, "ranger.authentication.method", ldapView.getDirectoryType().name()));
        configs.add(new BlueprintConfigurationEntry(rangerAdminSite, "ranger.ldap.ad.domain", " "));
        configs.add(new BlueprintConfigurationEntry(rangerAdminSite, "ranger.ldap.ad.url", ldapView.getConnectionURL()));
        configs.add(new BlueprintConfigurationEntry(rangerAdminSite, "ranger.ldap.ad.bind.dn", ldapView.getBindDn()));
        configs.add(new BlueprintConfigurationEntry(rangerAdminSite, "ranger.ldap.ad.bind.password", ldapView.getBindPassword()));
        configs.add(new BlueprintConfigurationEntry(rangerAdminSite, "ranger.ldap.ad.base.dn", ldapView.getUserSearchBase()));
        configs.add(new BlueprintConfigurationEntry(rangerAdminSite, "ranger.ldap.ad.user.searchfilter",
                String.format("(%s={0})", ldapView.getUserNameAttribute())));
        configs.add(new BlueprintConfigurationEntry(rangerAdminSite, "ranger.ldap.url", ldapView.getConnectionURL()));
        configs.add(new BlueprintConfigurationEntry(rangerAdminSite, "ranger.ldap.bind.dn", ldapView.getBindDn()));
        configs.add(new BlueprintConfigurationEntry(rangerAdminSite, "ranger.ldap.bind.password", ldapView.getBindPassword()));
        configs.add(new BlueprintConfigurationEntry(rangerAdminSite, "ranger.ldap.base.dn", ldapView.getUserSearchBase()));
        configs.add(new BlueprintConfigurationEntry(rangerAdminSite, "ranger.ldap.user.searchfilter",
                String.format("(%s={0})", ldapView.getUserNameAttribute())));
        configs.add(new BlueprintConfigurationEntry(rangerAdminSite, "ranger.ldap.user.dnpattern",
                String.format("%s={0},%s", ldapView.getUserNameAttribute(), ldapView.getUserSearchBase())));
        configs.add(new BlueprintConfigurationEntry(rangerAdminSite, "ranger.ldap.group.searchbase", ldapView.getGroupSearchBase()));
        configs.add(new BlueprintConfigurationEntry(rangerAdminSite, "ranger.ldap.group.roleattribute", ldapView.getGroupNameAttribute()));

        return configs;
    }

    @Override
    public Set<String> components() {
        return Sets.newHashSet("RANGER_ADMIN");
    }
}

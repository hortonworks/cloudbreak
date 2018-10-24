package com.sequenceiq.cloudbreak.service.cluster.ambari;

import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariRepositoryVersionService.AMBARI_VERSION_2_7_2_0;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.vault.VaultService;
import com.sequenceiq.cloudbreak.template.views.LdapView;

@Service
public class AmbariLdapService {

    public static final String AMBARI_SERVER_CONF_LDAP_PASSWORD_DAT = "/etc/ambari-server/conf/ldap-password.dat";

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariLdapService.class);

    @Inject
    private AmbariClientFactory clientFactory;

    @Inject
    private AmbariRepositoryVersionService ambariRepositoryVersionService;

    @Inject
    private VaultService vaultService;

    public void setupLdap(Stack stack, Cluster cluster, AmbariRepo ambariRepo) {
        AmbariClient ambariClient = clientFactory.getAmbariClient(stack, cluster);
        LdapConfig ldapConfig = cluster.getLdapConfig();
        if (ldapConfig != null) {
            LOGGER.info("Setup LDAP on Ambari API for stack: {}", stack.getId());
            String bindPassword = vaultService.resolveSingleValue(ldapConfig.getBindPassword());
            LdapView ldapView = new LdapView(ldapConfig, bindPassword);
            Map<String, Object> ldapConfigs = new HashMap<>();
            ldapConfigs.put("ambari.ldap.authentication.enabled", true);
            ldapConfigs.put("ambari.ldap.connectivity.server.host", ldapView.getHost());
            ldapConfigs.put("ambari.ldap.connectivity.server.port", ldapView.getPort());
            ldapConfigs.put("ambari.ldap.connectivity.secondary.server.host", ldapView.getHost());
            ldapConfigs.put("ambari.ldap.connectivity.secondary.server.port", ldapView.getPort());
            ldapConfigs.put("ambari.ldap.connectivity.use_ssl", ldapView.isSecure());
            ldapConfigs.put("ambari.ldap.attributes.dn_attr", ldapView.getUserDnPattern());
            ldapConfigs.put("ambari.ldap.attributes.user.object_class", ldapView.getUserObjectClass());
            ldapConfigs.put("ambari.ldap.attributes.group.object_class", ldapView.getGroupObjectClass());
            ldapConfigs.put("ambari.ldap.attributes.user.name_attr", ldapView.getUserNameAttribute());
            ldapConfigs.put("ambari.ldap.attributes.group.name_attr", ldapView.getGroupNameAttribute());
            ldapConfigs.put("ambari.ldap.attributes.user.search_base", ldapView.getUserSearchBase());
            ldapConfigs.put("ambari.ldap.attributes.group.search_base", ldapView.getGroupSearchBase());
            ldapConfigs.put("ambari.ldap.attributes.group.member_attr", ldapView.getGroupMemberAttribute());
            ldapConfigs.put("ambari.ldap.connectivity.bind_dn", ldapView.getBindDn());
            ldapConfigs.put("ambari.ldap.connectivity.bind_password", AMBARI_SERVER_CONF_LDAP_PASSWORD_DAT);
            ldapConfigs.put("ambari.ldap.advanced.referrals", "follow");
            ldapConfigs.put("ambari.ldap.connectivity.anonymous_bind", false);
            if (ambariRepositoryVersionService.isVersionNewerOrEqualThanLimited(ambariRepo::getVersion, AMBARI_VERSION_2_7_2_0)) {
                ldapConfigs.put("ambari.ldap.advanced.collision_behavior", "convert");
            } else {
                ldapConfigs.put("ambari.ldap.advance.collision_behavior", "convert");
            }
            ldapConfigs.put("ambari.ldap.advanced.force_lowercase_usernames", false);
            ldapConfigs.put("ambari.ldap.advanced.pagination_enabled", true);
            ldapConfigs.put("ambari.ldap.advanced.group_mapping_rules", ldapView.getAdminGroup());
            ambariClient.configureLdap(ldapConfigs);
        }
    }

    public void syncLdap(Stack stack, Cluster cluster) {
        LOGGER.info("Sync LDAP on Ambari API for stack: {}", stack.getId());
        AmbariClient ambariClient = clientFactory.getAmbariClient(stack, cluster);
        ambariClient.syncLdap();
    }

}

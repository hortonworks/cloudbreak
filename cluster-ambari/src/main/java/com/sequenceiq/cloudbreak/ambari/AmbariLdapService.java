package com.sequenceiq.cloudbreak.ambari;

import static com.sequenceiq.cloudbreak.ambari.AmbariRepositoryVersionService.AMBARI_VERSION_2_7_100_0;
import static com.sequenceiq.cloudbreak.ambari.AmbariRepositoryVersionService.AMBARI_VERSION_2_7_2_0;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.LdapView;

@Service
public class AmbariLdapService {

    public static final String AMBARI_SERVER_CONF_LDAP_PASSWORD_DAT = "/etc/ambari-server/conf/ldap-password.dat";

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariLdapService.class);

    @Inject
    private AmbariRepositoryVersionService ambariRepositoryVersionService;

    public void setupLdap(Stack stack, Cluster cluster, AmbariRepo ambariRepo, AmbariClient ambariClient, LdapView ldapConfig)
            throws IOException, URISyntaxException {
        if (ldapConfig != null) {
            LOGGER.debug("Setup LDAP on Ambari API for stack: {}", stack.getId());
            Map<String, Object> ldapConfigs = new HashMap<>();
            ldapConfigs.put("ambari.ldap.authentication.enabled", true);
            ldapConfigs.put("ambari.ldap.connectivity.server.host", ldapConfig.getServerHost());
            ldapConfigs.put("ambari.ldap.connectivity.server.port", ldapConfig.getServerPort());
            ldapConfigs.put("ambari.ldap.connectivity.secondary.server.host", ldapConfig.getServerHost());
            ldapConfigs.put("ambari.ldap.connectivity.secondary.server.port", ldapConfig.getServerPort());
            ldapConfigs.put("ambari.ldap.connectivity.use_ssl", ldapConfig.isSecure());
            ldapConfigs.put("ambari.ldap.attributes.dn_attr", ldapConfig.getUserDnPattern());
            ldapConfigs.put("ambari.ldap.attributes.user.object_class", ldapConfig.getUserObjectClass());
            ldapConfigs.put("ambari.ldap.attributes.group.object_class", ldapConfig.getGroupObjectClass());
            ldapConfigs.put("ambari.ldap.attributes.user.name_attr", ldapConfig.getUserNameAttribute());
            ldapConfigs.put("ambari.ldap.attributes.group.name_attr", ldapConfig.getGroupNameAttribute());
            ldapConfigs.put("ambari.ldap.attributes.user.search_base", ldapConfig.getUserSearchBase());
            ldapConfigs.put("ambari.ldap.attributes.group.search_base", ldapConfig.getGroupSearchBase());
            ldapConfigs.put("ambari.ldap.attributes.group.member_attr", ldapConfig.getGroupMemberAttribute());
            ldapConfigs.put("ambari.ldap.connectivity.bind_dn", ldapConfig.getBindDn());
            ldapConfigs.put("ambari.ldap.connectivity.bind_password", AMBARI_SERVER_CONF_LDAP_PASSWORD_DAT);
            ldapConfigs.put("ambari.ldap.advanced.referrals", "follow");
            ldapConfigs.put("ambari.ldap.connectivity.anonymous_bind", false);
            if (ambariRepositoryVersionService.isVersionNewerOrEqualThanLimited(ambariRepo::getVersion, AMBARI_VERSION_2_7_2_0)) {
                ldapConfigs.put("ambari.ldap.advanced.collision_behavior", "convert");
                if (ambariRepositoryVersionService.isVersionNewerOrEqualThanLimited(ambariRepo::getVersion, AMBARI_VERSION_2_7_100_0)) {
                    ldapConfigs.put("ambari.ldap.advanced.disable_endpoint_identification", true);
                }
            } else {
                ldapConfigs.put("ambari.ldap.advance.collision_behavior", "convert");
            }
            ldapConfigs.put("ambari.ldap.advanced.force_lowercase_usernames", false);
            ldapConfigs.put("ambari.ldap.advanced.pagination_enabled", true);
            ldapConfigs.put("ambari.ldap.advanced.group_mapping_rules", ldapConfig.getAdminGroup());
            ambariClient.configureLdap(ldapConfigs);
        }
    }

    public void syncLdap(Stack stack, AmbariClient ambariClient, LdapView ldapView) throws IOException, URISyntaxException {
        if (ldapView != null) {
            LOGGER.debug("Sync LDAP on Ambari API for stack: {}", stack.getId());
            ambariClient.syncLdap();
        }
    }

}

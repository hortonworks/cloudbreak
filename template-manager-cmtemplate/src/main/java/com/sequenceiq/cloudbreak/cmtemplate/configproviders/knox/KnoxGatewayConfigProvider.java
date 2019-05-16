package com.sequenceiq.cloudbreak.cmtemplate.configproviders.knox;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.template.views.LdapView;

@Component
public class KnoxGatewayConfigProvider extends AbstractRoleConfigConfigProvider {

    private static final String KNOX_MASTER_SECRET = "gateway_master_secret";

    // This needs to be removed once Knox CSD with PAM is merged
    private static final String SEC_GROUP_MAPPING = "hadoop_security_group_mapping_class";

    // This needs to be removed once Knox CSD with PAM is merged
    private static final String LDAP_USR = "ldap_connection_user";

    // This needs to be removed once Knox CSD with PAM is merged
    private static final String LDAP_PWD = "ldap_connection_password";

    // This needs to be removed once Knox CSD with PAM is merged
    private static final String LDAP_URL = "ldap_connection_url";

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfig(String roleType, HostgroupView hostGroupView, TemplatePreparationObject source) {
        switch (roleType) {
            case "KNOX_GATEWAY":
                List<ApiClusterTemplateConfig> config = new ArrayList<>();

                config.add(config(SEC_GROUP_MAPPING, "org.apache.hadoop.security.ShellBasedUnixGroupsMapping"));
                config.add(config(KNOX_MASTER_SECRET, source.getGatewayView().getMasterSecret()));

                // This needs to be removed once Knox CSD with PAM is merged
                Optional<LdapView> ldapConfigOpt = source.getLdapConfig();
                if (ldapConfigOpt.isPresent()) {
                    LdapView ldapView = ldapConfigOpt.get();
                    config.add(config(LDAP_URL, String.format("%s://%s:%d", ldapView.getProtocol(), ldapView.getServerHost(), ldapView.getServerPort())));
                    config.add(config(LDAP_USR, ldapView.getBindDn()));
                    config.add((config(LDAP_PWD, ldapView.getBindPassword())));
                } else {
                    config.add(config(LDAP_URL, "ldap://localhost:3389"));
                    config.add(config(LDAP_USR, "admin"));
                    config.add(config(LDAP_PWD, "admin-password"));
                }

                return config;
            default:
                return List.of();
        }
    }

    @Override
    public String getServiceType() {
        return "KNOX";
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of("KNOX_GATEWAY");
    }
}

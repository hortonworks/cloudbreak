package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hue;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.DirectoryType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.template.views.LdapView;
import com.sequenceiq.cloudbreak.type.KerberosType;

@Component
public class HueLdapConfigProvider extends AbstractRoleConfigConfigProvider {

    private static final String HUE_AUTH_BACKEND = "auth_backend";

    private static final String BACKEND_LDAP_BACKEND = "desktop.auth.backend.LdapBackend";

    private static final String LDAP_CERT = "ldap_cert";

    private static final String FREEIPA_CERT_PATH = "/etc/ipa/ca.crt";

    private static final String HUE_SEARCH_BIND_AUTHENTICATION = "search_bind_authentication";

    private static final String HUE_NT_DOMAIN = "nt_domain";

    private static final String HUE_LDAP_URL = "ldap_url";

    private static final String HUE_BASE_DN = "base_dn";

    private static final String HUE_BIND_DN = "bind_dn";

    private static final String HUE_BIND_PASSWORD = "bind_password";

    private static final String HUE_USER_NAME_ATTR = "user_name_attr";

    private static final String HUE_USE_START_TLS = "use_start_tls";

    private static final String HUE_GROUP_MEMBER_ATTR = "group_member_attr";

    private static final String HUE_GROUP_NAME_ATTR = "group_name_attr";

    private static final String HUE_GROUP_FILTER = "group_filter";

    private static final String HUE_USER_FILTER = "user_filter";

    private static final String HUE_TEST_LDAP_GROUP = "test_ldap_group";

    private static final String ADMINS = "admins";

    private static final String HUE_TEST_LDAP_USER = "test_ldap_user";

    private static final String ADMIN = "admin";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(TemplatePreparationObject source) {
        LdapView ldapView = source.getLdapConfig().get();
        List<ApiClusterTemplateConfig> result = new ArrayList<>();
        result.add(config(HUE_AUTH_BACKEND, BACKEND_LDAP_BACKEND));
        result.add(config(HUE_SEARCH_BIND_AUTHENTICATION, Boolean.TRUE.toString()));
        result.add(config(HUE_LDAP_URL, ldapView.getConnectionURL()));
        result.add(config(HUE_USE_START_TLS, Boolean.toString(ldapView.isSecure())));

        result.add(config(HUE_BASE_DN, ldapView.getUserSearchBase()));
        result.add(config(HUE_BIND_DN, ldapView.getBindDn()));
        result.add(config(HUE_BIND_PASSWORD, ldapView.getBindPassword()));

        result.add(config(HUE_GROUP_FILTER, String.format("(objectclass=%s)", ldapView.getGroupObjectClass())));
        result.add(config(HUE_GROUP_MEMBER_ATTR, ldapView.getGroupMemberAttribute()));
        result.add(config(HUE_GROUP_NAME_ATTR, ldapView.getGroupNameAttribute()));

        result.add(config(HUE_USER_FILTER, String.format("(objectclass=%s)", ldapView.getUserObjectClass())));
        result.add(config(HUE_USER_NAME_ATTR, ldapView.getUserNameAttribute()));

        result.add(config(HUE_TEST_LDAP_GROUP, ADMINS));
        result.add(config(HUE_TEST_LDAP_USER, ADMIN));

        if (ldapView.getDirectoryType() == DirectoryType.ACTIVE_DIRECTORY) {
            result.add(config(HUE_NT_DOMAIN, ldapView.getDomain()));
        }

        return List.copyOf(result);
    }

    @Override
    public String getServiceType() {
        return "HUE";
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(HueRoles.HUE_SERVER, HueRoles.HUE_LOAD_BALANCER);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return source.getLdapConfig().isPresent() && cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfig(String roleType, HostgroupView hostGroupView, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> result = new ArrayList<>();
        if (HueRoles.HUE_SERVER.equals(roleType)) {
            source.getKerberosConfig()
                  .filter(kc -> kc.getType() == KerberosType.FREEIPA)
                  .ifPresent((c) -> result.add(config(LDAP_CERT, FREEIPA_CERT_PATH)));
            LdapView ldapView = source.getLdapConfig().get();
            result.add(
                config("hue_server_hue_safety_valve",
                       String.format("[impala]\nauth_username=%s\nauth_password=%s\n", "admin", ldapView.getBindPassword())));
        }
        return List.copyOf(result);
    }
}

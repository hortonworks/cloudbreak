package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateVariable;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.LdapView;

@Component
public class HueLdapConfigProvider implements CmTemplateComponentConfigProvider {

    private static final String HUE_LDAP_URL = "hue-ldap_url";

    private static final String HUE_NT_DOMAIN = "hue-nt_domain";

    private static final String HUE_BASE_DN = "hue-base_dn";

    private static final String HUE_BIND_DN = "hue-bind_dn";

    private static final String HUE_BIND_PASSWORD = "hue-bind_password";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(TemplatePreparationObject templatePreparationObject) {
        List<ApiClusterTemplateConfig> result = new ArrayList<>();
        result.add(new ApiClusterTemplateConfig().name("auth_backend").value("desktop.auth.backend.LdapBackend"));
        result.add(new ApiClusterTemplateConfig().name("ldap_url").variable(HUE_LDAP_URL));
        result.add(new ApiClusterTemplateConfig().name("nt_domain").variable(HUE_NT_DOMAIN));
        // TODO Add ldap_cert cfg if needed
        result.add(new ApiClusterTemplateConfig().name("search_bind_authentication").value(Boolean.TRUE.toString()));
        result.add(new ApiClusterTemplateConfig().name("base_dn").variable(HUE_BASE_DN));
        result.add(new ApiClusterTemplateConfig().name("bind_dn").variable(HUE_BIND_DN));
        result.add(new ApiClusterTemplateConfig().name("bind_password").variable(HUE_BIND_PASSWORD));
        result.add(new ApiClusterTemplateConfig().name("use_start_tls").value(Boolean.FALSE.toString()));
        // TODO Add navmetadataserver_ldap_password cfg if needed
        // TODO Add navmetadataserver_ldap_user cfg if needed
        // TODO Add create_users_on_login cfg if needed
        // TODO Add group_filter cfg if needed
        // TODO Add group_member_attr cfg if needed
        // TODO Add group_name_attr cfg if needed
        // TODO Add ldap_username_pattern cfg if needed
        // TODO Add test_ldap_group cfg if needed
        // TODO Add test_ldap_user cfg if needed
        // TODO Add user_filter cfg if needed
        // TODO Add user_name_attr cfg if needed
        return result;
    }

    @Override
    public List<ApiClusterTemplateVariable> getServiceConfigVariables(TemplatePreparationObject source) {
        List<ApiClusterTemplateVariable> result = new ArrayList<>();
        LdapView ldapView = source.getLdapConfig().get();
        result.add(new ApiClusterTemplateVariable().name(HUE_LDAP_URL).value(ldapView.getConnectionURL()));
        result.add(new ApiClusterTemplateVariable().name(HUE_NT_DOMAIN).value(ldapView.getDomain()));
        // TODO Add ldap_cert var if needed
        result.add(new ApiClusterTemplateVariable().name(HUE_BASE_DN).value(ldapView.getUserSearchBase()));
        result.add(new ApiClusterTemplateVariable().name(HUE_BIND_DN).value(ldapView.getBindDn()));
        result.add(new ApiClusterTemplateVariable().name(HUE_BIND_PASSWORD).value(ldapView.getBindPassword()));
        // TODO Add navmetadataserver_ldap_password var if needed
        // TODO Add navmetadataserver_ldap_user var if needed
        // TODO Add create_users_on_login var if needed
        // TODO Add group_filter var if needed
        // TODO Add group_member_attr var if needed
        // TODO Add group_name_attr var if needed
        // TODO Add ldap_username_pattern var if needed
        // TODO Add test_ldap_group var if needed
        // TODO Add test_ldap_user var if needed
        // TODO Add user_filter var if needed
        // TODO Add user_name_attr var if needed
        return result;
    }

    @Override
    public String getServiceType() {
        return "HUE";
    }

    @Override
    public List<String> getRoleTypes() {
        return Collections.singletonList("HUE_SERVER");
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return source.getLdapConfig().isPresent() && cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }

}

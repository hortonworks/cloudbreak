package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive;

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
public class HiveServer2LdapConfigProvider implements CmTemplateComponentConfigProvider {

    private static final String HS_2_HIVESERVER_2_LDAP_URI = "hs2-hiveserver2_ldap_uri";

    private static final String HS_2_HIVESERVER_2_LDAP_DOMAIN = "hs2-hiveserver2_ldap_domain";

    private static final String HS_2_HIVESERVER_2_LDAP_BASEDN = "hs2-hiveserver2_ldap_basedn";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(TemplatePreparationObject templatePreparationObject) {
        List<ApiClusterTemplateConfig> result = new ArrayList<>();
        result.add(new ApiClusterTemplateConfig().name("hiveserver2_ldap_uri").variable(HS_2_HIVESERVER_2_LDAP_URI));
        result.add(new ApiClusterTemplateConfig().name("hiveserver2_ldap_domain").variable(HS_2_HIVESERVER_2_LDAP_DOMAIN));
        result.add(new ApiClusterTemplateConfig().name("hiveserver2_ldap_basedn").variable(HS_2_HIVESERVER_2_LDAP_BASEDN));
        result.add(new ApiClusterTemplateConfig().name("hiveserver2_enable_ldap_auth").value(Boolean.TRUE.toString()));
        return result;
    }

    @Override
    public List<ApiClusterTemplateVariable> getServiceConfigVariables(TemplatePreparationObject source) {
        List<ApiClusterTemplateVariable> result = new ArrayList<>();
        LdapView ldapView = source.getLdapConfig().get();
        result.add(new ApiClusterTemplateVariable().name(HS_2_HIVESERVER_2_LDAP_URI).value(ldapView.getConnectionURL()));
        result.add(new ApiClusterTemplateVariable().name(HS_2_HIVESERVER_2_LDAP_DOMAIN).value(ldapView.getDomain()));
        result.add(new ApiClusterTemplateVariable().name(HS_2_HIVESERVER_2_LDAP_BASEDN).value(ldapView.getGroupSearchBase()));
        return result;
    }

    @Override
    public String getServiceType() {
        return "HIVE";
    }

    @Override
    public List<String> getRoleTypes() {
        return Collections.singletonList("HIVESERVER2");
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return source.getGatewayView() == null && source.getLdapConfig().isPresent()
                && cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }

}

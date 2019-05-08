package com.sequenceiq.cloudbreak.cmtemplate.configproviders.solr;

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
public class SolrLdapConfigProvider implements CmTemplateComponentConfigProvider {

    private static final String SOLR_SOLR_LDAP_URI = "solr-solr_ldap_uri";

    private static final String SOLR_LDAP_DOMAIN = "solr-ldap_domain";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(TemplatePreparationObject templatePreparationObject) {
        List<ApiClusterTemplateConfig> result = new ArrayList<>();
        result.add(new ApiClusterTemplateConfig().name("solr_enable_ldap_auth").value(Boolean.TRUE.toString()));
        result.add(new ApiClusterTemplateConfig().name("solr_ldap_uri").variable(SOLR_SOLR_LDAP_URI));
        result.add(new ApiClusterTemplateConfig().name("ldap_domain").variable(SOLR_LDAP_DOMAIN));
        // TODO Add solr_ldap_basedn cfg if needed
        // TODO Add solr_ldap_enable_starttls cfg if needed
        return result;
    }

    @Override
    public List<ApiClusterTemplateVariable> getServiceConfigVariables(TemplatePreparationObject source) {
        List<ApiClusterTemplateVariable> result = new ArrayList<>();
        LdapView ldapView = source.getLdapConfig().get();
        result.add(new ApiClusterTemplateVariable().name(SOLR_SOLR_LDAP_URI).value(ldapView.getConnectionURL()));
        result.add(new ApiClusterTemplateVariable().name(SOLR_LDAP_DOMAIN).value(ldapView.getDomain()));
        // TODO Add solr_ldap_basedn var if needed
        // TODO Add solr_ldap_enable_starttls var if needed
        return result;
    }

    @Override
    public String getServiceType() {
        return "SOLR";
    }

    @Override
    public List<String> getRoleTypes() {
        return Collections.singletonList("SOLR_SERVER");
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return source.getLdapConfig().isPresent() && cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }

}

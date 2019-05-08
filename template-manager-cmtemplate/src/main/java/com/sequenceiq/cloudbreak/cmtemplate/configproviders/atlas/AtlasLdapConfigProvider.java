package com.sequenceiq.cloudbreak.cmtemplate.configproviders.atlas;

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
public class AtlasLdapConfigProvider implements CmTemplateComponentConfigProvider {

    private static final String ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_TYPE = "atlas-atlas_authentication_method_ldap_type";

    private static final String ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_AD_DOMAIN = "atlas-atlas_authentication_method_ldap_ad_domain";

    private static final String ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_AD_URL = "atlas-atlas_authentication_method_ldap_ad_url";

    private static final String ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_AD_BASE_DN = "atlas-atlas_authentication_method_ldap_ad_base_dn";

    private static final String ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_AD_BIND_DN = "atlas-atlas_authentication_method_ldap_ad_bind_dn";

    private static final String ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_AD_BIND_PASSWORD = "atlas-atlas_authentication_method_ldap_ad_bind_password";

    private static final String ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_USER_DNPATTERN = "atlas-atlas_authentication_method_ldap_userDNpattern";

    private static final String ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_GROUP_SEARCH_BASE = "atlas-atlas_authentication_method_ldap_groupSearchBase";

    private static final String ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_GROUP_SEARCH_FILTER = "atlas-atlas_authentication_method_ldap_groupSearchFilter";

    private static final String ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_URL = "atlas-atlas_authentication_method_ldap_url";

    private static final String ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_BASE_DN = "atlas-atlas_authentication_method_ldap_base_dn";

    private static final String ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_BIND_DN = "atlas-atlas_authentication_method_ldap_bind_dn";

    private static final String ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_BIND_PASSWORD = "atlas-atlas_authentication_method_ldap_bind_password";

    private static final String ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_USER_SEARCHFILTER = "atlas-atlas_authentication_method_ldap_user_searchfilter";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(TemplatePreparationObject templatePreparationObject) {
        List<ApiClusterTemplateConfig> result = new ArrayList<>();
        result.add(new ApiClusterTemplateConfig().name("atlas_authentication_method_ldap_type").variable(ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_TYPE));
        result.add(new ApiClusterTemplateConfig().name("atlas_authentication_method_ldap").value(Boolean.TRUE.toString()));
        result.add(new ApiClusterTemplateConfig().name("atlas_authentication_method_ldap_ad_domain")
                .variable(ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_AD_DOMAIN));
        result.add(new ApiClusterTemplateConfig().name("atlas_authentication_method_ldap_ad_url").variable(ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_AD_URL));
        result.add(new ApiClusterTemplateConfig().name("atlas_authentication_method_ldap_ad_base_dn")
                .variable(ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_AD_BASE_DN));
        result.add(new ApiClusterTemplateConfig().name("atlas_authentication_method_ldap_ad_bind_dn")
                .variable(ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_AD_BIND_DN));
        result.add(new ApiClusterTemplateConfig().name("atlas_authentication_method_ldap_ad_bind_password")
                .variable(ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_AD_BIND_PASSWORD));
        result.add(new ApiClusterTemplateConfig().name("atlas_authentication_method_ldap_ugi_groups").value(Boolean.FALSE.toString()));
        if (templatePreparationObject.getLdapConfig().get().isLdap()) {
            // Note: The Ambari Handlebars template had a setting for atlas-application.properties / atlas.authentication.method.ldap.domain, but that seems to
            // be an invalid property name.
            result.add(new ApiClusterTemplateConfig().name("atlas_authentication_method_ldap_userDNpattern")
                    .variable(ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_USER_DNPATTERN));
            result.add(new ApiClusterTemplateConfig().name("atlas_authentication_method_ldap_groupSearchBase")
                    .variable(ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_GROUP_SEARCH_BASE));
            result.add(new ApiClusterTemplateConfig().name("atlas_authentication_method_ldap_groupSearchFilter")
                    .variable(ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_GROUP_SEARCH_FILTER));
            result.add(new ApiClusterTemplateConfig().name("atlas_authentication_method_ldap_url").variable(ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_URL));
            result.add(new ApiClusterTemplateConfig().name("atlas_authentication_method_ldap_base_dn")
                    .variable(ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_BASE_DN));
            result.add(new ApiClusterTemplateConfig().name("atlas_authentication_method_ldap_bind_dn")
                    .variable(ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_BIND_DN));
            result.add(new ApiClusterTemplateConfig().name("atlas_authentication_method_ldap_bind_password")
                    .variable(ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_BIND_PASSWORD));
            result.add(new ApiClusterTemplateConfig().name("atlas_authentication_method_ldap_user_searchfilter")
                    .variable(ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_USER_SEARCHFILTER));
        }
        return result;
    }

    @Override
    public List<ApiClusterTemplateVariable> getServiceConfigVariables(TemplatePreparationObject source) {
        List<ApiClusterTemplateVariable> result = new ArrayList<>();
        LdapView ldapView = source.getLdapConfig().get();
        result.add(new ApiClusterTemplateVariable().name(ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_TYPE).value(ldapView.getDirectoryTypeShort()));
        result.add(new ApiClusterTemplateVariable().name(ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_AD_DOMAIN).value(ldapView.getDomain()));
        result.add(new ApiClusterTemplateVariable().name(ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_AD_URL).value(ldapView.getConnectionURL()));
        result.add(new ApiClusterTemplateVariable().name(ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_AD_BASE_DN).value(ldapView.getUserSearchBase()));
        result.add(new ApiClusterTemplateVariable().name(ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_AD_BIND_DN).value(ldapView.getBindDn()));
        result.add(new ApiClusterTemplateVariable().name(ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_AD_BIND_PASSWORD).value(ldapView.getBindPassword()));
        if (ldapView.isLdap()) {
            result.add(new ApiClusterTemplateVariable().name(ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_USER_DNPATTERN).value(ldapView.getUserDnPattern()));
            result.add(new ApiClusterTemplateVariable().name(ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_GROUP_SEARCH_BASE).value(ldapView.getGroupSearchBase()));
            result.add(new ApiClusterTemplateVariable().name(ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_GROUP_SEARCH_FILTER).value("member={0}"));
            result.add(new ApiClusterTemplateVariable().name(ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_URL).value(ldapView.getConnectionURL()));
            result.add(new ApiClusterTemplateVariable().name(ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_BASE_DN).value(ldapView.getUserSearchBase()));
            result.add(new ApiClusterTemplateVariable().name(ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_BIND_DN).value(ldapView.getBindDn()));
            result.add(new ApiClusterTemplateVariable().name(ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_BIND_PASSWORD).value(ldapView.getBindPassword()));
            result.add(new ApiClusterTemplateVariable().name(ATLAS_ATLAS_AUTHENTICATION_METHOD_LDAP_USER_SEARCHFILTER).value("mail={0}"));
        }
        return result;
    }

    @Override
    public String getServiceType() {
        return "ATLAS";
    }

    @Override
    public List<String> getRoleTypes() {
        return Collections.singletonList("ATLAS_SERVER");
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return source.getLdapConfig().isPresent() && cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }

}

package com.sequenceiq.cloudbreak.cmtemplate.configproviders.impala;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.configVar;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.variable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateVariable;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.dto.LdapView;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.type.KerberosType;

@Component
public class ImpalaLdapConfigProvider extends AbstractRoleConfigProvider {

    private static final String IMPALA_IMPALA_LDAP_URI = "impala-impala_ldap_uri";

    private static final String IMPALA_LDAP_BASE_DN = "impala-ldap_baseDN";

    private static final String IMPALA_LDAP_DOMAIN = "impala-ldap_domain";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> result = new ArrayList<>();
        result.add(config("enable_ldap_auth", Boolean.TRUE.toString()));
        result.add(configVar("impala_ldap_uri", IMPALA_IMPALA_LDAP_URI));
        LdapView ldapView = source.getLdapConfig().get();
        if (ldapView.isLdap()) {
            result.add(configVar("ldap_baseDN", IMPALA_LDAP_BASE_DN));
        } else {
            result.add(configVar("ldap_domain", IMPALA_LDAP_DOMAIN));
        }
        if (!ldapView.isSecure()) {
            result.add(config("enable_ldap_tls", Boolean.TRUE.toString()));
        }
        return result;
    }

    @Override
    public List<ApiClusterTemplateVariable> getServiceConfigVariables(TemplatePreparationObject source) {
        List<ApiClusterTemplateVariable> result = new ArrayList<>();
        LdapView ldapView = source.getLdapConfig().get();
        result.add(variable(IMPALA_IMPALA_LDAP_URI, ldapView.getConnectionURL()));
        if (ldapView.isLdap()) {
            result.add(variable(IMPALA_LDAP_BASE_DN, ldapView.getUserSearchBase()));
        } else {
            result.add(variable(IMPALA_LDAP_DOMAIN, ldapView.getDomain()));
        }
        return result;
    }

    @Override
    public String getServiceType() {
        return ImpalaRoles.SERVICE_IMPALA;
    }

    @Override
    public List<String> getRoleTypes() {
        return Collections.singletonList(ImpalaRoles.ROLE_IMPALAD);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return source.getLdapConfig().isPresent() && cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        switch (roleType) {
            case ImpalaRoles.ROLE_IMPALAD:
                Optional<KerberosConfig> kerberosConfig = source.getKerberosConfig();
                if (kerberosConfig.isPresent() && kerberosConfig.get().getType() == KerberosType.FREEIPA) {
                    return List.of(
                            config("impalad_ldap_ca_certificate", "/etc/ipa/ca.crt")
                    );
                } else {
                    return List.of();
                }
            default:
                return List.of();
        }
    }

}

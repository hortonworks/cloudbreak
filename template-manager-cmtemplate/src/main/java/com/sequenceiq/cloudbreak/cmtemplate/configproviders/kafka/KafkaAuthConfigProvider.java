package com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.List;

import com.google.common.collect.Lists;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.dto.LdapView;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class KafkaAuthConfigProvider implements CmTemplateComponentConfigProvider {

    private static final String LDAP_AUTH_URL = "ldap.auth.url";

    private static final String LDAP_AUTH_USER_DN_TEMPLATE = "ldap.auth.user.dn.template";

    private static final String LDAP_AUTH_ENABLE = "ldap.auth.enable";

    private static final String SASL_AUTH_METHOD = "sasl.plain.auth";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        KafkaConfigProviderUtils.CdhVersionForStreaming cdhVersion = KafkaConfigProviderUtils.getCdhVersionForStreaming(source);
        LdapView ldapView = source.getLdapConfig().get();
        switch (cdhVersion) {
            case VERSION_7_0_2:
                return ldapConfig(ldapView);
            case VERSION_7_0_2_2_OR_LATER:
                return ldapAndPamConfig(ldapView);
            case VERSION_7_0_2_CANNOT_DETERMINE_PATCH:
                return generalAuthConfig(ldapView);
            default:
                return List.of();
        }
    }

    private List<ApiClusterTemplateConfig> ldapConfig(LdapView ldapView) {
        List<ApiClusterTemplateConfig> config = generalAuthConfig(ldapView);
        config.add(config(LDAP_AUTH_ENABLE, "true"));
        return config;
    }

    private List<ApiClusterTemplateConfig> ldapAndPamConfig(LdapView ldapView) {
        List<ApiClusterTemplateConfig> config = generalAuthConfig(ldapView);
        config.add(config(SASL_AUTH_METHOD, "PAM"));
        return config;
    }

    private List<ApiClusterTemplateConfig> generalAuthConfig(LdapView ldapView) {
        return Lists.newArrayList(
                config(LDAP_AUTH_URL, ldapView.getConnectionURL()),
                config(LDAP_AUTH_USER_DN_TEMPLATE, ldapView.getUserDnPattern()));
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return source.getLdapConfig().isPresent() && cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(KafkaRoles.KAFKA_BROKER);
    }

    @Override
    public String getServiceType() {
        return KafkaRoles.KAFKA_SERVICE;
    }
}

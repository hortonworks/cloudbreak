package com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.List;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.dto.LdapView;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class KafkaAuthConfigProvider implements CmTemplateComponentConfigProvider {

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        KafkaConfigProviderUtils.KafkaAuthConfigType authType = KafkaConfigProviderUtils.getCdhVersionForStreaming(source).authType();
        LdapView ldapView = source.getLdapConfig().get();
        switch (authType) {
            case LDAP_AUTH:
                return ldapConfig(ldapView);
            case SASL_PAM_AUTH:
                return ldapAndPamConfig(ldapView);
            case LDAP_BASE_CONFIG:
                return generalAuthConfig(ldapView);
            default:
                return List.of();
        }
    }

    private List<ApiClusterTemplateConfig> ldapConfig(LdapView ldapView) {
        List<ApiClusterTemplateConfig> config = generalAuthConfig(ldapView);
        config.add(config(KafkaConfigs.LDAP_AUTH_ENABLE, "true"));
        return config;
    }

    private List<ApiClusterTemplateConfig> ldapAndPamConfig(LdapView ldapView) {
        List<ApiClusterTemplateConfig> config = generalAuthConfig(ldapView);
        config.add(config(KafkaConfigs.SASL_AUTH_METHOD, "PAM"));
        return config;
    }

    private List<ApiClusterTemplateConfig> generalAuthConfig(LdapView ldapView) {
        return Lists.newArrayList(
                config(KafkaConfigs.LDAP_AUTH_URL, ldapView.getConnectionURL()),
                config(KafkaConfigs.LDAP_AUTH_USER_DN_TEMPLATE, ldapView.getUserDnPattern()));
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return StackType.WORKLOAD.equals(source.getStackType())
                && source.getLdapConfig().isPresent()
                && cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
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

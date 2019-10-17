package com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.dto.LdapView;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class KafkaLdapConfigProvider implements CmTemplateComponentConfigProvider {

    private static final String LDAP_AUTH_URL = "ldap.auth.url";

    private static final String LDAP_AUTH_USER_DN_TEMPLATE = "ldap.auth.user.dn.template";

    private static final String LDAP_AUTH_ENABLE = "ldap.auth.enable";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        LdapView ldapView = source.getLdapConfig().get();
        return List.of(
                config(LDAP_AUTH_ENABLE, "true"),
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

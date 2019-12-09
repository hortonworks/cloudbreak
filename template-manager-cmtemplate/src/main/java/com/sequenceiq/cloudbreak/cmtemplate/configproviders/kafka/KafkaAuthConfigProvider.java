package com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_0_2;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_1_0;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.List;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
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
        String cdhVersion = templateProcessor.getVersion().orElse("");
        LdapView ldapView = source.getLdapConfig().get();

        return supportsPam(cdhVersion) ? ldapAndPamConfig(ldapView) :
                supportsLdap(cdhVersion) ? ldapConfig(ldapView) : List.of();
    }

    private boolean supportsLdap(String cdhVersion) {
        return isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERAMANAGER_VERSION_7_0_2);
    }

    private boolean supportsPam(String cdhVersion) {
        return isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERAMANAGER_VERSION_7_1_0);
    }

    private List<ApiClusterTemplateConfig> ldapConfig(LdapView ldapView) {
        return generalLdapConfig(ldapView, config(LDAP_AUTH_ENABLE, "true"));
    }

    private List<ApiClusterTemplateConfig> ldapAndPamConfig(LdapView ldapView) {
        return generalLdapConfig(ldapView, config(SASL_AUTH_METHOD, "PAM"));
    }

    private List<ApiClusterTemplateConfig> generalLdapConfig(LdapView ldapView, ApiClusterTemplateConfig additionalConfig) {
        return List.of(
            config(LDAP_AUTH_URL, ldapView.getConnectionURL()),
            config(LDAP_AUTH_USER_DN_TEMPLATE, ldapView.getUserDnPattern()),
            additionalConfig);
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

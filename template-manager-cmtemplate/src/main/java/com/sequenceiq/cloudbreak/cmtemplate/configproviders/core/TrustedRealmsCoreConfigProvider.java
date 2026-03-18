package com.sequenceiq.cloudbreak.cmtemplate.configproviders.core;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.dto.TrustView;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class TrustedRealmsCoreConfigProvider extends CommonCoreConfigProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrustedRealmsCoreConfigProvider.class);

    private static final String TRUSTED_REALMS = "trusted_realms";

    private static final String AUTH_TO_LOCAL_LOWERCASE = "set_auth_to_local_to_lowercase";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> serviceConfigs = new ArrayList<>();

        List<String> trustedRealms = new ArrayList<>();
        source.getTrustView()
                .map(TrustView::realm)
                .map(String::toUpperCase)
                .ifPresentOrElse(trustedRealms::add, () -> LOGGER.warn("Missing realm for trust"));
        source.getKerberosConfig()
                .map(KerberosConfig::getRealm)
                .ifPresentOrElse(trustedRealms::add, () -> LOGGER.warn("Missing kerberos realm"));
        if (!trustedRealms.isEmpty()) {
            serviceConfigs.add(config(TRUSTED_REALMS, String.join(",", trustedRealms)));
            serviceConfigs.add(config(AUTH_TO_LOCAL_LOWERCASE, Boolean.TRUE.toString()));
        }
        LOGGER.debug("Core-settings config params for cross realm trust: {}", serviceConfigs);
        return serviceConfigs;
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return source.getTrustView().isPresent();
    }
}

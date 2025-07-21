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
import com.sequenceiq.cloudbreak.template.views.DatalakeView;

@Component
public class HybridCoreConfigProvider extends CommonCoreConfigProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(HybridCoreConfigProvider.class);

    private static final String HADOOP_RPC_PROTECTION = "hadoop_rpc_protection";

    private static final String TRUSTED_REALMS = "trusted_realms";

    private static final String AUTH_TO_LOCAL_LOWERCASE = "set_auth_to_local_to_lowercase";

    private static final String HIVE = "HIVE";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> serviceConfigs = new ArrayList<>();

        source.getDatalakeView()
                .map(DatalakeView::getRdcView)
                .map(rdcView -> rdcView.getServiceConfig(HIVE, HADOOP_RPC_PROTECTION))
                .ifPresentOrElse(rpcProtection -> serviceConfigs.add(config(HADOOP_RPC_PROTECTION, rpcProtection)),
                        () -> LOGGER.warn("Missing {} config parameter from remote context", HADOOP_RPC_PROTECTION));

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
        return cmTemplateProcessor.isHybridDatahub(source);
    }
}

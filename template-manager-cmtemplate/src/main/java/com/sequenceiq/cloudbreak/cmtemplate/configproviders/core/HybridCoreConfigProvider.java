package com.sequenceiq.cloudbreak.cmtemplate.configproviders.core;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.HADOOP_RPC_PROTECTION;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveRoles.HIVE;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.DatalakeView;

@Component
public class HybridCoreConfigProvider extends CommonCoreConfigProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(HybridCoreConfigProvider.class);

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> serviceConfigs = new ArrayList<>();

        source.getDatalakeView()
                .map(DatalakeView::getRdcView)
                .map(rdcView -> rdcView.getServiceConfig(HIVE, HADOOP_RPC_PROTECTION))
                .ifPresentOrElse(rpcProtection -> serviceConfigs.add(config(HADOOP_RPC_PROTECTION, rpcProtection)),
                        () -> LOGGER.warn("Missing {} config parameter from remote context", HADOOP_RPC_PROTECTION));


        LOGGER.debug("Core-settings config params for hybrid: {}", serviceConfigs);
        return serviceConfigs;
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return cmTemplateProcessor.isHybridDatahub(source);
    }
}

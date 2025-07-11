package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.getSafetyValveProperty;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsConfigHelper.HDFS_CLIENT_CONFIG_SAFETY_VALVE;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsConfigHelper.HDFS_SERVICE_CONFIG_SAFETY_VALVE;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsConfigHelper.HYBRID_DH_NAME_SERVICE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.sdx.RdcView;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class HdfsHybridRoleConfigProvider extends AbstractRoleConfigProvider {

    @Inject
    private HdfsConfigHelper hdfsConfigHelper;

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> configs = new ArrayList<>();
        getRemoteHdfsHaSafetyValveValue(source)
                .ifPresent(safetyValveValue -> configs.add(config(HDFS_CLIENT_CONFIG_SAFETY_VALVE, safetyValveValue)));
        return configs;
    }

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> configs = new ArrayList<>();
        getRemoteHdfsHaSafetyValveValue(source)
                .ifPresent(safetyValveValue -> configs.add(config(HDFS_SERVICE_CONFIG_SAFETY_VALVE, safetyValveValue)));
        return configs;
    }

    @Override
    public String getServiceType() {
        return HdfsRoles.HDFS;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(HdfsRoles.GATEWAY);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return super.isConfigurationNeeded(cmTemplateProcessor, source) && cmTemplateProcessor.isHybridDatahub(source);
    }

    private Optional<String> getRemoteHdfsHaSafetyValveValue(TemplatePreparationObject source) {
        RdcView rdcView = source.getDatalakeView().get().getRdcView();
        String datalakeNameService = hdfsConfigHelper.getNameService(rdcView);
        if (ObjectUtils.isNotEmpty(datalakeNameService)) {
            String nameServices = Joiner.on(',').join(List.of(datalakeNameService, HYBRID_DH_NAME_SERVICE));
            StringBuilder configValueBuilder = new StringBuilder();
            configValueBuilder
                    .append(getSafetyValveProperty("dfs.nameservices", nameServices))
                    .append(getSafetyValveProperty("dfs.internal.nameservices", HYBRID_DH_NAME_SERVICE))
                    .append(getSafetyValveProperty("dfs.client.failover.proxy.provider." + datalakeNameService,
                            "org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider"));

            rdcView.getRoleConfigs(HdfsRoles.HDFS, HdfsRoles.NAMENODE).entrySet().stream()
                    .filter(HdfsHybridRoleConfigProvider::isHAConfig)
                    .map(entry -> getSafetyValveProperty(entry.getKey(), entry.getValue()))
                    .forEach(configValueBuilder::append);
            return Optional.of(configValueBuilder.toString());
        }
        return Optional.empty();
    }

    private static boolean isHAConfig(Map.Entry<String, String> entry) {
        return entry.getKey().startsWith("dfs.ha.namenodes.") || entry.getKey().startsWith("dfs.namenode.");
    }
}

package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.getSafetyValveProperty;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsConfigHelper.HDFS_CLIENT_CONFIG_SAFETY_VALVE;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsConfigHelper.HYBRID_DH_NAME_SERVICE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.sdx.RdcView;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class HdfsHybridRoleConfigProvider extends AbstractRoleConfigProvider {

    private static final Joiner COMMA_JOINER = Joiner.on(',');

    @Inject
    private HdfsConfigHelper hdfsConfigHelper;

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> configs = new ArrayList<>();
        RdcView rdcView = source.getDatalakeView().get().getRdcView();
        configs.addAll(getRemoteHdfsHaConfigs(rdcView));
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
        return super.isConfigurationNeeded(cmTemplateProcessor, source) && hdfsConfigHelper.isHybridDatahub(cmTemplateProcessor, source);
    }

    private List<ApiClusterTemplateConfig> getRemoteHdfsHaConfigs(RdcView rdcView) {
        List<String> nameNodes = List.copyOf(rdcView.getEndpoints(HdfsRoles.HDFS, HdfsRoles.NAMENODE));
        if (nameNodes.size() > 1) {
            StringBuilder clientConfigBuilder = new StringBuilder();
            String datalakeNameService = hdfsConfigHelper.getNameService(rdcView);
            String nameServices = COMMA_JOINER.join(List.of(datalakeNameService, HYBRID_DH_NAME_SERVICE));
            clientConfigBuilder
                    .append(getSafetyValveProperty("dfs.nameservices", nameServices))
                    .append(getSafetyValveProperty("dfs.internal.nameservices", HYBRID_DH_NAME_SERVICE))
                    .append(getSafetyValveProperty("dfs.client.failover.proxy.provider." + datalakeNameService,
                            "org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider"));

            rdcView.getRoleConfigs(HdfsRoles.HDFS, HdfsRoles.NAMENODE).entrySet().stream()
                    .filter(HdfsHybridRoleConfigProvider::isHAConfig)
                    .map(entry -> getSafetyValveProperty(entry.getKey(), entry.getValue()))
                    .forEach(clientConfigBuilder::append);
            return List.of(config(HDFS_CLIENT_CONFIG_SAFETY_VALVE, clientConfigBuilder.toString()));
        }
        return List.of();
    }

    private static boolean isHAConfig(Map.Entry<String, String> entry) {
        return entry.getKey().matches("dfs\\.namenode\\..*-address\\..*") || entry.getKey().matches("dfs\\.ha\\.namenodes\\..*");
    }
}

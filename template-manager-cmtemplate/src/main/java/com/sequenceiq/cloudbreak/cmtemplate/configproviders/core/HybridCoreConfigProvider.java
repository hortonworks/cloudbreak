package com.sequenceiq.cloudbreak.cmtemplate.configproviders.core;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.getSafetyValveProperty;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.CORE_DEFAULTFS;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.CORE_SITE_SAFETY_VALVE;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.DFS_NAMENODE_KERBEROS_PRINCIPAL;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.HADOOP_RPC_PROTECTION;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsRoles.HDFS;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsRoles.NAMENODE;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveRoles.HIVE;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsConfigHelper;
import com.sequenceiq.cloudbreak.dto.TrustView;
import com.sequenceiq.cloudbreak.sdx.RdcView;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.DatalakeView;
import com.sequenceiq.common.model.CloudStorageCdpService;

@Component
public class HybridCoreConfigProvider extends CommonCoreConfigProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(HybridCoreConfigProvider.class);

    @Inject
    private HdfsConfigHelper hdfsConfigHelper;

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> serviceConfigs = new ArrayList<>();

        source.getDatalakeView()
                .map(DatalakeView::getRdcView)
                .map(rdcView -> rdcView.getServiceConfig(HIVE, HADOOP_RPC_PROTECTION))
                .ifPresentOrElse(rpcProtection -> serviceConfigs.add(config(HADOOP_RPC_PROTECTION, rpcProtection)),
                        () -> LOGGER.warn("Missing {} config parameter from remote context", HADOOP_RPC_PROTECTION));

        if (!templateProcessor.isRoleTypePresentInService(HDFS, Lists.newArrayList(NAMENODE))) {
            serviceConfigs.addAll(getRemoteHdfsPropertiesForStubDfs(source));
        }

        LOGGER.debug("Core-settings config params for hybrid cross realm trust: {}", serviceConfigs);
        return serviceConfigs;
    }

    private List<ApiClusterTemplateConfig> getRemoteHdfsPropertiesForStubDfs(TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> serviceConfigs = new ArrayList<>();
        ConfigUtils.getStorageLocationForServiceProperty(source, CloudStorageCdpService.REMOTE_FS.name())
                .ifPresent(remoteFs -> serviceConfigs.add(config(CORE_DEFAULTFS, remoteFs.getValue())));
        StringBuilder safetyValveValueBuilder = new StringBuilder();
        safetyValveValueBuilder.append(getNameNodePrincipalSafetyValveProperty(source));
        safetyValveValueBuilder.append(getRemoteHdfsNameserviceSafetyValveProperties(source));
        serviceConfigs.add(config(CORE_SITE_SAFETY_VALVE, safetyValveValueBuilder.toString()));
        return serviceConfigs;
    }

    private String getNameNodePrincipalSafetyValveProperty(TemplatePreparationObject source) {
        return source.getTrustView()
                .map(TrustView::realm)
                .map(String::toUpperCase)
                .map(trustedRealm -> getSafetyValveProperty(DFS_NAMENODE_KERBEROS_PRINCIPAL, "hdfs/_HOST@" + trustedRealm))
                .orElse("");
    }

    private String getRemoteHdfsNameserviceSafetyValveProperties(TemplatePreparationObject source) {
        RdcView rdcView = source.getDatalakeView().get().getRdcView();
        String datalakeNameService = hdfsConfigHelper.getNameService(rdcView);
        if (StringUtils.isNotBlank(datalakeNameService)) {
            LOGGER.info("Datalake nameservice for HDFS is {}", datalakeNameService);
            return new StringBuilder()
                    .append(getSafetyValveProperty("dfs.nameservices", datalakeNameService))
                    .append(hdfsConfigHelper.getNameServiceConfigSafetyValveValue(rdcView))
                    .toString();
        } else {
            LOGGER.info("Datalake does not have nameservice for HDFS");
            return "";
        }
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return cmTemplateProcessor.isHybridDatahub(source);
    }
}

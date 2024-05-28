package com.sequenceiq.cloudbreak.cmtemplate.configproviders.core;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.CORE_DEFAULTFS;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.CORE_SETTINGS;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.CORE_SITE_SAFETY_VALVE;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.DEFAULT_BACKUP;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.HADOOP_SECURITY_GROUPS_CACHE_BACKGROUND_RELOAD;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.STORAGEOPERATIONS;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsRoles.HDFS;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsRoles.NAMENODE;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.adls.AdlsGen2ConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.s3.S3ConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class CDLCoreConfigProvider extends CoreConfigProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(CDLCoreConfigProvider.class);

    @Inject
    private S3ConfigProvider s3ConfigProvider;

    @Inject
    private AdlsGen2ConfigProvider adlsConfigProvider;

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> apiClusterTemplateConfigs = new ArrayList<>();
        Optional<ApiClusterTemplateConfig> roleConfig = templateProcessor.getRoleConfig(CORE_SETTINGS, STORAGEOPERATIONS, CORE_DEFAULTFS);
        if (roleConfig.isEmpty()) {
            ConfigUtils.getStorageLocationForServiceProperty(source, DEFAULT_BACKUP)
                    .ifPresent(location -> apiClusterTemplateConfigs.add(config(CORE_DEFAULTFS, location.getValue())));
        }

        StringBuilder hdfsCoreSiteSafetyValveValue = new StringBuilder();
        s3ConfigProvider.getServiceConfigs(source, hdfsCoreSiteSafetyValveValue);
        adlsConfigProvider.populateServiceConfigs(source, hdfsCoreSiteSafetyValveValue, templateProcessor.getStackVersion());

        hdfsCoreSiteSafetyValveValue.append(ConfigUtils.getSafetyValveProperty(HADOOP_SECURITY_GROUPS_CACHE_BACKGROUND_RELOAD, "true"));
        if (!hdfsCoreSiteSafetyValveValue.toString().isEmpty()) {
            LOGGER.info("Adding '{}' to the cluster template config.", CORE_SITE_SAFETY_VALVE);
            apiClusterTemplateConfigs.add(config(CORE_SITE_SAFETY_VALVE, hdfsCoreSiteSafetyValveValue.toString()));
        }

        return apiClusterTemplateConfigs;
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return source.getDatalakeView().map(view ->  Crn.ResourceType.INSTANCE.equals(Crn.safeFromString(view.getCrn()).getResourceType())).orElse(false)
                && !cmTemplateProcessor.isRoleTypePresentInService(HDFS, Lists.newArrayList(NAMENODE))
                && source.getFileSystemConfigurationView().isPresent();
    }
}

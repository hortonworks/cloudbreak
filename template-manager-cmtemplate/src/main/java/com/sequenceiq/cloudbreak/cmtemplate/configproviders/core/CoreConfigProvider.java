package com.sequenceiq.cloudbreak.cmtemplate.configproviders.core;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionOlderThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.CORE_DEFAULTFS;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.CORE_SETTINGS;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.CORE_SETTINGS_REF_NAME;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.CORE_SETTINGS_SERVICE_REF_NAME;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.CORE_SITE_SAFETY_VALVE;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.HADOOP_RPC_PROTECTION;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.HADOOP_SECURITY_GROUPS_CACHE_BACKGROUND_RELOAD;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.STORAGEOPERATIONS;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsRoles.HDFS;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsRoles.NAMENODE;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.adls.AdlsGen2ConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.s3.S3ConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.common.api.type.InstanceGroupType;

@Component
public class CoreConfigProvider extends AbstractRoleConfigProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoreConfigProvider.class);

    @Inject
    private S3ConfigProvider s3ConfigProvider;

    @Inject
    private AdlsGen2ConfigProvider adlsConfigProvider;

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        return List.of();
    }

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> apiClusterTemplateConfigs = new ArrayList<>();
        Optional<ApiClusterTemplateConfig> roleConfig = templateProcessor.getRoleConfig(CORE_SETTINGS, STORAGEOPERATIONS, CORE_DEFAULTFS);
        if (roleConfig.isEmpty()) {
            ConfigUtils.getStorageLocationForServiceProperty(source, CORE_DEFAULTFS)
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

        if (source.getGeneralClusterConfigs().isGovCloud()) {
            apiClusterTemplateConfigs.add(config(HADOOP_RPC_PROTECTION, "privacy"));
        }

        return apiClusterTemplateConfigs;
    }

    @Override
    public Map<String, ApiClusterTemplateService> getAdditionalServices(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        if (isConfigurationNeeded(cmTemplateProcessor, source) && cmTemplateProcessor.getServiceByType(CORE_SETTINGS).isEmpty()) {
            LOGGER.info("Adding '{}' as additional service.", CORE_SETTINGS);
            ApiClusterTemplateService coreSettings = createBaseCoreSettingsService(cmTemplateProcessor);
            Set<HostgroupView> hostgroupViews = source.getHostgroupViews();
            return hostgroupViews.stream()
                    .filter(filterByHostGroupViewType())
                    .collect(Collectors.toMap(HostgroupView::getName, v -> coreSettings));
        }
        return Map.of();
    }

    public Predicate<HostgroupView> filterByHostGroupViewType() {
        return hg -> InstanceGroupType.GATEWAY.equals(hg.getInstanceGroupType());
    }

    private ApiClusterTemplateService createBaseCoreSettingsService(CmTemplateProcessor cmTemplateProcessor) {
        ApiClusterTemplateService coreSettings = new ApiClusterTemplateService()
                .serviceType(CORE_SETTINGS)
                .refName(CORE_SETTINGS_SERVICE_REF_NAME);
        if (needToAddStorageOperationsRole(cmTemplateProcessor)) {
            LOGGER.info("CM version is older then 7.7.1, adding '{}' role to '{}' service.", STORAGEOPERATIONS, CORE_SETTINGS);
            ApiClusterTemplateRoleConfigGroup coreSettingsRole = new ApiClusterTemplateRoleConfigGroup()
                    .roleType(STORAGEOPERATIONS)
                    .base(true)
                    .refName(CORE_SETTINGS_REF_NAME);
            coreSettings.roleConfigGroups(List.of(coreSettingsRole));
        } else {
            coreSettings.roleConfigGroups(new LinkedList<>());
        }
        return coreSettings;
    }

    private boolean needToAddStorageOperationsRole(CmTemplateProcessor cmTemplateProcessor) {
        return cmTemplateProcessor.getCmVersion().isPresent()
                && isVersionOlderThanLimited(cmTemplateProcessor.getCmVersion().get(), CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_7_1);
    }

    @Override
    public String getServiceType() {
        return CORE_SETTINGS;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(STORAGEOPERATIONS);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return !cmTemplateProcessor.isRoleTypePresentInService(HDFS, Lists.newArrayList(NAMENODE))
                && source.getFileSystemConfigurationView().isPresent()
                && ConfigUtils.getStorageLocationForServiceProperty(source, CORE_DEFAULTFS).isPresent();
    }
}

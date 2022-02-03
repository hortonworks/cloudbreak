package com.sequenceiq.cloudbreak.cmtemplate.configproviders.core;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.CORE_SETTINGS;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.CORE_SETTINGS_REF_NAME;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.CORE_SETTINGS_SERVICE_REF_NAME;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.STORAGEOPERATIONS;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsRoles.HDFS;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsRoles.NAMENODE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.s3.S3ConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.common.api.type.InstanceGroupType;

@Component
public class CoreConfigProvider extends AbstractRoleConfigProvider {

    public static final String CORE_DEFAULTFS = "core_defaultfs";

    private static final String CORE_SITE_SAFETY_VALVE = "core_site_safety_valve";

    private static final String HADOOP_SECURITY_GROUPS_CACHE_BACKGROUND_RELOAD = "hadoop.security.groups.cache.background.reload";

    @Inject
    private S3ConfigProvider s3ConfigProvider;

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
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
        hdfsCoreSiteSafetyValveValue.append(ConfigUtils.getSafetyValveProperty(HADOOP_SECURITY_GROUPS_CACHE_BACKGROUND_RELOAD, "true"));

        if (!hdfsCoreSiteSafetyValveValue.toString().isEmpty()) {
            apiClusterTemplateConfigs.add(config(CORE_SITE_SAFETY_VALVE, hdfsCoreSiteSafetyValveValue.toString()));
        }

        return apiClusterTemplateConfigs;
    }

    @Override
    public Map<String, ApiClusterTemplateService> getAdditionalServices(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        if (isConfigurationNeeded(cmTemplateProcessor, source)
                && cmTemplateProcessor.getServiceByType(CORE_SETTINGS).isEmpty()) {
            ApiClusterTemplateService coreSettings = createBaseCoreSettingsService();
            Set<HostgroupView> hostgroupViews = source.getHostgroupViews();
            return hostgroupViews.stream()
                    .filter(hg -> InstanceGroupType.GATEWAY.equals(hg.getInstanceGroupType()))
                    .collect(Collectors.toMap(HostgroupView::getName, v -> coreSettings));
        }
        return Map.of();
    }

    private ApiClusterTemplateService createBaseCoreSettingsService() {
        ApiClusterTemplateService coreSettings = new ApiClusterTemplateService()
                .serviceType(CORE_SETTINGS)
                .refName(CORE_SETTINGS_SERVICE_REF_NAME);
        ApiClusterTemplateRoleConfigGroup coreSettingsRole = new ApiClusterTemplateRoleConfigGroup()
                .roleType(STORAGEOPERATIONS)
                .base(true)
                .refName(CORE_SETTINGS_REF_NAME);
        coreSettings.roleConfigGroups(List.of(coreSettingsRole));
        return coreSettings;
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

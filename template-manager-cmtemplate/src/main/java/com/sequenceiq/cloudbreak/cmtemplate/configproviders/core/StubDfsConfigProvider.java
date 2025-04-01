package com.sequenceiq.cloudbreak.cmtemplate.configproviders.core;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.CORE_DEFAULTFS;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.STORAGEOPERATIONS;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.STUB_DFS;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.STUB_DFS_SERVICE_REF_NAME;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.STUB_DFS_SERVICE_ROLE_STORAGEOPERATIONS_REF_NAME;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsRoles.HDFS;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsRoles.NAMENODE;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.common.api.type.InstanceGroupType;

@Component
public class StubDfsConfigProvider extends AbstractRoleConfigProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(StubDfsConfigProvider.class);

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        return List.of();
    }

    @Override
    public Map<String, ApiClusterTemplateService> getAdditionalServices(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        if (isConfigurationNeeded(cmTemplateProcessor, source)
                && cmTemplateProcessor.getServiceByType(STUB_DFS).isEmpty()) {
            LOGGER.info("'{}' is not part of the template so adding it as an additional service with '{}' role.", STUB_DFS, STORAGEOPERATIONS);
            ApiClusterTemplateService coreSettings = stubDfsSettings();
            Set<HostgroupView> hostgroupViews = source.getHostgroupViews();
            return hostgroupViews.stream()
                    .filter(hg -> InstanceGroupType.GATEWAY.equals(hg.getInstanceGroupType()))
                    .collect(Collectors.toMap(HostgroupView::getName, v -> coreSettings));
        }
        return Map.of();
    }

    private ApiClusterTemplateService stubDfsSettings() {
        ApiClusterTemplateService coreSettings = new ApiClusterTemplateService()
                .serviceType(STUB_DFS)
                .refName(STUB_DFS_SERVICE_REF_NAME);
        ApiClusterTemplateRoleConfigGroup coreSettingsRole = new ApiClusterTemplateRoleConfigGroup()
                .roleType(STORAGEOPERATIONS)
                .base(true)
                .refName(STUB_DFS_SERVICE_ROLE_STORAGEOPERATIONS_REF_NAME);
        coreSettings.roleConfigGroups(List.of(coreSettingsRole));
        return coreSettings;
    }

    @Override
    public String getServiceType() {
        return STUB_DFS;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(STORAGEOPERATIONS);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return cmTemplateProcessor.getCmVersion().isPresent()
                && isVersionNewerOrEqualThanLimited(cmTemplateProcessor.getCmVersion().get(), CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_7_1)
                && !cmTemplateProcessor.isRoleTypePresentInService(HDFS, Lists.newArrayList(NAMENODE))
                && source.getFileSystemConfigurationView().isPresent()
                && ConfigUtils.getStorageLocationForServiceProperty(source, CORE_DEFAULTFS).isPresent();
    }
}

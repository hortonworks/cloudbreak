package com.sequenceiq.cloudbreak.cmtemplate.configproviders.raz;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.raz.RangerRazRoles.RANGER_RAZ;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.raz.RangerRazRoles.RANGER_RAZ_SERVER;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.common.api.type.InstanceGroupType;

/**
 * Enables the Ranger Raz service.
 */
@Component
public class RangerRazConfigProvider extends AbstractRoleConfigProvider {

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        return List.of();
    }

    @Override
    public Map<String, ApiClusterTemplateService> getAdditionalServices(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        if (isConfigurationNeeded(cmTemplateProcessor, source)) {
            ApiClusterTemplateService coreSettings = createTemplate();
            Set<HostgroupView> hostgroupViews = source.getHostgroupViews();
            return hostgroupViews.stream()
                    .filter(hg -> InstanceGroupType.GATEWAY.equals(hg.getInstanceGroupType()))
                    .collect(Collectors.toMap(HostgroupView::getName, v -> coreSettings));
        }
        return Map.of();
    }

    @Override
    public String getServiceType() {
        return RANGER_RAZ;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(RANGER_RAZ_SERVER);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return StackType.DATALAKE == source.getStackType()
                && CloudPlatform.AZURE == source.getCloudPlatform()
                && CMRepositoryVersionUtil.isRazConfigurationSupported(source.getProductDetailsView().getCm())
                && source.getGeneralClusterConfigs().isEnableRangerRaz();
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        return List.of();
    }

    private ApiClusterTemplateService createTemplate() {
        ApiClusterTemplateService coreSettings = new ApiClusterTemplateService()
                .serviceType(RANGER_RAZ)
                .refName("ranger-RANGER_RAZ");
        ApiClusterTemplateRoleConfigGroup coreSettingsRole = new ApiClusterTemplateRoleConfigGroup()
                .roleType(RANGER_RAZ_SERVER)
                .base(true)
                .refName("ranger-RANGER_RAZ_SERVER");
        coreSettings.roleConfigGroups(List.of(coreSettingsRole));
        return coreSettings;
    }
}

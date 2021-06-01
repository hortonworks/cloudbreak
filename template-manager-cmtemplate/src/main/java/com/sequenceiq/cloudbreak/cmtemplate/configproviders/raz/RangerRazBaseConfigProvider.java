package com.sequenceiq.cloudbreak.cmtemplate.configproviders.raz;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isRazTokenConfigurationSupported;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.getSafetyValveProperty;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.raz.RangerRazRoles.RANGER_RAZ;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.raz.RangerRazRoles.RANGER_RAZ_SERVER;

import java.util.List;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

public abstract class RangerRazBaseConfigProvider extends AbstractRoleConfigProvider {

    private static final String RANGER_RAZ_SITE_XML_ROLE_SAFETY_VALVE = "ranger-raz-conf/ranger-raz-site.xml_role_safety_valve";

    private static final String RANGER_RAZ_BOOTSTRAP_SERVICETYPES = "ranger.raz.bootstrap.servicetypes";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        return List.of();
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
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        String cdhVersion = source.getBlueprintView().getProcessor().getVersion().orElse("");
        CloudPlatform cloudPlatform = source.getCloudPlatform();
        if (!Strings.isNullOrEmpty(cdhVersion) && isRazTokenConfigurationSupported(cdhVersion, cloudPlatform)) {
            String safetyValveValue = getSafetyValveProperty(RANGER_RAZ_BOOTSTRAP_SERVICETYPES, getServiceType(cloudPlatform));
            return List.of(config(RANGER_RAZ_SITE_XML_ROLE_SAFETY_VALVE, safetyValveValue));
        }
        return List.of();
    }

    private String getServiceType(CloudPlatform cloudPlatform) {
        switch (cloudPlatform) {
            case AZURE:
                return "adls";
            case AWS:
                return "s3";
            default:
                return null;
        }
    }

    protected ApiClusterTemplateService createTemplate() {
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

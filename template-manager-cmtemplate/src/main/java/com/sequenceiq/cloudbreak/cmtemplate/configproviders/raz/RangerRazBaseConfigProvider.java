package com.sequenceiq.cloudbreak.cmtemplate.configproviders.raz;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.raz.RangerRazRoles.RANGER_RAZ;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.raz.RangerRazRoles.RANGER_RAZ_SERVER;

import java.util.List;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

public abstract class RangerRazBaseConfigProvider extends AbstractRoleConfigProvider {

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
        if () {
            if (CloudPlatform.AZURE == source.getCloudPlatform()) {
                return List.of(
                        config("ranger.raz.processor.adls", "org.apache.ranger.raz.processor.adls.AdlsGen2RazProcessor"),
                        config("ranger.raz.bootstrap.servicetypes", "adls")
                );
            } else if (CloudPlatform.AWS == source.getCloudPlatform()) {
                return List.of(
                        config("ranger.raz.processor.s3", "org.apache.ranger.raz.processor.s3.S3RazProcessor"),
                        config("ranger.raz.bootstrap.servicetypes", "s3"),
                        config("ranger.raz.plugin.s3", "org.apache.ranger.raz.processor.s3.RangerS3BasePlugin")
                );
            }
        }
        return List.of();
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

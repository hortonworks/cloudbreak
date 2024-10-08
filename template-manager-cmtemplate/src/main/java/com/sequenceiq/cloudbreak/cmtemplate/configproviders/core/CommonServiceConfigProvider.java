package com.sequenceiq.cloudbreak.cmtemplate.configproviders.core;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.ENVIRONMENT_ACCOUNT_ID;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.ENVIRONMENT_CLOUD_PROVIDER;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.ENVIRONMENT_CRN;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.RESOURCE_CRN;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Component
public class CommonServiceConfigProvider extends CoreConfigProvider {

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return true;
    }

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> apiClusterTemplateConfigs = new ArrayList<>();
        if (templateProcessor.getCmVersion().isPresent()
                && isVersionNewerOrEqualThanLimited(templateProcessor.getCmVersion().get(), CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_12_0_500)) {
            CloudPlatform cloudPlatform = source.getCloudPlatform();
            apiClusterTemplateConfigs.add(config(ENVIRONMENT_CRN, source.getGeneralClusterConfigs().getEnvironmentCrn()));
            apiClusterTemplateConfigs.add(config(RESOURCE_CRN, source.getGeneralClusterConfigs().getResourceCrn()));
            apiClusterTemplateConfigs.add(config(ENVIRONMENT_ACCOUNT_ID, source.getGeneralClusterConfigs().getAccountId().orElse("UNKNOWN")));
            apiClusterTemplateConfigs.add(config(ENVIRONMENT_CLOUD_PROVIDER, cloudPlatform == null ? null : cloudPlatform.name()));
        }
        return apiClusterTemplateConfigs;
    }

    @Override
    public Predicate<HostgroupView> filterByHostGroupViewType() {
        return hgv -> true;
    }
}

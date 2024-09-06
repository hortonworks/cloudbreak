package com.sequenceiq.cloudbreak.cmtemplate.configproviders.dlm;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class DLMServiceConfigProvider implements CmTemplateComponentConfigProvider {

    public static final String DLM_DATAHUB_RESOURCE_CRN = "datahub.resource.crn";

    public static final String DLM_DATAHUB_ENVIRONMENT_CRN = "datahub.environment.crn";

    public static final String DLM_ACCOUNT_ID = "environment.accountId";

    public static final String DLM_CLOUD_PROVIDER = "environment.cloudProvider";

    @Override
    public String getServiceType() {
        return DLMServiceRoles.DLM_SERVICE;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(DLMServiceRoles.DLM_SERVER);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return true;
    }

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> dlmConfigs = new ArrayList<>();
        CloudPlatform cloudPlatform = source.getCloudPlatform();
        dlmConfigs.add(config(DLM_DATAHUB_ENVIRONMENT_CRN, source.getGeneralClusterConfigs().getEnvironmentCrn()));
        dlmConfigs.add(config(DLM_DATAHUB_RESOURCE_CRN, source.getGeneralClusterConfigs().getResourceCrn()));
        dlmConfigs.add(config(DLM_ACCOUNT_ID, source.getGeneralClusterConfigs().getAccountId().orElse("UNKNOWN")));
        dlmConfigs.add(config(DLM_CLOUD_PROVIDER, cloudPlatform == null ? null : cloudPlatform.name()));
        return dlmConfigs;
    }
}

package com.sequenceiq.cloudbreak.cmtemplate.configproviders.clo;

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
public class CLOServiceConfigProvider implements CmTemplateComponentConfigProvider {

    public static final String CLO_DATAHUB_RESOURCE_CRN = "datahub.resource.crn";

    public static final String CLO_DATAHUB_ENVIRONMENT_CRN = "datahub.environment.crn";

    public static final String CLO_ACCOUNT_ID = "environment.accountId";

    public static final String CLO_CLOUD_PROVIDER = "environment.cloudProvider";

    @Override
    public String getServiceType() {
        return CLOServiceRoles.CLO_SERVICE;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(CLOServiceRoles.CLO_SERVER);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return true;
    }

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> cloConfigs = new ArrayList<>();
        CloudPlatform cloudPlatform = source.getCloudPlatform();
        cloConfigs.add(config(CLO_DATAHUB_ENVIRONMENT_CRN, source.getGeneralClusterConfigs().getEnvironmentCrn()));
        cloConfigs.add(config(CLO_DATAHUB_RESOURCE_CRN, source.getGeneralClusterConfigs().getResourceCrn()));
        cloConfigs.add(config(CLO_ACCOUNT_ID, source.getGeneralClusterConfigs().getAccountId().orElse("UNKNOWN")));
        cloConfigs.add(config(CLO_CLOUD_PROVIDER, cloudPlatform == null ? null : cloudPlatform.name()));
        return cloConfigs;
    }
}

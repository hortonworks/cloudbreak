package com.sequenceiq.cloudbreak.cmtemplate.configproviders.core;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.ENVIRONMENT_ACCOUNT_ID;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.ENVIRONMENT_CLOUD_PROVIDER;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.ENVIRONMENT_CRN;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.RESOURCE_CRN;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Component
public class CommonCoreConfigProvider extends CoreConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonCoreConfigProvider.class);

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return true;
    }

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> apiClusterTemplateConfigs = new ArrayList<>();
        String cmGbn = templateProcessor.getCmVersion().orElse("");
        if (isVersionNewerOrEqualThanLimited(cmVersionFromGbn(cmGbn), CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_12_0_500)) {
            CloudPlatform cloudPlatform = source.getCloudPlatform();
            apiClusterTemplateConfigs.add(config(ENVIRONMENT_CRN, source.getGeneralClusterConfigs().getEnvironmentCrn()));
            apiClusterTemplateConfigs.add(config(RESOURCE_CRN, source.getGeneralClusterConfigs().getResourceCrn()));
            apiClusterTemplateConfigs.add(config(ENVIRONMENT_ACCOUNT_ID, source.getGeneralClusterConfigs().getAccountId().orElse("UNKNOWN")));
            apiClusterTemplateConfigs.add(config(ENVIRONMENT_CLOUD_PROVIDER, cloudPlatform == null ? null : cloudPlatform.name()));
        } else {
            LOGGER.info("Service Configuration not applicable for {} ", cmGbn);
        }
        return apiClusterTemplateConfigs;
    }

    /**
     * Need to get CM version (gbn) irrespective of it's build number, which is separated by '-'
     * for example, for "7.12.0.500-58279810" it should be "7.12.0.500"
     *
     * @param cmGbn the full CM version string, potentially containing a build number after a hyphen.
     * @return the CM version without the build number, or the original version string if no hyphen is found.
     */
    private String cmVersionFromGbn(String cmGbn) {
        return cmGbn.split("-")[0];
    }

    @Override
    public Predicate<HostgroupView> filterByHostGroupViewType() {
        return hgv -> true;
    }

    @Override
    public boolean isServiceConfigUpdateNeededForUpgrade(String fromCmVersion, String toCmVersion) {
        return isVersionNewerOrEqualThanLimited(cmVersionFromGbn(toCmVersion), CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_12_0_500);
    }

    @Override
    public Map<String, String> getUpdatedServiceConfigForUpgrade(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        CloudPlatform cloudPlatform = source.getCloudPlatform();
        Map<String, String> updatedConfigs = new HashMap<>();
        updatedConfigs.put(ENVIRONMENT_CRN, source.getGeneralClusterConfigs().getEnvironmentCrn());
        updatedConfigs.put(RESOURCE_CRN, source.getGeneralClusterConfigs().getResourceCrn());
        updatedConfigs.put(ENVIRONMENT_ACCOUNT_ID, source.getGeneralClusterConfigs().getAccountId().orElse("UNKNOWN"));
        updatedConfigs.put(ENVIRONMENT_CLOUD_PROVIDER, cloudPlatform == null ? null : cloudPlatform.name());
        return updatedConfigs;
    }
}

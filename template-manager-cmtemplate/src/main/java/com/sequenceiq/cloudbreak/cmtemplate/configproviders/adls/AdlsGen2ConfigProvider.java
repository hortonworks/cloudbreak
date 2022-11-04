package com.sequenceiq.cloudbreak.cmtemplate.configproviders.adls;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.common.model.FileSystemType.ADLS_GEN_2;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class AdlsGen2ConfigProvider {

    private static final String AZURE_READAHEAD_ENABLED = "fs.azure.enable.readahead";

    private static final String AZURE_READAHEAD_ENABLED_VALUE = "false";

    public void populateServiceConfigs(TemplatePreparationObject source, StringBuilder hdfsCoreSiteSafetyValveValue, String stackVersion) {
        if (adlsGen2FileSystemConfigured(source) && isVersionNewerOrEqualThanLimited(stackVersion, CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_12)) {
            hdfsCoreSiteSafetyValveValue.append(ConfigUtils.getSafetyValveProperty(AZURE_READAHEAD_ENABLED, AZURE_READAHEAD_ENABLED_VALUE));
        }
    }

    private boolean adlsGen2FileSystemConfigured(TemplatePreparationObject source) {
        return source.getFileSystemConfigurationView().map(fs -> ADLS_GEN_2.name().equals(fs.getType())).orElse(false);
    }
}

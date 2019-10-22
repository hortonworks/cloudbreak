package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveMetastoreCloudStorageServiceConfigProvider.HMS_METASTORE_EXTERNAL_DIR;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class HiveLLAPServiceConfigProvider implements CmTemplateComponentConfigProvider {

    private static final String HIVE_HOOK_PROTO_BASE_DIR_PARAM = "hive_hook_proto_base_directory";

    private static final String HIVE_HOOK_PROTO_BASE_DIR_SUFFIX = "/sys.db/query_data";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        return ConfigUtils.getStorageLocationForServiceProperty(source, HMS_METASTORE_EXTERNAL_DIR)
                .map(location -> location.getValue().replaceAll("/?$", "") + HIVE_HOOK_PROTO_BASE_DIR_SUFFIX)
                .map(logDir -> List.of(config(HIVE_HOOK_PROTO_BASE_DIR_PARAM, logDir)))
                .orElseGet(List::of);
    }

    @Override
    public String getServiceType() {
        return HiveRoles.HIVELLAP;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(HiveRoles.GATEWAY);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return source.getFileSystemConfigurationView().isPresent()
                && cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }
}
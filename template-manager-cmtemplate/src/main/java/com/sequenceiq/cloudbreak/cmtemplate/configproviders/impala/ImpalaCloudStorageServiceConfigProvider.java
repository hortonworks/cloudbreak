package com.sequenceiq.cloudbreak.cmtemplate.configproviders.impala;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_11;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveMetastoreCloudStorageServiceConfigProvider.HMS_METASTORE_DIR;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveMetastoreCloudStorageServiceConfigProvider.HMS_METASTORE_EXTERNAL_DIR;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ImpalaCloudStorageServiceConfigProvider implements CmTemplateComponentConfigProvider {

    private static final String IMPALA_CMD_ARGS_SAFETY_VALVE = "impala_cmd_args_safety_valve";

    private static final String IMPALA_STARTUP_FILESYSTEM_CHECK_DIRS = "--startup_filesystem_check_directories=";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor processor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> configs = new ArrayList<>();
        String cdhVersion = processor.getStackVersion();
        if (isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERA_STACK_VERSION_7_2_11)) {
            List<String> cloudPaths = new ArrayList<>();
            ConfigUtils.getStorageLocationForServiceProperty(source, HMS_METASTORE_DIR)
                    .ifPresent(location -> cloudPaths.add(location.getValue()));
            ConfigUtils.getStorageLocationForServiceProperty(source, HMS_METASTORE_EXTERNAL_DIR)
                    .ifPresent(location -> cloudPaths.add(location.getValue()));
            if (!cloudPaths.isEmpty()) {
                String hmsWarehousePaths = cloudPaths.stream().collect(Collectors.joining(","));
                configs.add(ConfigUtils.config(IMPALA_CMD_ARGS_SAFETY_VALVE, IMPALA_STARTUP_FILESYSTEM_CHECK_DIRS + hmsWarehousePaths));
            }
        }
        return configs;
    }

    @Override
    public String getServiceType() {
        return ImpalaRoles.SERVICE_IMPALA;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(ImpalaRoles.ROLE_IMPALAD);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return source.getFileSystemConfigurationView().isPresent() &&
                cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());

    }
}

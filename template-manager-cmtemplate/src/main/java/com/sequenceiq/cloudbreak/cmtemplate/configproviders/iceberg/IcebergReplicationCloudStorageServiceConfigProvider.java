package com.sequenceiq.cloudbreak.cmtemplate.configproviders.iceberg;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class IcebergReplicationCloudStorageServiceConfigProvider implements CmTemplateComponentConfigProvider {

    public static final String ICEBERG_REPLICATION_CLOUD_DATA_ROOT_DIR = "iceberg_replication_cloud_data_root_dir";

    public static final String ICEBERG_REPLICATION_CLOUD_DATA_ROOT = "iceberg_replication_cloud_data_root";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> serviceConfigs = new ArrayList<>();
        ConfigUtils.getStorageLocationForServiceProperty(source, ICEBERG_REPLICATION_CLOUD_DATA_ROOT_DIR)
                .ifPresent(location -> serviceConfigs.add(config(ICEBERG_REPLICATION_CLOUD_DATA_ROOT, location.getValue())));
        return serviceConfigs;
    }

    @Override
    public String getServiceType() {
        return IcebergRoles.ICEBERG_REPLICATION;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(IcebergRoles.ICEBERG_REPLICATION_ADMINSERVER);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return source.getFileSystemConfigurationView().isPresent()
                && cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }
}

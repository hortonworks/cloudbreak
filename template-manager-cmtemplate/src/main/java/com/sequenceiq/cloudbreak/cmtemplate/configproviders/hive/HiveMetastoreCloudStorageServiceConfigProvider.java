package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive;

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
public class HiveMetastoreCloudStorageServiceConfigProvider implements CmTemplateComponentConfigProvider {

    public static final String HIVE_SERVICE_CONFIG_SAFETY_VALVE = "hive_service_replication_config_safety_valve";

    public static final String HMS_METASTORE_DIR = "hive.metastore.warehouse.dir";

    public static final String HMS_METASTORE_EXTERNAL_DIR = "hive.metastore.warehouse.external.dir";

    public static final String HMS_METASTORE_REPLICA_DIR_TEMPLATE_LOCATION = "hive.repl.replica.functions.root.dir";

    public static final String HMS_METASTORE_REPLICA_DIR = "hive_repl_replica_functions_root_dir";

    public static final String HMS_METASTORE_DIR_TEMPLATE_PARAM = "hive_warehouse_directory";

    public static final String HMS_METASTORE_EXTERNAL_DIR_TEMPLATE_PARAM = "hive_warehouse_external_directory";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> hmsConfigs = new ArrayList<>(2);
        ConfigUtils.getStorageLocationForServiceProperty(source, HMS_METASTORE_DIR)
                .ifPresent(location -> hmsConfigs.add(config(HMS_METASTORE_DIR_TEMPLATE_PARAM, location.getValue())));

        ConfigUtils.getStorageLocationForServiceProperty(source, HMS_METASTORE_EXTERNAL_DIR)
                .ifPresent(location -> hmsConfigs.add(config(HMS_METASTORE_EXTERNAL_DIR_TEMPLATE_PARAM, location.getValue())));

        ConfigUtils.getStorageLocationForServiceProperty(source, HMS_METASTORE_REPLICA_DIR_TEMPLATE_LOCATION)
                .ifPresent(location -> hmsConfigs.add(config(HMS_METASTORE_REPLICA_DIR, location.getValue())));

        return hmsConfigs;
    }

    @Override
    public String getServiceType() {
        return HiveRoles.HIVE;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(HiveRoles.HIVEMETASTORE);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return source.getFileSystemConfigurationView().isPresent()
                && cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }
}
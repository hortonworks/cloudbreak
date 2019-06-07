package com.sequenceiq.cloudbreak.cmtemplate.configproviders.spark;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class SparkOnYarnCloudStorageServiceConfigProvider implements CmTemplateComponentConfigProvider {

    private static final String SPARK_ENV_CONFIG_SAFETY_VALVE = "spark-conf/spark-env.sh_service_safety_valve";

    private static final String SPARK_SQL_WAREHOUSE_DIR = "spark.sql.warehouse.dir";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(TemplatePreparationObject templatePreparationObject) {
        String cloudStorageProperty = getCloudStorageProperty(templatePreparationObject);
        if (!cloudStorageProperty.isEmpty()) {
            return List.of(config(SPARK_ENV_CONFIG_SAFETY_VALVE, cloudStorageProperty));
        }
        return List.of();
    }

    @Override
    public String getServiceType() {
        return SparkOnYarnRoles.SPARK_ON_YARN;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(SparkOnYarnRoles.GATEWAY, SparkOnYarnRoles.SPARK_YARN_HISTORY_SERVER);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return source.getFileSystemConfigurationView().isPresent()
                && cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }

    protected String getCloudStorageProperty(TemplatePreparationObject source) {
        StringBuilder sparkCloudStorage = new StringBuilder();
        ConfigUtils.getStorageLocationForServiceProperty(source, SPARK_SQL_WAREHOUSE_DIR).ifPresent(
                storageLocation -> sparkCloudStorage
                        .append(SPARK_SQL_WAREHOUSE_DIR)
                        .append("=")
                        .append(storageLocation.getValue()));
        return sparkCloudStorage.toString();
    }
}
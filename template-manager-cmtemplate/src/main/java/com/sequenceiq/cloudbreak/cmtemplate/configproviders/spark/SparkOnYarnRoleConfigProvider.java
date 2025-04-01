package com.sequenceiq.cloudbreak.cmtemplate.configproviders.spark;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveMetastoreCloudStorageServiceConfigProvider.HMS_METASTORE_EXTERNAL_DIR;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;

@Component
public class SparkOnYarnRoleConfigProvider extends AbstractRoleConfigProvider {

    private static final String SPARK_CONF_CLIENT_SAFETY_VALVE = "spark-conf/spark-defaults.conf_client_config_safety_valve";

    private static final String SPARK_YARN_ACCESS_DIR_PARAM = "spark.yarn.access.hadoopFileSystems=";

    private static final String SPARK_HADOOP_S3_SSL_CHANNEL_MODE = "spark.hadoop.fs.s3a.ssl.channel.mode=openssl";

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        switch (roleType) {
            case SparkRoles.GATEWAY:
                StringBuilder confClientConfigSafetyValveValue = new StringBuilder();
                Optional<StorageLocationView> storageLocationView = ConfigUtils.getStorageLocationForServiceProperty(source, HMS_METASTORE_EXTERNAL_DIR);
                if (storageLocationView.isPresent()) {
                    confClientConfigSafetyValveValue.append(SPARK_YARN_ACCESS_DIR_PARAM +
                            ConfigUtils.getBasePathFromStorageLocation(storageLocationView.get().getValue()));
                }
                if (CMRepositoryVersionUtil.isS3SslChannelModeSupported(ConfigUtils.getCdhVersion(source),
                        source.getCloudPlatform(), source.getPlatformVariant())) {
                    if (!confClientConfigSafetyValveValue.toString().isEmpty()) {
                        confClientConfigSafetyValveValue.append('\n');
                    }
                    confClientConfigSafetyValveValue.append(SPARK_HADOOP_S3_SSL_CHANNEL_MODE);
                }
                if (!confClientConfigSafetyValveValue.toString().isEmpty()) {
                    return List.of(config(SPARK_CONF_CLIENT_SAFETY_VALVE, confClientConfigSafetyValveValue.toString()));
                }
                return List.of();
            default:
                return List.of();
        }
    }

    @Override
    public String getServiceType() {
        return SparkRoles.SPARK_ON_YARN;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(SparkRoles.GATEWAY);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return source.getFileSystemConfigurationView().isPresent()
                && cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }
}

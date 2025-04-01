package com.sequenceiq.cloudbreak.cmtemplate.configproviders.spark;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveMetastoreCloudStorageServiceConfigProvider.HMS_METASTORE_EXTERNAL_DIR;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsConfigHelper;
import com.sequenceiq.cloudbreak.domain.BlueprintHybridOption;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class Spark3OnYarnRoleConfigProvider extends AbstractRoleConfigProvider {

    private static final String SPARK3_HISTORY_LOG_DIR = "spark_history_log_dir";

    private static final String SPARK3_HISTORY_PATH = "/user/spark/spark3ApplicationHistory";

    private static final String SPARK3_CONF_CLIENT_SAFETY_VALVE = "spark3-conf/spark-defaults.conf_client_config_safety_valve";

    private static final String SPARK3_HADOOP_S3_SSL_CHANNEL_MODE = "spark.hadoop.fs.s3a.ssl.channel.mode=openssl";

    private static final String SPARK3_KERBEROS_FILESYSTEMS_CONFIG = "spark_kerberos_access_hadoopfilesystems";

    private static final String SPARK3_KERBEROS_FILESYSTEMS_SAFETY_VALVE = "spark.kerberos.access.hadoopFileSystems=";

    @Inject
    private HdfsConfigHelper hdfsConfigHelper;

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> serviceConfigs = new ArrayList<>();
        if (StackType.WORKLOAD.equals(source.getStackType()) && BlueprintHybridOption.BURST_TO_CLOUD.equals(source.getBlueprintView().getHybridOption())) {
            // set absolute path for history server to allow the customer to modify the defaultFs without moving spark history from local HDFS
            Optional<String> hdfsUrl = hdfsConfigHelper.getHdfsUrl(templateProcessor, source);
            if (hdfsUrl.isPresent()) {
                String historyLogDir = hdfsUrl.get() + SPARK3_HISTORY_PATH;
                serviceConfigs.add(config(SPARK3_HISTORY_LOG_DIR, historyLogDir));
            }
        }
        return serviceConfigs;
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> roleConfigs = new ArrayList<>();
        switch (roleType) {
            case SparkRoles.GATEWAY:
                StringBuilder confClientConfigSafetyValveValue = new StringBuilder();
                getHadoopFileSystems(templateProcessor, source).ifPresent(hadoopFileSystems -> {
                    if (isVersionNewerOrEqualThanLimited(ConfigUtils.getCmVersion(source), CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_12_0_400)) {
                        roleConfigs.add(config(SPARK3_KERBEROS_FILESYSTEMS_CONFIG, hadoopFileSystems));
                    } else {
                        confClientConfigSafetyValveValue.append(SPARK3_KERBEROS_FILESYSTEMS_SAFETY_VALVE).append(hadoopFileSystems);
                    }
                });
                if (CMRepositoryVersionUtil.isS3SslChannelModeSupported(ConfigUtils.getCdhVersion(source),
                        source.getCloudPlatform(), source.getPlatformVariant())) {
                    if (!confClientConfigSafetyValveValue.isEmpty()) {
                        confClientConfigSafetyValveValue.append('\n');
                    }
                    confClientConfigSafetyValveValue.append(SPARK3_HADOOP_S3_SSL_CHANNEL_MODE);
                }
                if (!confClientConfigSafetyValveValue.isEmpty()) {
                    roleConfigs.add(config(SPARK3_CONF_CLIENT_SAFETY_VALVE, confClientConfigSafetyValveValue.toString()));
                }
                break;
            default:
                break;
        }
        return roleConfigs;
    }

    private Optional<String> getHadoopFileSystems(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        Optional<String> baseStorageLocation = getBaseStorageLocation(source);
        if (baseStorageLocation.isPresent()) {
            StringBuilder hadoopFileSystems = new StringBuilder();
            hadoopFileSystems.append(baseStorageLocation.get());
            hdfsConfigHelper.getAttachedDatalakeHdfsUrlForHybridDatahub(templateProcessor, source)
                    .ifPresent(dlHdfs -> hadoopFileSystems.append(',').append(dlHdfs));
            return Optional.of(hadoopFileSystems.toString());
        }
        return Optional.empty();
    }

    private Optional<String> getBaseStorageLocation(TemplatePreparationObject source) {
        return ConfigUtils.getStorageLocationForServiceProperty(source, HMS_METASTORE_EXTERNAL_DIR)
                .map(storageLocationView -> ConfigUtils.getBasePathFromStorageLocation(storageLocationView.getValue()));
    }

    @Override
    public String getServiceType() {
        return SparkRoles.SPARK3_ON_YARN;
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

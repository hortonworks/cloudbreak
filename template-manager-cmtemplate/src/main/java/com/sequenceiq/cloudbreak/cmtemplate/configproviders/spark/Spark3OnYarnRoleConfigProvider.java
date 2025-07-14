package com.sequenceiq.cloudbreak.cmtemplate.configproviders.spark;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class Spark3OnYarnRoleConfigProvider extends AbstractRoleConfigProvider {

    private static final String SPARK3_CONF_CLIENT_SAFETY_VALVE = "spark3-conf/spark-defaults.conf_client_config_safety_valve";

    private static final String SPARK3_HADOOP_S3_SSL_CHANNEL_MODE = "spark.hadoop.fs.s3a.ssl.channel.mode=openssl";

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> roleConfigs = new ArrayList<>();
        switch (roleType) {
            case SparkRoles.GATEWAY:
                StringBuilder confClientConfigSafetyValveValue = new StringBuilder();
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

package com.sequenceiq.cloudbreak.cmtemplate.configproviders.spark;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsConfigHelper;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class Spark3OnYarnHybridConfigProvider extends AbstractRoleConfigProvider {

    protected static final String SPARK3_HISTORY_LOG_DIR = "spark_history_log_dir";

    protected static final String SPARK3_HISTORY_PATH = "/user/spark/spark3ApplicationHistory";

    protected static final String SPARK3_KERBEROS_FILESYSTEMS_CONFIG = "spark_kerberos_access_hadoopfilesystems";

    @Inject
    private HdfsConfigHelper hdfsConfigHelper;

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> serviceConfigs = new ArrayList<>();
        hdfsConfigHelper.getHdfsUrl(templateProcessor, source).ifPresent(dhHdfs -> {
            // set absolute path for history server to allow the customer to modify the defaultFs without moving spark history from local HDFS
            String historyLogDir = dhHdfs + SPARK3_HISTORY_PATH;
            serviceConfigs.add(config(SPARK3_HISTORY_LOG_DIR, historyLogDir));
        });
        return serviceConfigs;
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> roleConfigs = new ArrayList<>();
        switch (roleType) {
            case SparkRoles.GATEWAY:
                StringBuilder confClientConfigSafetyValveValue = new StringBuilder();
                hdfsConfigHelper.getAttachedDatalakeHdfsUrlForHybridDatahub(templateProcessor, source).ifPresent(dlHdfs -> {
                    roleConfigs.add(config(SPARK3_KERBEROS_FILESYSTEMS_CONFIG, dlHdfs));
                });
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
        return cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes())
                && cmTemplateProcessor.isHybridDatahub(source);
    }
}

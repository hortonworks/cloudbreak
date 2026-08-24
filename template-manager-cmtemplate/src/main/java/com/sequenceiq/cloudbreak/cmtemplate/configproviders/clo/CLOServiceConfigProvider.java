package com.sequenceiq.cloudbreak.cmtemplate.configproviders.clo;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_13_2_20000;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.getRdsViewOfType;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@Component
public class CLOServiceConfigProvider implements CmTemplateComponentConfigProvider {

    public static final String CLO_DATAHUB_RESOURCE_CRN = "datahub.resource.crn";

    public static final String CLO_DATAHUB_ENVIRONMENT_CRN = "datahub.environment.crn";

    public static final String CLO_ACCOUNT_ID = "environment.accountId";

    public static final String CLO_CLOUD_PROVIDER = "environment.cloudProvider";

    public static final String CLO_DB_TYPE = "dlm_db_type";

    public static final String CLO_DB_HOST = "dlm_db_host";

    public static final String CLO_DB_PORT = "dlm_db_port";

    public static final String CLO_DB_NAME = "dlm_db_name";

    public static final String CLO_DB_JDBC_URL_OVERRIDE = "dlm_db_jdbc_url_override";

    public static final String CLO_DB_USER = "dlm_db_user";

    public static final String CLO_DB_PASSWORD = "dlm_db_password";

    public static final String CLO_HA_ENABLED = "dlm_ha_enabled";

    @Override
    public String getServiceType() {
        return CLOServiceRoles.CLO_SERVICE;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(CLOServiceRoles.CLO_SERVER);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return true;
    }

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> cloConfigs = new ArrayList<>();
        CloudPlatform cloudPlatform = source.getCloudPlatform();
        cloConfigs.add(config(CLO_DATAHUB_ENVIRONMENT_CRN, source.getGeneralClusterConfigs().getEnvironmentCrn()));
        cloConfigs.add(config(CLO_DATAHUB_RESOURCE_CRN, source.getGeneralClusterConfigs().getResourceCrn()));
        cloConfigs.add(config(CLO_ACCOUNT_ID, source.getGeneralClusterConfigs().getAccountId().orElse("UNKNOWN")));
        cloConfigs.add(config(CLO_CLOUD_PROVIDER, cloudPlatform == null ? null : cloudPlatform.name()));

        String cmVersion = ConfigUtils.getCmVersion(source);
        if (StringUtils.isNotEmpty(cmVersion)
                && isVersionNewerOrEqualThanLimited(cmVersion, CLOUDERAMANAGER_VERSION_7_13_2_20000)) {
            RdsView lakehouseOptimizerRds = getRdsView(source);
            if (lakehouseOptimizerRds != null) {
                cloConfigs.add(config(CLO_DB_TYPE, "postgresql"));
                cloConfigs.add(config(CLO_DB_HOST, lakehouseOptimizerRds.getHost()));
                String dbPort = lakehouseOptimizerRds.getPort();
                cloConfigs.add(config(CLO_DB_PORT, StringUtils.isNotEmpty(dbPort) ? dbPort : "5432"));
                cloConfigs.add(config(CLO_DB_NAME, lakehouseOptimizerRds.getDatabaseName()));
                cloConfigs.add(config(CLO_DB_JDBC_URL_OVERRIDE, lakehouseOptimizerRds.getConnectionURL()));
                cloConfigs.add(config(CLO_DB_USER, lakehouseOptimizerRds.getConnectionUserName()));
                cloConfigs.add(config(CLO_DB_PASSWORD, lakehouseOptimizerRds.getConnectionPassword()));
            }
            if (isCloHaEnabled(templateProcessor)) {
                cloConfigs.add(config(CLO_HA_ENABLED, Boolean.TRUE.toString()));
            }
        }
        return cloConfigs;
    }

    protected RdsView getRdsView(TemplatePreparationObject source) {
        RdsView rdsViewOfType = getRdsViewOfType(DatabaseType.LAKEHOUSE_OPTIMIZER, source);
        if (rdsViewOfType != null) {
            rdsViewOfType.setSslCertificateFilePath(source.getRdsSslCertificateFilePath());
        }
        return rdsViewOfType;
    }

    private boolean isCloHaEnabled(CmTemplateProcessor templateProcessor) {
        return templateProcessor.getComponentsByHostGroup().values().stream()
                .filter(components -> components.contains(CLOServiceRoles.CLO_SERVER))
                .count() > 1;
    }
}

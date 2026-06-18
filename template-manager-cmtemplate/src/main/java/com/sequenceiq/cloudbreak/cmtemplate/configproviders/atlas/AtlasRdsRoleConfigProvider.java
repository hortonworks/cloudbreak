package com.sequenceiq.cloudbreak.cmtemplate.configproviders.atlas;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.getSafetyValveProperty;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRdsRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hbase.HbaseRoles;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsRoles;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@Component
public class AtlasRdsRoleConfigProvider extends AbstractRdsRoleConfigProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(AtlasRdsRoleConfigProvider.class);

    private static final String ATLAS_SERVER_ROLE_TYPE = "ATLAS_SERVER";

    private static final String ATLAS_SAFETY_VALVE_CONFIG_KEY = "atlas_config_safety_valve";

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        boolean superRequiresConfiguration = super.isConfigurationNeeded(cmTemplateProcessor, source);
        boolean result = false;
        if (superRequiresConfiguration) {
            String cdhVersion = ConfigUtils.getCdhVersion(source);
            String cmVersion = ConfigUtils.getCmVersion(source);
            boolean stackTypeIsDataLake = StackType.DATALAKE.equals(source.getStackType());
            boolean noHBaseAndHdfsButAtlas = cmTemplateProcessor.doesCMComponentExistsInBlueprint(ATLAS_SERVER_ROLE_TYPE)
                    && !cmTemplateProcessor.isServiceTypePresent(HbaseRoles.HBASE)
                    && !cmTemplateProcessor.isServiceTypePresent(HdfsRoles.HDFS);

            result = stackTypeIsDataLake
                    && noHBaseAndHdfsButAtlas
                    && isVersionNewerOrEqualThanLimited(cmVersion, CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_13_2_10000)
                    && isVersionNewerOrEqualThanLimited(cdhVersion, CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_2);
        }
        return result;
    }

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> serviceConfigs = new ArrayList<>();
        RdsView rdsView = getRdsView(source);
        serviceConfigs.add(config("atlas_database_host", rdsView.getHost()));
        serviceConfigs.add(config("atlas_database_name", rdsView.getDatabaseName()));
        serviceConfigs.add(config("atlas_database_user", rdsView.getConnectionUserName()));
        serviceConfigs.add(config("atlas_database_password", rdsView.getConnectionPassword()));
        serviceConfigs.add(config(ATLAS_SAFETY_VALVE_CONFIG_KEY, getSafetyValveProperty("atlas_database_type", "PostgreSQL")));
        return serviceConfigs;
    }

    @Override
    public DatabaseType dbType() {
        return DatabaseType.ATLAS;
    }

    @Override
    public String dbUserKey() {
        return "atlas_database_user";
    }

    @Override
    public String dbPasswordKey() {
        return "atlas_database_password";
    }

    @Override
    public String getServiceType() {
        return "ATLAS";
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(ATLAS_SERVER_ROLE_TYPE);
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> roleConfigs = new ArrayList<>();
        RdsView rdsView = getRdsView(source);
        if (rdsView.isUseSsl()) {
            LOGGER.info("Adding SSL options to {}", ATLAS_SERVER_ROLE_TYPE);
            roleConfigs.add(config("atlas.db.ssl.enabled", "true"));
            roleConfigs.add(config("atlas.db.ssl.required", "true"));
            roleConfigs.add(config("atlas.db.ssl.verifyServerCertificate", "true"));
            roleConfigs.add(config("atlas.db.ssl.certificateFile", rdsView.getSslCertificateFilePath()));
        }
        return roleConfigs;
    }
}

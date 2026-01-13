package com.sequenceiq.cloudbreak.cmtemplate.configproviders.dataviz;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.getSafetyValveProperty;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRdsRoleConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@Component
public class DatavizRoleConfigProvider extends AbstractRdsRoleConfigProvider {

    static final String METADATA_STORE = "METADATA_STORE";

    static final String METASTORE_DB_HOST = "METASTORE_DB_HOST";

    static final String METASTORE_DB_PORT = "METASTORE_DB_PORT";

    static final String METASTORE_DB_NAME = "METASTORE_DB_NAME";

    static final String METASTORE_DB_USER = "METASTORE_DB_USER";

    static final String DATAVIZ_ADVANCED_SETTINGS = "DATAVIZ_ADVANCED_SETTINGS";

    static final String DATAVIZ_SAFETY_VALVE = "dataviz-site.xml_service_safety_valve";

    static final String METASTORE_DB_PASSWORD = "METASTORE_DB_PASSWORD";

    static final String SSL_CONFIG_TEMPLATE = "\nif 'OPTIONS' not in DATABASES['default']:" +
            "\n\tDATABASES['default']['OPTIONS'] = {}" +
            "\nDATABASES['default']['OPTIONS'].update({" +
            "\n\t'sslmode': 'verify-ca'," +
            "\n\t'sslrootcert': '%s'" +
            "\n})";

    @Override
    public String dbUserKey() {
        return METASTORE_DB_NAME;
    }

    @Override
    public String dbPasswordKey() {
        return METASTORE_DB_PASSWORD;
    }

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        RdsView datavizRdsView = getRdsView(source);
        List<ApiClusterTemplateConfig> configs = new ArrayList<>();

        configs.add(config(METADATA_STORE, "postgresql"));
        configs.add(config(METASTORE_DB_HOST, datavizRdsView.getHost()));
        configs.add(config(METASTORE_DB_PORT, datavizRdsView.getPort()));
        configs.add(config(METASTORE_DB_NAME, datavizRdsView.getDatabaseName()));
        configs.add(config(METASTORE_DB_USER, datavizRdsView.getUserName()));
        configs.add(config(METASTORE_DB_PASSWORD, datavizRdsView.getPassword()));
        if (datavizRdsView.isUseSsl()) {
            configs.add(config(DATAVIZ_SAFETY_VALVE, getSafetyValveSslConfig(datavizRdsView)));
        }
        return configs;
    }

    private String getSafetyValveSslConfig(RdsView datavizRdsView) {
        return getSafetyValveProperty(
                DATAVIZ_ADVANCED_SETTINGS,
                String.format(SSL_CONFIG_TEMPLATE, datavizRdsView.getSslCertificateFilePath())
        );
    }

    @Override
    public String getServiceType() {
        return DatavizRoles.DATAVIZ;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(DatavizRoles.DATAVIZ_WEBSERVER);
    }

    @Override
    public DatabaseType dbType() {
        return DatabaseType.DATAVIZ;
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        return List.of();
    }
}

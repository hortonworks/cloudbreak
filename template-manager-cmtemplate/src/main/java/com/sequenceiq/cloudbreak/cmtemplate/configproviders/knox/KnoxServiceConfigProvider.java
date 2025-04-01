package com.sequenceiq.cloudbreak.cmtemplate.configproviders.knox;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_1_0;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isKnoxDatabaseSupported;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRdsRoleConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@Component
public class KnoxServiceConfigProvider extends AbstractRdsRoleConfigProvider implements BaseKnoxConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(KnoxServiceConfigProvider.class);

    private static final String KNOX_AUTORESTART_ON_STOP = "autorestart_on_stop";

    private static final String DATABASE_TYPE = "knox_gateway_database_type";

    private static final String DATABASE_NAME = "knox_gateway_database_name";

    private static final String DATABASE_HOST = "knox_gateway_database_host";

    private static final String DATABASE_PORT = "knox_gateway_database_port";

    private static final String DATABASE_USER = "knox_gateway_database_user";

    private static final String DATABASE_PASSWORD = "knox_gateway_database_password";

    private static final String GATEWAY_DATABASE_SSL_ENABLED = "gateway_database_ssl_enabled";

    private static final String GATEWAY_DATABASE_SSL_TRUSTSTORE_FILE = "gateway_database_ssl_truststore_file";

    @Override
    public String dbUserKey() {
        return DATABASE_USER;
    }

    @Override
    public String dbPasswordKey() {
        return DATABASE_PASSWORD;
    }

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> configList = new ArrayList<>();
        LOGGER.info("The productDetailsView is: {} ", source.getProductDetailsView());
        if (source.getProductDetailsView() != null
                && isKnoxDatabaseSupported(source.getProductDetailsView().getCm(), getCdhProduct(source), getCdhPatchVersion(source))) {
            LOGGER.info("Configuring Knox topology database.");
            RdsView knoxGatewayRdsView = getRdsView(source);
            LOGGER.info("The Knox rdsConfig is {}.", knoxGatewayRdsView);
            configList.add(config(DATABASE_TYPE, knoxGatewayRdsView.getSubprotocol()));
            configList.add(config(DATABASE_NAME, knoxGatewayRdsView.getDatabaseName()));
            configList.add(config(DATABASE_HOST, knoxGatewayRdsView.getHost()));
            configList.add(config(DATABASE_PORT, knoxGatewayRdsView.getPort()));
            configList.add(config(DATABASE_USER, knoxGatewayRdsView.getConnectionUserName()));
            configList.add(config(DATABASE_PASSWORD, knoxGatewayRdsView.getConnectionPassword()));
        } else {
            LOGGER.info("The Knox database configuration is not supported.");
        }
        configList.add(config(KNOX_AUTORESTART_ON_STOP, Boolean.TRUE.toString()));
        return configList;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(KnoxRoles.KNOX_GATEWAY, KnoxRoles.IDBROKER);
    }

    @Override
    public DatabaseType dbType() {
        return DatabaseType.KNOX_GATEWAY;
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        String cmVersion = cmTemplateProcessor.getCmVersion().orElse("");
        LOGGER.info("The cm version is: {} ", cmVersion);
        return isVersionNewerOrEqualThanLimited(cmVersion, CLOUDERAMANAGER_VERSION_7_1_0);
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> configList = new ArrayList<>();
        if (source.getProductDetailsView() != null
                && isKnoxDatabaseSupported(source.getProductDetailsView().getCm(), getCdhProduct(source), getCdhPatchVersion(source))) {
            LOGGER.info("Configuring Knox topology database.");
            RdsView knoxGatewayRdsView = getRdsView(source);
            LOGGER.info("The Knox rdsConfig is {}.", knoxGatewayRdsView);
            if (knoxGatewayRdsView.isUseSsl()) {
                LOGGER.info("The Knox rds will use ssl.");
                configList.add(config(GATEWAY_DATABASE_SSL_ENABLED, "true"));
                configList.add(config(GATEWAY_DATABASE_SSL_TRUSTSTORE_FILE, knoxGatewayRdsView.getSslCertificateFilePath()));
            }
        }
        return configList;
    }

}

package com.sequenceiq.cloudbreak.cmtemplate.configproviders.ssb;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.FLINK_VERSION_1_15_1;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry.StreamingAppRdsRoleConfigProviderUtil.dataBaseTypeForCM;
import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.flink.FlinkConfigProviderUtils;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@Component
public class SqlStreamBuilderSseConfigProvider extends SqlStreamBuilderConfigProvider {

    static final String DATABASE_TYPE = "database_type";

    static final String DATABASE_HOST = "database_host";

    static final String DATABASE_PORT = "database_port";

    static final String DATABASE_SCHEMA = "database_schema";

    static final String DATABASE_USER = "database_user";

    static final String DATABASE_PASSWORD = "database_password";

    static final String DATABASE_JDBC_URL_OVERRIDE = "database_jdbc_url_override";

    @Inject
    private FlinkConfigProviderUtils utils;

    @Override
    public String dbUserKey() {
        return DATABASE_USER;
    }

    @Override
    public String dbPasswordKey() {
        return DATABASE_PASSWORD;
    }

    @Override
    public String getServiceType() {
        return SqlStreamBuilderRoles.SQL_STREAM_BUILDER;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(SqlStreamBuilderRoles.STREAMING_SQL_ENGINE, SqlStreamBuilderRoles.MATERIALIZED_VIEW_ENGINE);
    }

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        RdsView ssbRdsView = getRdsView(source);

        Optional<ClouderaManagerProduct> flinkProduct = utils.getFlinkProduct(source.getProductDetailsView().getProducts());
        List<ApiClusterTemplateConfig> configList = new ArrayList<>();
        addDbConfigs(ssbRdsView, configList);
        addDbSslConfigsIfNeeded(ssbRdsView, configList, flinkProduct);

        String cdhVersion = getCdhVersion(source);
        utils.addReleaseNameIfNeeded(cdhVersion, configList, flinkProduct);

        return configList;
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        return emptyList();
    }

    @Override
    public DatabaseType dbType() {
        return DatabaseType.SQL_STREAM_BUILDER_ADMIN;
    }

    private void addDbConfigs(RdsView ssbRdsView, List<ApiClusterTemplateConfig> configList) {
        configList.add(config(DATABASE_TYPE, dataBaseTypeForCM(ssbRdsView.getDatabaseVendor())));
        configList.add(config(DATABASE_HOST, ssbRdsView.getHost()));
        configList.add(config(DATABASE_PORT, ssbRdsView.getPort()));
        configList.add(config(DATABASE_SCHEMA, ssbRdsView.getDatabaseName()));
        configList.add(config(DATABASE_USER, ssbRdsView.getConnectionUserName()));
        configList.add(config(DATABASE_PASSWORD, ssbRdsView.getConnectionPassword()));
    }

    private void addDbSslConfigsIfNeeded(
            RdsView ssbRdsView,
            List<ApiClusterTemplateConfig> configList,
            Optional<ClouderaManagerProduct> flinkProduct) {
        flinkProduct.ifPresent(fp -> {
            if (isVersionNewerOrEqualThanLimited(getFlinkVersion(fp), FLINK_VERSION_1_15_1) && ssbRdsView.isUseSsl()) {
                configList.add(config(DATABASE_JDBC_URL_OVERRIDE, ssbRdsView.getConnectionURL()));
            }
        });
    }

    private String getFlinkVersion(ClouderaManagerProduct flinkProduct) {
        // 1.15.1-csadh1.9.0.1-cdh7.2.16.0-254-37351973 -> 1.15.1
        return flinkProduct.getVersion().split("-")[0];
    }
}

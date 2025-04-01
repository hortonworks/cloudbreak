package com.sequenceiq.cloudbreak.cmtemplate.configproviders.queryprocessor;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_9_2;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.getCmVersion;

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
public class QueryProcessorConfigProvider extends AbstractRdsRoleConfigProvider {

    @Override
    public String dbUserKey() {
        return "query_processor_database_username";
    }

    @Override
    public String dbPasswordKey() {
        return "query_processor_database_password";
    }

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        RdsView queryProcessorView = getRdsView(source);
        List<ApiClusterTemplateConfig> configList = new ArrayList<>();
        addDbConfigs(queryProcessorView, configList);
        addDbSslConfigsIfNeeded(queryProcessorView, configList, getCmVersion(source));
        return configList;
    }

    private void addDbConfigs(RdsView queryProcessorView, List<ApiClusterTemplateConfig> configList) {
        configList.add(config("query_processor_database_host", queryProcessorView.getHost()));
        configList.add(config("query_processor_database_port", queryProcessorView.getPort()));
        configList.add(config("query_processor_database_name", queryProcessorView.getDatabaseName()));
        configList.add(config(dbUserKey(), queryProcessorView.getConnectionUserName()));
        configList.add(config(dbPasswordKey(), queryProcessorView.getConnectionPassword()));
    }

    private void addDbSslConfigsIfNeeded(RdsView queryProcessorView, List<ApiClusterTemplateConfig> configList, String cmVersion) {
        if (isVersionNewerOrEqualThanLimited(cmVersion, CLOUDERAMANAGER_VERSION_7_9_2) && queryProcessorView.isUseSsl()) {
            configList.add(config("query_processor_database_url_query_params", getDbSslConnectionUrlOptions(queryProcessorView)));
        }
    }

    private String getDbSslConnectionUrlOptions(RdsView queryProcessorView) {
        String connectionURLOptions = queryProcessorView.getConnectionURLOptions();
        if (!connectionURLOptions.startsWith("?")) {
            throw new IllegalStateException(String.format("Malformed connectionURLOptions string; expected to start with '?' but it did not. Received: '%s'",
                    connectionURLOptions));
        }
        // Query Processor expects a string of additional JDBC options here (starting with &) that will be appended after some further options.
        return "&" + connectionURLOptions.substring(1);
    }

    @Override
    public String getServiceType() {
        return QueryStoreRoles.QUERY_PROCESSOR;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(QueryStoreRoles.QUERY_PROCESSOR);
    }

    @Override
    public DatabaseType dbType() {
        return DatabaseType.QUERY_PROCESSOR;
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        return List.of();
    }

}

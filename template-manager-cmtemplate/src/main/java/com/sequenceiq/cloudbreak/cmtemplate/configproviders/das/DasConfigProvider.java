package com.sequenceiq.cloudbreak.cmtemplate.configproviders.das;

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
public class DasConfigProvider extends AbstractRdsRoleConfigProvider {

    @Override
    public String dbUserKey() {
        return "data_analytics_studio_database_username";
    }

    @Override
    public String dbPasswordKey() {
        return "data_analytics_studio_database_password";
    }

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        RdsView dasView = getRdsView(source);
        List<ApiClusterTemplateConfig> configList = new ArrayList<>();
        addDbConfigs(dasView, configList);
        addDbSslConfigsIfNeeded(dasView, configList, getCmVersion(source));
        return configList;
    }

    private void addDbConfigs(RdsView dasView, List<ApiClusterTemplateConfig> configList) {
        configList.add(config("data_analytics_studio_database_host", dasView.getHost()));
        configList.add(config("data_analytics_studio_database_port", dasView.getPort()));
        configList.add(config("data_analytics_studio_database_name", dasView.getDatabaseName()));
        configList.add(config(dbUserKey(), dasView.getConnectionUserName()));
        configList.add(config(dbPasswordKey(), dasView.getConnectionPassword()));
    }

    private void addDbSslConfigsIfNeeded(RdsView dasView, List<ApiClusterTemplateConfig> configList, String cmVersion) {
        if (isVersionNewerOrEqualThanLimited(cmVersion, CLOUDERAMANAGER_VERSION_7_9_2) && dasView.isUseSsl()) {
            configList.add(config("data_analytics_studio_database_url_query_params", dasView.getConnectionURLOptions()));
        }
    }

    @Override
    public String getServiceType() {
        return DasRoles.DAS;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(DasRoles.WEBAPP, DasRoles.EVENTPROCESSOR);
    }

    @Override
    public DatabaseType dbType() {
        return DatabaseType.HIVE_DAS;
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        switch (roleType) {
            case DasRoles.WEBAPP:
                return List.of(config("data_analytics_studio_user_authentication", "KNOX_PROXY"));
            default:
                return List.of();
        }
    }

}

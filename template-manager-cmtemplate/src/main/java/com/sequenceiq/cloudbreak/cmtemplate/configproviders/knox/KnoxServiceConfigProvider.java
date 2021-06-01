package com.sequenceiq.cloudbreak.cmtemplate.configproviders.knox;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_1_0;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isKnoxDatabaseSupported;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static java.util.Collections.emptyList;

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
public class KnoxServiceConfigProvider extends AbstractRdsRoleConfigProvider {

    private static final String KNOX_AUTORESTART_ON_STOP = "autorestart_on_stop";

    private static final String DATABASE_TYPE = "knox_gateway_database_type";

    private static final String DATABASE_NAME = "knox_gateway_database_name";

    private static final String DATABASE_HOST = "knox_gateway_database_host";

    private static final String DATABASE_PORT = "knox_gateway_database_port";

    private static final String DATABASE_USER = "knox_gateway_database_user";

    private static final String DATABASE_PASSWORD = "knox_gateway_database_password";

    @Override
    public String getServiceType() {
        return KnoxRoles.KNOX;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(KnoxRoles.KNOX_GATEWAY, KnoxRoles.IDBROKER);
    }

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> configList = new ArrayList<>();
        if (source.getProductDetailsView() != null
                && isKnoxDatabaseSupported(source.getProductDetailsView().getCm(), getCdhProduct(source), getCdhPatchVersion(source))) {
            RdsView knoxGatewayRdsView = getRdsView(source);
            configList.add(config(DATABASE_TYPE, knoxGatewayRdsView.getSubprotocol()));
            configList.add(config(DATABASE_NAME, knoxGatewayRdsView.getDatabaseName()));
            configList.add(config(DATABASE_HOST, knoxGatewayRdsView.getHost()));
            configList.add(config(DATABASE_PORT, knoxGatewayRdsView.getPort()));
            configList.add(config(DATABASE_USER, knoxGatewayRdsView.getConnectionUserName()));
            configList.add(config(DATABASE_PASSWORD, knoxGatewayRdsView.getConnectionPassword()));
        }
        configList.add(config(KNOX_AUTORESTART_ON_STOP, Boolean.TRUE.toString()));
        return configList;
    }

    @Override
    protected DatabaseType dbType() {
        return DatabaseType.KNOX_GATEWAY;
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        String cmVersion = cmTemplateProcessor.getCmVersion().orElse("");
        return isVersionNewerOrEqualThanLimited(cmVersion, CLOUDERAMANAGER_VERSION_7_1_0);
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        return emptyList();
    }

}

package com.sequenceiq.cloudbreak.cmtemplate.configproviders.smm;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry.StreamingAppRdsRoleConfigProviderUtil.dataBaseTypeForCM;
import static java.util.Collections.emptyList;

import java.util.List;

import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRdsRoleConfigProvider;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@Component
public class StreamsMessagingManagerServiceConfigProvider extends AbstractRdsRoleConfigProvider {

    static final String DATABASE_TYPE = "smm_database_type";

    static final String DATABASE_NAME = "smm_database_name";

    static final String DATABASE_HOST = "smm_database_host";

    static final String DATABASE_PORT = "smm_database_port";

    static final String DATABASE_USER = "smm_database_user";

    static final String DATABASE_PASSWORD = "smm_database_password";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        RdsView smmRdsView = getRdsView(source);
        return List.of(
                config(DATABASE_TYPE, dataBaseTypeForCM(smmRdsView.getDatabaseVendor())),
                config(DATABASE_NAME, smmRdsView.getDatabaseName()),
                config(DATABASE_HOST, smmRdsView.getHost()),
                config(DATABASE_PORT, smmRdsView.getPort()),
                config(DATABASE_USER, smmRdsView.getConnectionUserName()),
                config(DATABASE_PASSWORD, smmRdsView.getConnectionPassword())
        );
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        return emptyList();
    }

    @Override
    public String getServiceType() {
        return StreamsMessagingManagerRoles.STREAMS_MESSAGING_MANAGER;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(StreamsMessagingManagerRoles.STREAMS_MESSAGING_MANAGER_SERVER);
    }

    @Override
    protected DatabaseType dbType() {
        return DatabaseType.STREAMS_MESSAGING_MANAGER;
    }
}

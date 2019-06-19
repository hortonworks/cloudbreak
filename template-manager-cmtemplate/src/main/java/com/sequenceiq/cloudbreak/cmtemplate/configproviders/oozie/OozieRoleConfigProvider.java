package com.sequenceiq.cloudbreak.cmtemplate.configproviders.oozie;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRdsRoleConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@Component
public class OozieRoleConfigProvider extends AbstractRdsRoleConfigProvider {

    private static final String OOZIE_DATABASE_HOST = "oozie_database_host";

    private static final String OOZIE_DATABASE_NAME = "oozie_database_name";

    private static final String OOZIE_DATABASE_TYPE = "oozie_database_type";

    private static final String OOZIE_DATABASE_USER = "oozie_database_user";

    private static final String OOZIE_DATABASE_PASSWORD = "oozie_database_password";

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        switch (roleType) {
            case OozieRoles.OOZIE_SERVER:
                RdsView oozieRdsView = getRdsView(source);
                return List.of(
                        config(OOZIE_DATABASE_HOST, oozieRdsView.getHost()),
                        config(OOZIE_DATABASE_NAME, oozieRdsView.getDatabaseName()),
                        config(OOZIE_DATABASE_TYPE, oozieRdsView.getSubprotocol()),
                        config(OOZIE_DATABASE_USER, oozieRdsView.getConnectionUserName()),
                        config(OOZIE_DATABASE_PASSWORD, oozieRdsView.getConnectionPassword())
                );
            default:
                return List.of();
        }
    }

    @Override
    public String getServiceType() {
        return OozieRoles.OOZIE;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(OozieRoles.OOZIE_SERVER);
    }

    @Override
    protected DatabaseType dbType() {
        return DatabaseType.OOZIE;
    }
}
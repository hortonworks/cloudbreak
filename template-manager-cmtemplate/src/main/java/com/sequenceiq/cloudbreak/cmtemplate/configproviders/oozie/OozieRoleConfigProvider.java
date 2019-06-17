package com.sequenceiq.cloudbreak.cmtemplate.configproviders.oozie;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.google.common.base.Preconditions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@Component
public class OozieRoleConfigProvider extends AbstractRoleConfigProvider {

    private static final String OOZIE_DATABASE_HOST = "oozie_database_host";

    private static final String OOZIE_DATABASE_NAME = "oozie_database_name";

    private static final String OOZIE_DATABASE_TYPE = "oozie_database_type";

    private static final String OOZIE_DATABASE_USER = "oozie_database_user";

    private static final String OOZIE_DATABASE_PASSWORD = "oozie_database_password";

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        switch (roleType) {
            case OozieRoles.OOZIE_SERVER:
                Optional<RDSConfig> rdsConfigOptional = getFirstRDSConfigOptional(source);
                Preconditions.checkArgument(rdsConfigOptional.isPresent());
                RdsView oozieRdsView = new RdsView(rdsConfigOptional.get());
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
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return getFirstRDSConfigOptional(source).isPresent() && cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }

    private Optional<RDSConfig> getFirstRDSConfigOptional(TemplatePreparationObject source) {
        return ConfigUtils.getFirstRDSConfigOptional(source, DatabaseType.OOZIE);
    }
}
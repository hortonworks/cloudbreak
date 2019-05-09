package com.sequenceiq.cloudbreak.cmtemplate.configproviders.rds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateVariable;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@Component
public class OozieConfigProvider extends AbstractRdsConfigProvider {

    private static final String OOZIE_DATABASE_HOST = "oozie_database_host";

    private static final String OOZIE_DATABASE_NAME = "oozie_database_name";

    private static final String OOZIE_DATABASE_TYPE = "oozie_database_type";

    private static final String OOZIE_DATABASE_USER = "oozie_database_user";

    private static final String OOZIE_DATABASE_PASSWORD = "oozie_database_password";

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfig(String roleType, HostgroupView hostGroupView) {
        List<ApiClusterTemplateConfig> roleConfigs = new ArrayList<>();

        switch (roleType) {
            case "OOZIE_SERVER":
                String databaseHost = getRoleTypeVariableName(hostGroupView.getName(), roleType, OOZIE_DATABASE_HOST);
                roleConfigs.add(new ApiClusterTemplateConfig().name(OOZIE_DATABASE_HOST).variable(databaseHost));

                String databaseName = getRoleTypeVariableName(hostGroupView.getName(), roleType, OOZIE_DATABASE_NAME);
                roleConfigs.add(new ApiClusterTemplateConfig().name(OOZIE_DATABASE_NAME).variable(databaseName));

                String databaseType = getRoleTypeVariableName(hostGroupView.getName(), roleType, OOZIE_DATABASE_TYPE);
                roleConfigs.add(new ApiClusterTemplateConfig().name(OOZIE_DATABASE_TYPE).variable(databaseType));

                String databaseUser = getRoleTypeVariableName(hostGroupView.getName(), roleType, OOZIE_DATABASE_USER);
                roleConfigs.add(new ApiClusterTemplateConfig().name(OOZIE_DATABASE_USER).variable(databaseUser));

                String databasePassword = getRoleTypeVariableName(hostGroupView.getName(), roleType, OOZIE_DATABASE_PASSWORD);
                roleConfigs.add(new ApiClusterTemplateConfig().name(OOZIE_DATABASE_PASSWORD).variable(databasePassword));

                break;
            default:
                break;
        }

        return roleConfigs;
    }

    @Override
    protected List<ApiClusterTemplateVariable> getVariables(String roleType, HostgroupView hostGroupView, TemplatePreparationObject templatePreparationObject) {
        List<ApiClusterTemplateVariable> variables = new ArrayList<>();

        switch (roleType) {
            case "OOZIE_SERVER":
                RdsView oozieRdsView = new RdsView(getFirstRDSConfigOptional(templatePreparationObject).get());

                String databaseHostVar = getRoleTypeVariableName(hostGroupView.getName(), roleType, OOZIE_DATABASE_HOST);
                variables.add(new ApiClusterTemplateVariable().name(databaseHostVar).value(oozieRdsView.getHost()));

                String databaseNameVar = getRoleTypeVariableName(hostGroupView.getName(), roleType, OOZIE_DATABASE_NAME);
                variables.add(new ApiClusterTemplateVariable().name(databaseNameVar).value(oozieRdsView.getDatabaseName()));

                String databaseTypeVar = getRoleTypeVariableName(hostGroupView.getName(), roleType, OOZIE_DATABASE_TYPE);
                variables.add(new ApiClusterTemplateVariable().name(databaseTypeVar).value(oozieRdsView.getSubprotocol()));

                String databaseUserVar = getRoleTypeVariableName(hostGroupView.getName(), roleType, OOZIE_DATABASE_USER);
                variables.add(new ApiClusterTemplateVariable().name(databaseUserVar).value(oozieRdsView.getConnectionUserName()));

                String databasePasswordVar = getRoleTypeVariableName(hostGroupView.getName(), roleType, OOZIE_DATABASE_PASSWORD);
                variables.add(new ApiClusterTemplateVariable().name(databasePasswordVar).value(oozieRdsView.getConnectionPassword()));

                break;
            default:
                break;
        }

        return variables;
    }

    @Override
    public String getServiceType() {
        return "OOZIE";
    }

    @Override
    public List<String> getRoleTypes() {
        return Collections.singletonList("OOZIE_SERVER");
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return getFirstRDSConfigOptional(source).isPresent() && cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }

    private Optional<RDSConfig> getFirstRDSConfigOptional(TemplatePreparationObject source) {
        return source.getRdsConfigs().stream().filter(rds -> DatabaseType.OOZIE.name().equalsIgnoreCase(rds.getType())).findFirst();
    }
}
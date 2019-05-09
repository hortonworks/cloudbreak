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
public class RangerConfigProvider extends AbstractRdsConfigProvider {

    private static final String RANGER_DATABASE_HOST = "ranger_database_host";

    private static final String RANGER_DATABASE_NAME = "ranger_database_name";

    private static final String RANGER_DATABASE_TYPE = "ranger_database_type";

    private static final String RANGER_DATABASE_USER = "ranger_database_user";

    private static final String RANGER_DATABASE_PASSWORD = "ranger_database_password";

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfig(String roleType, HostgroupView hostGroupView) {
        List<ApiClusterTemplateConfig> roleConfigs = new ArrayList<>();

        switch (roleType) {
            case "RANGER_ADMIN":
                String databaseHost = getRoleTypeVariableName(hostGroupView.getName(), roleType, RANGER_DATABASE_HOST);
                roleConfigs.add(new ApiClusterTemplateConfig().name(RANGER_DATABASE_HOST).variable(databaseHost));

                String databaseName = getRoleTypeVariableName(hostGroupView.getName(), roleType, RANGER_DATABASE_NAME);
                roleConfigs.add(new ApiClusterTemplateConfig().name(RANGER_DATABASE_NAME).variable(databaseName));

                String databaseType = getRoleTypeVariableName(hostGroupView.getName(), roleType, RANGER_DATABASE_TYPE);
                roleConfigs.add(new ApiClusterTemplateConfig().name(RANGER_DATABASE_TYPE).variable(databaseType));

                String databaseUser = getRoleTypeVariableName(hostGroupView.getName(), roleType, RANGER_DATABASE_USER);
                roleConfigs.add(new ApiClusterTemplateConfig().name(RANGER_DATABASE_USER).variable(databaseUser));

                String databasePassword = getRoleTypeVariableName(hostGroupView.getName(), roleType, RANGER_DATABASE_PASSWORD);
                roleConfigs.add(new ApiClusterTemplateConfig().name(RANGER_DATABASE_PASSWORD).variable(databasePassword));

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
            case "RANGER_ADMIN":
                RdsView rangerRdsView = new RdsView(getFirstRDSConfigOptional(templatePreparationObject).get());

                String databaseHostVar = getRoleTypeVariableName(hostGroupView.getName(), roleType, RANGER_DATABASE_HOST);
                variables.add(new ApiClusterTemplateVariable().name(databaseHostVar).value(rangerRdsView.getHost()));

                String databaseNameVar = getRoleTypeVariableName(hostGroupView.getName(), roleType, RANGER_DATABASE_NAME);
                variables.add(new ApiClusterTemplateVariable().name(databaseNameVar).value(rangerRdsView.getDatabaseName()));

                String databaseTypeVar = getRoleTypeVariableName(hostGroupView.getName(), roleType, RANGER_DATABASE_TYPE);
                variables.add(new ApiClusterTemplateVariable().name(databaseTypeVar).value(rangerRdsView.getSubprotocol()));

                String databaseUserVar = getRoleTypeVariableName(hostGroupView.getName(), roleType, RANGER_DATABASE_USER);
                variables.add(new ApiClusterTemplateVariable().name(databaseUserVar).value(rangerRdsView.getConnectionUserName()));

                String databasePasswordVar = getRoleTypeVariableName(hostGroupView.getName(), roleType, RANGER_DATABASE_PASSWORD);
                variables.add(new ApiClusterTemplateVariable().name(databasePasswordVar).value(rangerRdsView.getConnectionPassword()));

                break;
            default:
                break;
        }

        return variables;
    }

    @Override
    public String getServiceType() {
        return "RANGER";
    }

    @Override
    public List<String> getRoleTypes() {
        return Collections.singletonList("RANGER_ADMIN");
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return getFirstRDSConfigOptional(source).isPresent() && cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }

    private Optional<RDSConfig> getFirstRDSConfigOptional(TemplatePreparationObject source) {
        return source.getRdsConfigs().stream().filter(rds -> DatabaseType.RANGER.name().equalsIgnoreCase(rds.getType())).findFirst();
    }
}
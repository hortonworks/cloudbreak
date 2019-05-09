package com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateVariable;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigConfigProvider;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@Component
public class RangerRoleConfigProvider extends AbstractRoleConfigConfigProvider {

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfig(String roleType, HostgroupView hostGroupView) {
        List<ApiClusterTemplateConfig> roleConfigs = new ArrayList<>();

        switch (roleType) {
            case "RANGER_ADMIN":
                roleConfigs.add(new ApiClusterTemplateConfig().name("ranger_database_host").variable("ranger-ranger_database_host"));
                roleConfigs.add(new ApiClusterTemplateConfig().name("ranger_database_name").variable("ranger-ranger_database_name"));
                roleConfigs.add(new ApiClusterTemplateConfig().name("ranger_database_type").variable("ranger-ranger_database_type"));
                roleConfigs.add(new ApiClusterTemplateConfig().name("ranger_database_user").variable("ranger-ranger_database_user"));
                roleConfigs.add(new ApiClusterTemplateConfig().name("ranger_database_password").variable("ranger-ranger_database_password"));
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
                variables.add(new ApiClusterTemplateVariable().name("ranger-ranger_database_host").value(rangerRdsView.getHost()));
                variables.add(new ApiClusterTemplateVariable().name("ranger-ranger_database_name").value(rangerRdsView.getDatabaseName()));
                variables.add(new ApiClusterTemplateVariable().name("ranger-ranger_database_type").value(rangerRdsView.getSubprotocol()));
                variables.add(new ApiClusterTemplateVariable().name("ranger-ranger_database_user").value(rangerRdsView.getConnectionUserName()));
                variables.add(new ApiClusterTemplateVariable().name("ranger-ranger_database_password").value(rangerRdsView.getConnectionPassword()));
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
        return cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }

    private Optional<RDSConfig> getFirstRDSConfigOptional(TemplatePreparationObject source) {
        return source.getRdsConfigs().stream().filter(rds -> DatabaseType.RANGER.name().equalsIgnoreCase(rds.getType())).findFirst();
    }
}

package com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateVariable;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigConfigProvider;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
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
                RDSConfig rdsConfig = getRDSConfig(templatePreparationObject);
                RdsView rangerRdsView = new RdsView(rdsConfig);
                if (rangerRdsView.getDatabaseVendor() == DatabaseVendor.POSTGRES) {
                    variables.add(new ApiClusterTemplateVariable().name("ranger-ranger_database_host").value(rangerRdsView.getHost()));
                    variables.add(new ApiClusterTemplateVariable().name("ranger-ranger_database_name").value(rangerRdsView.getDatabaseName()));
                    variables.add(new ApiClusterTemplateVariable().name("ranger-ranger_database_type").value("PostgreSQL"));
                    variables.add(new ApiClusterTemplateVariable().name("ranger-ranger_database_user").value(rangerRdsView.getConnectionUserName()));
                    variables.add(new ApiClusterTemplateVariable().name("ranger-ranger_database_password").value(rangerRdsView.getConnectionPassword()));
                } else {
                    throw new CloudbreakServiceException("Ranger database supports only PostgreSQL");
                }
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

    private RDSConfig getRDSConfig(TemplatePreparationObject source) {
        List<RDSConfig> rdsConfigs = source.getRdsConfigs().stream().
                filter(rds -> DatabaseType.RANGER.name().equalsIgnoreCase(rds.getType())).collect(Collectors.toList());
        if (rdsConfigs.size() < 1) {
            throw new CloudbreakServiceException("Ranger database has not been provided for RANGER_ADMIN component");

        } else if (rdsConfigs.size() > 1) {
            throw new CloudbreakServiceException("Multiple databases have been provided for RANGER_ADMIN component");
        }

        return rdsConfigs.get(0);
    }
}

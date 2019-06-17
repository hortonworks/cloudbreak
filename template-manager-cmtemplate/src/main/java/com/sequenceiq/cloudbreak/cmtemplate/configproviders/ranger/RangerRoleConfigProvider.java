package com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@Component
public class RangerRoleConfigProvider extends AbstractRoleConfigProvider {

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        switch (roleType) {
            case RangerRoles.RANGER_ADMIN:
                RDSConfig rdsConfig = getRDSConfig(source);
                RdsView rangerRdsView = new RdsView(rdsConfig);
                return List.of(
                        config("ranger_database_host", rangerRdsView.getHost()),
                        config("ranger_database_name", rangerRdsView.getDatabaseName()),
                        config("ranger_database_type", getRangerDbType(rangerRdsView)),
                        config("ranger_database_user", rangerRdsView.getConnectionUserName()),
                        config("ranger_database_password", rangerRdsView.getConnectionPassword())
                );
            default:
                return List.of();
        }
    }

    @Override
    public String getServiceType() {
        return RangerRoles.RANGER;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(RangerRoles.RANGER_ADMIN);
    }

    private String getRangerDbType(RdsView rdsView) {
        switch (rdsView.getDatabaseVendor()) {
            case POSTGRES:
                return "PostgreSQL";
            default:
                throw new CloudbreakServiceException("Unsupported Ranger database type: " + rdsView.getDatabaseVendor().displayName());
        }
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

package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.google.common.base.Preconditions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@Component
public class HiveMetastoreConfigProvider extends AbstractRoleConfigConfigProvider {

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(TemplatePreparationObject templatePreparationObject) {
        Optional<RDSConfig> rdsConfigOptional = getFirstRDSConfigOptional(templatePreparationObject);
        Preconditions.checkArgument(rdsConfigOptional.isPresent());
        RdsView hiveView = new RdsView(rdsConfigOptional.get());
        return List.of(
                config("hive_metastore_database_host", hiveView.getHost()),
                config("hive_metastore_database_name", hiveView.getDatabaseName()),
                config("hive_metastore_database_password", hiveView.getConnectionPassword()),
                config("hive_metastore_database_port", hiveView.getPort()),
                config("hive_metastore_database_type", hiveView.getSubprotocol()),
                config("hive_metastore_database_user", hiveView.getConnectionUserName())
        );
    }

    @Override
    public String getServiceType() {
        return HiveRoles.HIVE;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(HiveRoles.HIVEMETASTORE);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return getFirstRDSConfigOptional(source).isPresent() && cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfig(String roleType, HostgroupView hostGroupView, TemplatePreparationObject source) {
        return List.of(
                config("metastore_canary_health_enabled", Boolean.FALSE.toString())
        );
    }

    private Optional<RDSConfig> getFirstRDSConfigOptional(TemplatePreparationObject source) {
        return ConfigUtils.getFirstRDSConfigOptional(source, DatabaseType.HIVE);
    }

}

package com.sequenceiq.cloudbreak.cmtemplate.configproviders;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateVariable;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@Component
public class HiveMetastoreConfigProvider implements CmTemplateComponentConfigProvider {

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(TemplatePreparationObject templatePreparationObject) {
        List<ApiClusterTemplateConfig> result = new ArrayList<>();
        result.add(new ApiClusterTemplateConfig().name("hive_metastore_database_password").variable("hive-hive_metastore_database_password"));
        result.add(new ApiClusterTemplateConfig().name("hive_metastore_database_port").variable("hive-hive_metastore_database_port"));
        result.add(new ApiClusterTemplateConfig().name("hive_metastore_database_host").variable("hive-hive_metastore_database_host"));
        result.add(new ApiClusterTemplateConfig().name("hive_metastore_database_type").variable("hive-hive_metastore_database_type"));
        result.add(new ApiClusterTemplateConfig().name("hive_metastore_database_name").variable("hive-hive_metastore_database_name"));
        return result;
    }

    @Override
    public List<ApiClusterTemplateVariable> getVariables(TemplatePreparationObject source) {
        List<ApiClusterTemplateVariable> result = new ArrayList<>();
        RdsView hiveView = new RdsView(getFirstRDSConfigOptional(source).get());
        result.add(new ApiClusterTemplateVariable().name("hive-hive_metastore_database_host").value(hiveView.getHost()));
        result.add(new ApiClusterTemplateVariable().name("hive-hive_metastore_database_port").value(hiveView.getPort()));
        result.add(new ApiClusterTemplateVariable().name("hive-hive_metastore_database_name").value(hiveView.getDatabaseName()));
        result.add(new ApiClusterTemplateVariable().name("hive-hive_metastore_database_type").value(hiveView.getSubprotocol()));
        result.add(new ApiClusterTemplateVariable().name("hive-hive_metastore_database_password").value(hiveView.getConnectionPassword()));
        return result;
    }

    @Override
    public String getServiceType() {
        return "HIVE";
    }

    @Override
    public String getRoleType() {
        return "HIVEMETASTORE";
    }

    @Override
    public boolean specialCondition(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return getFirstRDSConfigOptional(source).isPresent() && cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleType());
    }

    private Optional<RDSConfig> getFirstRDSConfigOptional(TemplatePreparationObject source) {
        return source.getRdsConfigs().stream().filter(rds -> DatabaseType.HIVE.name().equalsIgnoreCase(rds.getType())).findFirst();
    }
}

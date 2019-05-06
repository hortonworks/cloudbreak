package com.sequenceiq.cloudbreak.cmtemplate.configproviders;

import java.util.ArrayList;
import java.util.Collections;
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
public class SqoopConfigProvider implements CmTemplateComponentConfigProvider {

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(TemplatePreparationObject templatePreparationObject) {
        List<ApiClusterTemplateConfig> result = new ArrayList<>();
        result.add(new ApiClusterTemplateConfig().name("sqoop_repository_database_host").variable("sqoop-sqoop_repository_database_host"));
        result.add(new ApiClusterTemplateConfig().name("sqoop_repository_database_name").variable("sqoop-sqoop_repository_database_name"));
        result.add(new ApiClusterTemplateConfig().name("sqoop_repository_database_type").variable("sqoop-sqoop_repository_database_type"));
        result.add(new ApiClusterTemplateConfig().name("sqoop_repository_database_user").variable("sqoop-sqoop_repository_database_user"));
        result.add(new ApiClusterTemplateConfig().name("sqoop_repository_database_password").variable("sqoop-sqoop_repository_database_password"));
        return result;
    }

    @Override
    public List<ApiClusterTemplateVariable> getServiceConfigVariables(TemplatePreparationObject source) {
        List<ApiClusterTemplateVariable> result = new ArrayList<>();
        RdsView sqoopRdsView = new RdsView(getFirstRDSConfigOptional(source).get());
        result.add(new ApiClusterTemplateVariable().name("sqoop-sqoop_repository_database_host").value(sqoopRdsView.getHost()));
        result.add(new ApiClusterTemplateVariable().name("sqoop-sqoop_repository_database_name").value(sqoopRdsView.getDatabaseName()));
        result.add(new ApiClusterTemplateVariable().name("sqoop-sqoop_repository_database_type").value(sqoopRdsView.getSubprotocol()));
        result.add(new ApiClusterTemplateVariable().name("sqoop-sqoop_repository_database_user").value(sqoopRdsView.getConnectionUserName()));
        result.add(new ApiClusterTemplateVariable().name("sqoop-sqoop_repository_database_password").value(sqoopRdsView.getConnectionPassword()));
        return result;
    }

    @Override
    public String getServiceType() {
        return "SQOOP";
    }

    @Override
    public List<String> getRoleTypes() {
        return Collections.singletonList("SQOOP_SERVER");
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return getFirstRDSConfigOptional(source).isPresent() && cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }

    private Optional<RDSConfig> getFirstRDSConfigOptional(TemplatePreparationObject source) {
        return source.getRdsConfigs().stream().filter(rds -> DatabaseType.SQOOP.name().equalsIgnoreCase(rds.getType())).findFirst();
    }
}
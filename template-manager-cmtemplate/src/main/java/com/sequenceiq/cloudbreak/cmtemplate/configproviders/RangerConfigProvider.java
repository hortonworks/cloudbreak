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
public class RangerConfigProvider implements CmTemplateComponentConfigProvider {

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(TemplatePreparationObject templatePreparationObject) {
        List<ApiClusterTemplateConfig> result = new ArrayList<>();
        result.add(new ApiClusterTemplateConfig().name("ranger_database_host").variable("ranger-ranger_database_host"));
        result.add(new ApiClusterTemplateConfig().name("ranger_database_name").variable("ranger-ranger_database_name"));
        result.add(new ApiClusterTemplateConfig().name("ranger_database_type").variable("ranger-ranger_database_type"));
        result.add(new ApiClusterTemplateConfig().name("ranger_database_user").variable("ranger-ranger_database_user"));
        result.add(new ApiClusterTemplateConfig().name("ranger_database_user_password").variable("ranger-ranger_database_user_password"));
        return result;
    }

    @Override
    public List<ApiClusterTemplateVariable> getServiceConfigVariables(TemplatePreparationObject source) {
        List<ApiClusterTemplateVariable> result = new ArrayList<>();
        RdsView rangerRdsView = new RdsView(getFirstRDSConfigOptional(source).get());
        result.add(new ApiClusterTemplateVariable().name("ranger-ranger_database_host").value(rangerRdsView.getHost()));
        result.add(new ApiClusterTemplateVariable().name("ranger-ranger_database_name").value(rangerRdsView.getDatabaseName()));
        result.add(new ApiClusterTemplateVariable().name("ranger-ranger_database_type").value(rangerRdsView.getSubprotocol()));
        result.add(new ApiClusterTemplateVariable().name("ranger-ranger_database_user").value(rangerRdsView.getConnectionUserName()));
        result.add(new ApiClusterTemplateVariable().name("ranger-ranger_database_user_password").value(rangerRdsView.getConnectionPassword()));
        return result;
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
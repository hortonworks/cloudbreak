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
public class HueConfigProvider implements CmTemplateComponentConfigProvider {

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(TemplatePreparationObject templatePreparationObject) {
        List<ApiClusterTemplateConfig> result = new ArrayList<>();
        result.add(new ApiClusterTemplateConfig().name("database_host").variable("hue-hue_database_host"));
        result.add(new ApiClusterTemplateConfig().name("database_port").variable("hue-hue_database_port"));
        result.add(new ApiClusterTemplateConfig().name("database_name").variable("hue-hue_database_name"));
        result.add(new ApiClusterTemplateConfig().name("database_type").variable("hue-hue_database_type"));
        result.add(new ApiClusterTemplateConfig().name("database_user").variable("hue-hue_database_user"));
        result.add(new ApiClusterTemplateConfig().name("database_password").variable("hue-hue_database_password"));
        return result;
    }

    @Override
    public List<ApiClusterTemplateVariable> getServiceConfigVariables(TemplatePreparationObject source) {
        List<ApiClusterTemplateVariable> result = new ArrayList<>();
        RdsView hueRdsView = new RdsView(getFirstRDSConfigOptional(source).get());
        result.add(new ApiClusterTemplateVariable().name("hue-hue_database_host").value(hueRdsView.getHost()));
        result.add(new ApiClusterTemplateVariable().name("hue-hue_database_port").value(hueRdsView.getPort()));
        result.add(new ApiClusterTemplateVariable().name("hue-hue_database_name").value(hueRdsView.getDatabaseName()));
        result.add(new ApiClusterTemplateVariable().name("hue-hue_database_type").value(hueRdsView.getSubprotocol()));
        result.add(new ApiClusterTemplateVariable().name("hue-hue_database_user").value(hueRdsView.getConnectionUserName()));
        result.add(new ApiClusterTemplateVariable().name("hue-hue_database_password").value(hueRdsView.getConnectionPassword()));
        return result;
    }

    @Override
    public String getServiceType() {
        return "HUE";
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of("HUE_SERVER", "HUE_LOAD_BALANCER");
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return getFirstRDSConfigOptional(source).isPresent() && cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }

    private Optional<RDSConfig> getFirstRDSConfigOptional(TemplatePreparationObject source) {
        return source.getRdsConfigs().stream().filter(rds -> DatabaseType.HUE.name().equalsIgnoreCase(rds.getType())).findFirst();
    }
}

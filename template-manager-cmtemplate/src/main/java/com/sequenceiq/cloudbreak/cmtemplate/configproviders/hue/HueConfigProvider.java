package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hue;

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

    private static final String HUE_DATABASE_HOST = "hue-hue_database_host";

    private static final String HUE_DATABASE_PORT = "hue-hue_database_port";

    private static final String HUE_DATABASE_NAME = "hue-hue_database_name";

    private static final String HUE_HUE_DATABASE_TYPE = "hue-hue_database_type";

    private static final String HUE_HUE_DATABASE_USER = "hue-hue_database_user";

    private static final String HUE_DATABASE_PASSWORD = "hue-hue_database_password";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(TemplatePreparationObject templatePreparationObject) {
        List<ApiClusterTemplateConfig> result = new ArrayList<>();
        result.add(new ApiClusterTemplateConfig().name("database_host").variable(HUE_DATABASE_HOST));
        result.add(new ApiClusterTemplateConfig().name("database_port").variable(HUE_DATABASE_PORT));
        result.add(new ApiClusterTemplateConfig().name("database_name").variable(HUE_DATABASE_NAME));
        result.add(new ApiClusterTemplateConfig().name("database_type").variable(HUE_HUE_DATABASE_TYPE));
        result.add(new ApiClusterTemplateConfig().name("database_user").variable(HUE_HUE_DATABASE_USER));
        result.add(new ApiClusterTemplateConfig().name("database_password").variable(HUE_DATABASE_PASSWORD));
        return result;
    }

    @Override
    public List<ApiClusterTemplateVariable> getServiceConfigVariables(TemplatePreparationObject source) {
        List<ApiClusterTemplateVariable> result = new ArrayList<>();
        RdsView hueRdsView = new RdsView(getFirstRDSConfigOptional(source).get());
        result.add(new ApiClusterTemplateVariable().name(HUE_DATABASE_HOST).value(hueRdsView.getHost()));
        result.add(new ApiClusterTemplateVariable().name(HUE_DATABASE_PORT).value(hueRdsView.getPort()));
        result.add(new ApiClusterTemplateVariable().name(HUE_DATABASE_NAME).value(hueRdsView.getDatabaseName()));
        result.add(new ApiClusterTemplateVariable().name(HUE_HUE_DATABASE_TYPE).value(hueRdsView.getSubprotocol()));
        result.add(new ApiClusterTemplateVariable().name(HUE_HUE_DATABASE_USER).value(hueRdsView.getConnectionUserName()));
        result.add(new ApiClusterTemplateVariable().name(HUE_DATABASE_PASSWORD).value(hueRdsView.getConnectionPassword()));
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

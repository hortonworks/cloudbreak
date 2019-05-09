package com.sequenceiq.cloudbreak.cmtemplate.configproviders.oozie;

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
public class OozieConfigProvider implements CmTemplateComponentConfigProvider {

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(TemplatePreparationObject templatePreparationObject) {
        List<ApiClusterTemplateConfig> result = new ArrayList<>();
        result.add(new ApiClusterTemplateConfig().name("oozie_database_host").variable("oozie-oozie_database_host"));
        result.add(new ApiClusterTemplateConfig().name("oozie_database_name").variable("oozie-oozie_database_name"));
        result.add(new ApiClusterTemplateConfig().name("oozie_database_type").variable("oozie-oozie_database_type"));
        result.add(new ApiClusterTemplateConfig().name("oozie_database_user").variable("oozie-oozie_database_user"));
        result.add(new ApiClusterTemplateConfig().name("oozie_database_password").variable("oozie-oozie_database_password"));
        return result;
    }

    @Override
    public List<ApiClusterTemplateVariable> getServiceConfigVariables(TemplatePreparationObject source) {
        List<ApiClusterTemplateVariable> result = new ArrayList<>();
        RdsView oozieRdsView = new RdsView(getFirstRDSConfigOptional(source).get());
        result.add(new ApiClusterTemplateVariable().name("oozie-oozie_database_host").value(oozieRdsView.getHost()));
        result.add(new ApiClusterTemplateVariable().name("oozie-oozie_database_name").value(oozieRdsView.getDatabaseName()));
        result.add(new ApiClusterTemplateVariable().name("oozie-oozie_database_type").value(oozieRdsView.getSubprotocol()));
        result.add(new ApiClusterTemplateVariable().name("oozie-oozie_database_user").value(oozieRdsView.getConnectionUserName()));
        result.add(new ApiClusterTemplateVariable().name("oozie-oozie_database_password").value(oozieRdsView.getConnectionPassword()));
        return result;
    }

    @Override
    public String getServiceType() {
        return "OOZIE";
    }

    @Override
    public List<String> getRoleTypes() {
        return Collections.singletonList("OOZIE_SERVER");
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return getFirstRDSConfigOptional(source).isPresent() && cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }

    private Optional<RDSConfig> getFirstRDSConfigOptional(TemplatePreparationObject source) {
        return source.getRdsConfigs().stream().filter(rds -> DatabaseType.OOZIE.name().equalsIgnoreCase(rds.getType())).findFirst();
    }
}
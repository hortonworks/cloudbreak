package com.sequenceiq.cloudbreak.cmtemplate.configproviders.nifi;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.service.ExposedServiceCollector;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class NifiKnoxRoleConfigProvider extends AbstractRoleConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(NifiKnoxRoleConfigProvider.class);

    @Inject
    private ExposedServiceCollector exposedServiceCollector;

    @Override
    public List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        LOGGER.info("add property values for NifiKnoxRoleConfigProvider");
        return new ArrayList<>();
    }

    @Override
    public String getServiceType() {
        return "NIFI";
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of("NIFI_NODE");
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return Objects.nonNull(source.getGatewayView())
                && Objects.nonNull(source.getGatewayView().getExposedServices())
                && source.getGatewayView().getExposedServices().contains(exposedServiceCollector.getNiFiService().getKnoxService());
    }

}

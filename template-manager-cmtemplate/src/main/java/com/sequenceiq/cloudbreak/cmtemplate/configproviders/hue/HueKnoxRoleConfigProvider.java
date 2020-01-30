package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hue;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.service.ExposedServiceCollector;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class HueKnoxRoleConfigProvider implements CmTemplateComponentConfigProvider {

    private static final String AUTH_BACKEND = "auth_backend";

    private static final String KNOX_AUTH_BACKED = "desktop.auth.backend.KnoxSpnegoDjangoBackend";

    @Inject
    private ExposedServiceCollector exposedServiceCollector;

    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        return List.of(config(AUTH_BACKEND, KNOX_AUTH_BACKED));
    }

    @Override
    public String getServiceType() {
        return "HUE";
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(HueRoles.HUE_LOAD_BALANCER);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return Objects.nonNull(source.getGatewayView())
                && Objects.nonNull(source.getGatewayView().getExposedServices())
                && source.getGatewayView().getExposedServices().contains(exposedServiceCollector.getHueService().getKnoxService());
    }

}

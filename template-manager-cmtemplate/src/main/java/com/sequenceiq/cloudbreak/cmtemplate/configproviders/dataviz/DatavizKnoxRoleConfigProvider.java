package com.sequenceiq.cloudbreak.cmtemplate.configproviders.dataviz;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class DatavizKnoxRoleConfigProvider implements CmTemplateComponentConfigProvider {

    private static final String AUTH_BACKEND = "AUTHENTICATION_BACKENDS";

    private static final String KNOX_AUTH_BACKED = "arcwebbase.backends.KnoxSpnegoDjangoBackend";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        return List.of(config(AUTH_BACKEND, KNOX_AUTH_BACKED));
    }

    @Override
    public String getServiceType() {
        return DatavizRoles.DATAVIZ;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(DatavizRoles.DATAVIZ_WEBSERVER);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return Objects.nonNull(source.getGatewayView())
                && Objects.nonNull(source.getGatewayView().getExposedServices());
    }
}

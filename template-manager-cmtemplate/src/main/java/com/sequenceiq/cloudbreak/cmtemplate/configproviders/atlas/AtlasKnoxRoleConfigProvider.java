package com.sequenceiq.cloudbreak.cmtemplate.configproviders.atlas;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ExposedService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class AtlasKnoxRoleConfigProvider extends AbstractRoleConfigProvider {

    private static final String ATLAS_KNOX_CONFIG =  "atlas.authentication.method.trustedproxy=true"
            + "atlas.proxyuser.knox.hosts=*"
            + "atlas.proxyuser.knox.users=*"
            + "atlas.proxyuser.knox.groups=*";

    private static final String ATLAS_CONFIG_SAFETY_VALVE = "conf/atlas-application.properties_role_safety_valve";

    @Override
    public List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        return List.of(config(ATLAS_CONFIG_SAFETY_VALVE, ATLAS_KNOX_CONFIG));
    }

    @Override
    public String getServiceType() {
        return "ATLAS";
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of("ATLAS_SERVER");
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return Objects.nonNull(source.getGatewayView())
                && Objects.nonNull(source.getGatewayView().getExposedServices())
                && source.getGatewayView().getExposedServices().getValue().contains(ExposedService.ATLAS.getKnoxService());
    }

}

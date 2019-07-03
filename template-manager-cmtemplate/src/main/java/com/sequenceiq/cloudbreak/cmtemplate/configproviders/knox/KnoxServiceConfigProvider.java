package com.sequenceiq.cloudbreak.cmtemplate.configproviders.knox;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class KnoxServiceConfigProvider implements CmTemplateComponentConfigProvider {

    private static final String SAFETY_VALVE = "conf/gateway-site.xml_service_safety_valve";

    private static final String DEFAULT_PROXY_NAME = "cdp-proxy";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject templatePreparationObject) {
        String topologyName = Objects.nonNull(templatePreparationObject.getGatewayView())
                ? templatePreparationObject.getGatewayView().getTopologyName()
                : DEFAULT_PROXY_NAME;
        return List.of(
                config(SAFETY_VALVE, ConfigUtils.getSafetyValveProperty("default.app.topology.name", topologyName)));
    }

    @Override
    public String getServiceType() {
        return KnoxRoles.KNOX;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(KnoxRoles.KNOX_GATEWAY);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return Objects.nonNull(source.getGatewayView())
                && Objects.nonNull(source.getGatewayView().getExposedServices());
    }

}

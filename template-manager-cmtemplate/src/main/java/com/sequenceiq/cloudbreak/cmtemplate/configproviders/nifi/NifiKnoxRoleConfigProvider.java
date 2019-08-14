package com.sequenceiq.cloudbreak.cmtemplate.configproviders.nifi;

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
public class NifiKnoxRoleConfigProvider extends AbstractRoleConfigProvider {

    private static final String PROXY_CONTEXT_PATH = "nifi.web.proxy.context.path";

    @Override
    public List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        String clusterName = source.getGeneralClusterConfigs().getClusterName();
        String topologyName = source.getGatewayView().getTopologyName();
        return List.of(config(PROXY_CONTEXT_PATH, String.format("%s/%s/nifi-app", clusterName, topologyName)));
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
                && source.getGatewayView().getExposedServices().contains(ExposedService.NIFI.getKnoxService());
    }

}

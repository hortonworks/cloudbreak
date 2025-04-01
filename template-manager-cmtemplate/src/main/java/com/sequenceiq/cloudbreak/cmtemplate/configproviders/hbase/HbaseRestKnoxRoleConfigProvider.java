package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hbase;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.List;
import java.util.Objects;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.service.ExposedServiceCollector;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class HbaseRestKnoxRoleConfigProvider extends AbstractRoleConfigProvider {

    private static final String CONFIG_SAFETY_VALVE = "hbase_restserver_config_safety_valve";

    private static final String SUPPORT_PROXYUSER = "hbase.rest.support.proxyuser";

    @Inject
    private ExposedServiceCollector exposedServiceCollector;

    @Override
    public List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        return List.of(
                config(CONFIG_SAFETY_VALVE,
                        ConfigUtils.getSafetyValveProperty(SUPPORT_PROXYUSER, "true")));
    }

    @Override
    public String getServiceType() {
        return "HBASE";
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(HbaseRoles.HBASERESTSERVER);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return Objects.nonNull(source.getGatewayView())
                && Objects.nonNull(source.getGatewayView().getExposedServices())
                && source.getGatewayView().getExposedServices().contains(exposedServiceCollector.getHBaseRestService().getKnoxService());
    }

}

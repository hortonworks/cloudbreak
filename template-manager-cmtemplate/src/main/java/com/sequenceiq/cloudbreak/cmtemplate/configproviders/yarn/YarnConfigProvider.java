package com.sequenceiq.cloudbreak.cmtemplate.configproviders.yarn;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ExposedService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveRoles;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class YarnConfigProvider implements CmTemplateComponentConfigProvider {

    private static final String YARN_SITE_SERVICE_SAFETY_VALVE = "yarn_service_config_safety_valve";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject templatePreparationObject) {
        List<ApiClusterTemplateConfig> apiClusterTemplateConfigs = Lists.newArrayList();
        if (templateProcessor.getServiceByType(HiveRoles.HIVELLAP).isPresent()) {
            apiClusterTemplateConfigs.add(config(YARN_SITE_SERVICE_SAFETY_VALVE, getYarnSiteServiceValveValue()));
        }
        return apiClusterTemplateConfigs;
    }

    @Override
    public String getServiceType() {
        return YarnRoles.YARN;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(YarnRoles.YARN);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return Objects.nonNull(source.getGatewayView())
                && Objects.nonNull(source.getGatewayView().getExposedServices())
                && source.getGatewayView().getExposedServices().contains(ExposedService.RESOURCEMANAGER_WEB.getKnoxService());
    }

    private String getYarnSiteServiceValveValue() {
        return ConfigUtils.getSafetyValveProperty("yarn.resourcemanager.monitor.capacity.preemption.intra-queue-preemption.enabled", "true")
                + ConfigUtils.getSafetyValveProperty("yarn.scheduler.capacity.ordering-policy.priority-utilization.underutilized-preemption.enabled", "true")
                + ConfigUtils.getSafetyValveProperty("yarn.resourcemanager.placement-constraints.handler", "scheduler");
    }

}

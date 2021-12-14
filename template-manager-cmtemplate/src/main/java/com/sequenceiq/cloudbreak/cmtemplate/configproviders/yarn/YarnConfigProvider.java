package com.sequenceiq.cloudbreak.cmtemplate.configproviders.yarn;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider.LOG4J2_FORMAT_MSG_NO_LOOKUPS;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.service.ExposedServiceCollector;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveRoles;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class YarnConfigProvider implements CmTemplateComponentConfigProvider {

    private static final String YARN_SITE_SERVICE_SAFETY_VALVE = "yarn_service_config_safety_valve";

    private static final String MAPREDUCE_MAP_JAVA_OPTS = "mapreduce_map_java_opts";

    private static final String MAPREDUCE_REDUCE_JAVA_OPTS = "mapreduce_reduce_java_opts";

    @Inject
    private ExposedServiceCollector exposedServiceCollector;

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject templatePreparationObject) {
        List<ApiClusterTemplateConfig> apiClusterTemplateConfigs = Lists.newArrayList();
        if (templateProcessor.getServiceByType(HiveRoles.HIVELLAP).isPresent()) {
            apiClusterTemplateConfigs.add(config(YARN_SITE_SERVICE_SAFETY_VALVE, getYarnSiteServiceValveValue()));
        }
        apiClusterTemplateConfigs.add(config(MAPREDUCE_MAP_JAVA_OPTS, LOG4J2_FORMAT_MSG_NO_LOOKUPS));
        apiClusterTemplateConfigs.add(config(MAPREDUCE_REDUCE_JAVA_OPTS, LOG4J2_FORMAT_MSG_NO_LOOKUPS));
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
                && source.getGatewayView().getExposedServices().contains(exposedServiceCollector.getResourceManagerWebService().getKnoxService());
    }

    private String getYarnSiteServiceValveValue() {
        return ConfigUtils.getSafetyValveProperty("yarn.resourcemanager.monitor.capacity.preemption.intra-queue-preemption.enabled", "true")
                + ConfigUtils.getSafetyValveProperty("yarn.scheduler.capacity.ordering-policy.priority-utilization.underutilized-preemption.enabled", "true")
                + ConfigUtils.getSafetyValveProperty("yarn.resourcemanager.placement-constraints.handler", "scheduler");
    }

}

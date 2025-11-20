package com.sequenceiq.cloudbreak.cmtemplate.configproviders.yarn;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.List;
import java.util.Objects;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.service.ExposedServiceCollector;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsConfigHelper;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveRoles;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class YarnConfigProvider extends AbstractRoleConfigProvider {

    private static final String YARN_SITE_SERVICE_SAFETY_VALVE = "yarn_service_config_safety_valve";

    private static final String MAPREDUCE_CLIENT_ENV_SAFETY_VALVE = "mapreduce_client_env_safety_valve";

    private static final String HADOOP_OPTS = "HADOOP_OPTS=\"-Dorg.wildfly.openssl.path=/usr/lib64 ${HADOOP_OPTS}\"";

    private static final String MR_APPLICATION_FRAMEWORK_PATH = "mapreduce_application_framework_path";

    private static final String DEFAULT_MR_APPLICATION_FRAMEWORK_PATH = "/user/yarn/mapreduce/mr-framework/{version}-mr-framework.tar.gz#mr-framework";

    @Inject
    private ExposedServiceCollector exposedServiceCollector;

    @Inject
    private HdfsConfigHelper hdfsConfigHelper;

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject templatePreparationObject) {
        List<ApiClusterTemplateConfig> apiClusterTemplateConfigs = Lists.newArrayList();
        if (templateProcessor.getServiceByType(HiveRoles.HIVELLAP).isPresent()) {
            apiClusterTemplateConfigs.add(config(YARN_SITE_SERVICE_SAFETY_VALVE, getYarnSiteServiceValveValue()));
        }
        return apiClusterTemplateConfigs;
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        switch (roleType) {
            case YarnRoles.GATEWAY:
                List<ApiClusterTemplateConfig> roleConfigs = Lists.newArrayList();
                roleConfigs.add(config(MAPREDUCE_CLIENT_ENV_SAFETY_VALVE, HADOOP_OPTS));
                if (templateProcessor.isHybridDatahub(source)) {
                    hdfsConfigHelper.getHdfsUrl(templateProcessor, source)
                            .ifPresent(hdfsUrl -> roleConfigs.add(config(MR_APPLICATION_FRAMEWORK_PATH, hdfsUrl + DEFAULT_MR_APPLICATION_FRAMEWORK_PATH)));
                }
                return roleConfigs;
            default:
                return List.of();
        }
    }

    @Override
    public String getServiceType() {
        return YarnRoles.YARN;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(YarnRoles.YARN, YarnRoles.GATEWAY);
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

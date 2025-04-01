package com.sequenceiq.cloudbreak.cmtemplate.configproviders.yarn;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CDPD_VERSION_7_2_11;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.yarn.YarnConstants.ATTRIBUTE_NAME_NODE_INSTANCE_TYPE;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.yarn.YarnConstants.ATTRIBUTE_NODE_INSTANCE_TYPE_COMPUTE;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.yarn.YarnConstants.ATTRIBUTE_NODE_INSTANCE_TYPE_WORKER;

import java.util.LinkedList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveRoles;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class YarnResourceManagerRoleConfigProvider extends AbstractRoleConfigProvider {

    private static final String CAPACITY_SCHEDULER_SAFETY_VALVE = "resourcemanager_capacity_scheduler_configuration";

    private static final String CAPACITY_SCHEDULER_CLASS = "yarn_resourcemanager_scheduler_class";

    private static final String RESOURCE_MANAGER_YARN_SITE_SAFETY_VALVE = "resourcemanager_config_safety_valve";

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        switch (roleType) {
            case YarnRoles.RESOURCEMANAGER:
                List<ApiClusterTemplateConfig> configs = new LinkedList<>();
                if (isContainerPlacementConfigRequired(source)) {
                    configs.add(config(RESOURCE_MANAGER_YARN_SITE_SAFETY_VALVE, getYarnSiteContainerPlacementSafetyValveValues()));
                }
                if (isLlapBasedConfigNeeded((CmTemplateProcessor) source.getBlueprintView().getProcessor())) {
                    configs.add(config(CAPACITY_SCHEDULER_SAFETY_VALVE, ConfigUtils.getSafetyValveConfiguration(getCapacitySchedulerValveValue())));
                    configs.add(config(CAPACITY_SCHEDULER_CLASS, "org.apache.hadoop.yarn.server.resourcemanager.scheduler.capacity.CapacityScheduler"));
                }
                return configs;

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
        return List.of(YarnRoles.RESOURCEMANAGER);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return isLlapBasedConfigNeeded(cmTemplateProcessor) || isContainerPlacementConfigRequired(source);
    }

    private boolean isLlapBasedConfigNeeded(CmTemplateProcessor cmTemplateProcessor) {
        return cmTemplateProcessor.getServiceByType(HiveRoles.HIVELLAP).isPresent()
                && cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }

    private String getCapacitySchedulerValveValue() {
        return ConfigUtils.getSafetyValveProperty("yarn.scheduler.capacity.default.minimum-user-limit-percent", "100")
                + ConfigUtils.getSafetyValveProperty("yarn.scheduler.capacity.maximum-am-resource-percent", "0.2")
                + ConfigUtils.getSafetyValveProperty("yarn.scheduler.capacity.maximum-applications", "10000")
                + ConfigUtils.getSafetyValveProperty("yarn.scheduler.capacity.node-locality-delay", "40")
                + ConfigUtils.getSafetyValveProperty("yarn.scheduler.capacity.root.accessible-node-labels", "*")
                + ConfigUtils.getSafetyValveProperty("yarn.scheduler.capacity.root.acl_administer_queue", "*")
                + ConfigUtils.getSafetyValveProperty("yarn.scheduler.capacity.root.acl_submit_applications", "*")
                + ConfigUtils.getSafetyValveProperty("yarn.scheduler.capacity.root.capacity", "100")
                + ConfigUtils.getSafetyValveProperty("yarn.scheduler.capacity.root.default.acl_administer_jobs", "*")
                + ConfigUtils.getSafetyValveProperty("yarn.scheduler.capacity.root.default.acl_submit_applications", "*")
                + ConfigUtils.getSafetyValveProperty("yarn.scheduler.capacity.root.default.capacity", "50.0")
                + ConfigUtils.getSafetyValveProperty("yarn.scheduler.capacity.root.default.maximum-capacity", "50.0")
                + ConfigUtils.getSafetyValveProperty("yarn.scheduler.capacity.root.default.state", "RUNNING")
                + ConfigUtils.getSafetyValveProperty("yarn.scheduler.capacity.root.default.user-limit-factor", "1")
                + ConfigUtils.getSafetyValveProperty("yarn.scheduler.capacity.root.llap.acl_administer_queue", "hive")
                + ConfigUtils.getSafetyValveProperty("yarn.scheduler.capacity.root.llap.acl_submit_applications", "hive")
                + ConfigUtils.getSafetyValveProperty("yarn.scheduler.capacity.root.llap.capacity", "50.0")
                + ConfigUtils.getSafetyValveProperty("yarn.scheduler.capacity.root.llap.maximum-am-resource-percent", "1")
                + ConfigUtils.getSafetyValveProperty("yarn.scheduler.capacity.root.llap.maximum-capacity", "50.0")
                + ConfigUtils.getSafetyValveProperty("yarn.scheduler.capacity.root.llap.minimum-user-limit-percent", "100")
                + ConfigUtils.getSafetyValveProperty("yarn.scheduler.capacity.root.llap.ordering-policy", "fifo")
                + ConfigUtils.getSafetyValveProperty("yarn.scheduler.capacity.root.llap.state", "RUNNING")
                + ConfigUtils.getSafetyValveProperty("yarn.scheduler.capacity.root.llap.user-limit-factor", "1")
                + ConfigUtils.getSafetyValveProperty("yarn.scheduler.capacity.root.queues", "default,llap")
                + ConfigUtils.getSafetyValveProperty("yarn.scheduler.capacity.schedule-asynchronously.enable", "true")
                + ConfigUtils.getSafetyValveProperty("yarn.scheduler.capacity.schedule-asynchronously.maximum-threads", "1")
                + ConfigUtils.getSafetyValveProperty("yarn.scheduler.capacity.schedule-asynchronously.scheduling-interval-ms", "10");
    }

    private boolean isContainerPlacementConfigRequired(TemplatePreparationObject source) {
        String cdpdVersion = source.getBlueprintView().getProcessor().getStackVersion() == null ?
                "" : source.getBlueprintView().getProcessor().getStackVersion();
        return isVersionNewerOrEqualThanLimited(cdpdVersion, CDPD_VERSION_7_2_11);
    }

    private String getYarnSiteContainerPlacementSafetyValveValues() {
        return
                ConfigUtils.getSafetyValveProperty("yarn.resourcemanager.am.placement-preference-with-node-attributes",
                        String.format("ORDER NODES IN %s WITH %s > %s", ATTRIBUTE_NAME_NODE_INSTANCE_TYPE,
                                ATTRIBUTE_NODE_INSTANCE_TYPE_WORKER, ATTRIBUTE_NODE_INSTANCE_TYPE_COMPUTE))
                        + ConfigUtils.getSafetyValveProperty("yarn.resourcemanager.non-am.placement-preference-with-node-attributes",
                        String.format("ORDER NODES IN %s WITH %s > %s", ATTRIBUTE_NAME_NODE_INSTANCE_TYPE,
                                ATTRIBUTE_NODE_INSTANCE_TYPE_COMPUTE, ATTRIBUTE_NODE_INSTANCE_TYPE_WORKER));

    }
}
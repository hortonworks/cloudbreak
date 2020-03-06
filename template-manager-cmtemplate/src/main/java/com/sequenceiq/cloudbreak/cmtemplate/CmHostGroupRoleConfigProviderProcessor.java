package com.sequenceiq.cloudbreak.cmtemplate;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateHostTemplate;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Component
public class CmHostGroupRoleConfigProviderProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmHostGroupRoleConfigProviderProcessor.class);

    @Inject
    private List<CmHostGroupRoleConfigProvider> providers;

    public void process(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        if (!getHostTemplates(templateProcessor).isEmpty()) {
            updateConfigsInTemplate(templateProcessor, generateConfigs(templateProcessor, source));
        }
    }

    private List<ApiClusterTemplateHostTemplate> getHostTemplates(CmTemplateProcessor templateProcessor) {
        return ofNullable(templateProcessor.getTemplate().getHostTemplates()).orElseGet(List::of);
    }

    private Map<String, Map<String, List<ApiClusterTemplateConfig>>> generateConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        Map<String, Map<String, List<ApiClusterTemplateConfig>>> configsByRoleConfigGroup = new HashMap<>();

        Map<String, HostgroupView> hostGroups = source.getHostgroupViews().stream()
                .collect(toMap(HostgroupView::getName, Function.identity()));
        List<ApiClusterTemplateHostTemplate> hostTemplates = getHostTemplates(templateProcessor);
        Map<String, ServiceComponent> serviceComponents = templateProcessor.mapRoleRefsToServiceComponents();

        for (ApiClusterTemplateHostTemplate hostTemplate : hostTemplates) {
            String hostGroupName = hostTemplate.getRefName();
            List<String> roleConfigGroups = ofNullable(hostTemplate.getRoleConfigGroupsRefNames()).orElseGet(List::of);
            HostgroupView hostgroupView = hostGroups.get(hostGroupName);

            for (String roleConfigGroup : roleConfigGroups) {
                for (CmHostGroupRoleConfigProvider provider : providers) {
                    ServiceComponent serviceComponent = serviceComponents.get(roleConfigGroup);

                    if (serviceComponent != null
                            && Objects.equals(provider.getServiceType(), serviceComponent.getService())
                            && provider.getRoleTypes().contains(serviceComponent.getComponent())) {

                        configsByRoleConfigGroup.computeIfAbsent(roleConfigGroup, __ -> new HashMap<>())
                                .computeIfAbsent(hostGroupName, __ -> new ArrayList<>())
                                .addAll(provider.getRoleConfigs(serviceComponent.getComponent(), hostgroupView, source));
                    }
                }
            }
        }
        return configsByRoleConfigGroup;
    }

    private void updateConfigsInTemplate(CmTemplateProcessor templateProcessor, Map<String, Map<String, List<ApiClusterTemplateConfig>>> newConfigsByRCG) {
        Map<String, ApiClusterTemplateRoleConfigGroup> roleConfigGroupByName = templateProcessor.getTemplate().getServices().stream()
                .flatMap(service -> ofNullable(service.getRoleConfigGroups()).orElseGet(List::of).stream())
                .collect(toMap(ApiClusterTemplateRoleConfigGroup::getRefName, Function.identity()));

        newConfigsByRCG.forEach((configGroupName, configsByHostGroup) -> {
            ApiClusterTemplateRoleConfigGroup configGroup = roleConfigGroupByName.get(configGroupName);
            configsByHostGroup.values().forEach(configs -> templateProcessor.mergeRoleConfigs(configGroup, configs));
        });
    }

    private ApiClusterTemplateRoleConfigGroup copyForHostGroup(ApiClusterTemplateRoleConfigGroup original, String hostGroupName) {
        String displayName = original.getDisplayName() != null ? (original.getDisplayName() + " in host group " + hostGroupName) : null;
        String refName = original.getRefName().replaceAll("[^-]+$", hostGroupName);
        return new ApiClusterTemplateRoleConfigGroup()
                .base(false)
                // no need to copy here, as CmTemplateProcessor#mergeRoleConfigs creates a new list if needed
                .configs(original.getConfigs())
                .displayName(displayName)
                .refName(refName)
                .roleType(original.getRoleType());
    }

}

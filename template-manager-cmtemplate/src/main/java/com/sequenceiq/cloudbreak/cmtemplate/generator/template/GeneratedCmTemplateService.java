package com.sequenceiq.cloudbreak.cmtemplate.generator.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.model.ApiClusterTemplateHostTemplate;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.cmtemplate.generator.configuration.CmTemplateGeneratorConfigurationResolver;
import com.sequenceiq.cloudbreak.cmtemplate.generator.configuration.domain.dependencies.ServiceConfig;
import com.sequenceiq.cloudbreak.cmtemplate.generator.template.domain.GeneratedCmTemplate;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

@Service
public class GeneratedCmTemplateService {

    @Inject
    private CmTemplateGeneratorConfigurationResolver resolver;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    public GeneratedCmTemplate prepareClouderaManagerTemplate(Set<String> services, String stackType, String version, String uuid) {

        CmTemplateProcessor processor = initiateTemplate();

        Set<ServiceConfig> serviceConfigs = collectServiceConfigs(services);
        Map<String, Set<String>> hostServiceMap = new HashMap<>();

        prepareCdhVersion(stackType, version, processor);
        processor.setDisplayName(prepareDisplayName(uuid));
        processor.setServices(prepareApiClusterTemplateServices(serviceConfigs, hostServiceMap));
        processor.setHostTemplates(prepareApiClusterTemplateHostTemplates(hostServiceMap));

        return new GeneratedCmTemplate(prepareTemplate(processor));
    }

    private String prepareTemplate(CmTemplateProcessor processor) {
        return JsonUtil.writeValueAsStringSilent(processor.getTemplate(), true);
    }

    private CmTemplateProcessor initiateTemplate() {
        return cmTemplateProcessorFactory.get("{}");
    }

    private String prepareDisplayName(String uuid) {
        return "cloudbreak-generated-" + uuid;
    }

    private void prepareCdhVersion(String stackType, String version, CmTemplateProcessor processor) {
        if ("CDH".equals(stackType)) {
            processor.setCdhVersion(version);
        }
    }

    private Set<ServiceConfig> collectServiceConfigs(Set<String> services) {
        Set<ServiceConfig> serviceConfigs = new HashSet<>();
        for (String service : services) {
            for (ServiceConfig serviceInformation : resolver.serviceConfigs()) {
                if (serviceInformation.getName().equals(service.toUpperCase(Locale.ROOT))) {
                    serviceConfigs.add(serviceInformation);
                }
            }
        }
        return serviceConfigs;
    }

    private List<ApiClusterTemplateService> prepareApiClusterTemplateServices(Set<ServiceConfig> serviceConfigs, Map<String, Set<String>> hostServiceMap) {
        List<ApiClusterTemplateService> clusterTemplateServices = new ArrayList<>();
        for (ServiceConfig serviceConfig : serviceConfigs) {
            String serviceName = serviceConfig.getName();
            String lowerCaseServiceName = serviceName.toLowerCase();

            ApiClusterTemplateService apiClusterTemplateService = new ApiClusterTemplateService();
            apiClusterTemplateService.setRefName(lowerCaseServiceName);
            apiClusterTemplateService.setServiceType(serviceName);
            apiClusterTemplateService.setRoleConfigGroups(new ArrayList<>());

            serviceConfig.getComponents().forEach(component -> {
                Set<ApiClusterTemplateRoleConfigGroup> roleConfigGroups = new HashSet<>();
                component.getGroups().forEach(group -> {

                    String componentName = component.getName();
                    boolean base = component.getGroups().size() == 1 || component.isBase();
                    String hostServiceNameEnd = base ? "BASE" : group.toUpperCase(Locale.ROOT);
                    String hostServiceName = String.format("%s-%s-%s", lowerCaseServiceName, component.getName().toUpperCase(Locale.ROOT), hostServiceNameEnd);

                    ApiClusterTemplateRoleConfigGroup apiClusterTemplateRoleConfigGroup = new ApiClusterTemplateRoleConfigGroup();
                    apiClusterTemplateRoleConfigGroup.setRoleType(componentName.toUpperCase(Locale.ROOT));
                    apiClusterTemplateRoleConfigGroup.setRefName(hostServiceName);
                    apiClusterTemplateRoleConfigGroup.setBase(base);

                    if (hostServiceMap.keySet().contains(group)) {
                        hostServiceMap.get(group).add(hostServiceName);
                    } else {
                        hostServiceMap.put(group, Sets.newHashSet(hostServiceName));
                    }

                    roleConfigGroups.add(apiClusterTemplateRoleConfigGroup);
                });

                apiClusterTemplateService.getRoleConfigGroups().addAll(roleConfigGroups);
            });
            clusterTemplateServices.add(apiClusterTemplateService);
        }
        return clusterTemplateServices;
    }

    private List<ApiClusterTemplateHostTemplate> prepareApiClusterTemplateHostTemplates(Map<String, Set<String>> hostServiceMap) {
        List<ApiClusterTemplateHostTemplate> hostTemplates = new ArrayList<>();
        hostServiceMap.forEach((key, roleConfigRefNames) -> {
            ApiClusterTemplateHostTemplate apiClusterTemplateHostTemplate = new ApiClusterTemplateHostTemplate();
            apiClusterTemplateHostTemplate.setRefName(key);
            apiClusterTemplateHostTemplate.setRoleConfigGroupsRefNames(new ArrayList<>());
            apiClusterTemplateHostTemplate.getRoleConfigGroupsRefNames().addAll(roleConfigRefNames);
            hostTemplates.add(apiClusterTemplateHostTemplate);
        });
        return hostTemplates;
    }
}

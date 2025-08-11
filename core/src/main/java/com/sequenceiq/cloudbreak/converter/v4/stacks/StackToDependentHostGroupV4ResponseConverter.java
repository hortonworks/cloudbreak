package com.sequenceiq.cloudbreak.converter.v4.stacks;

import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.DependentHostGroupsV4Response;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.stack.DependentRolesHealthCheckService;

@Component
public class StackToDependentHostGroupV4ResponseConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackToDependentHostGroupV4ResponseConverter.class);

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Inject
    private DependentRolesHealthCheckService dependentRolesHealthCheckService;

    public DependentHostGroupsV4Response convert(StackDto stack, Set<String> hostGroups) {
        Map<String, Set<String>> dependentHostGroupsForHostGroup = Maps.newHashMap();
        Map<String, Set<String>> dependentComponentsForHostGroup = Maps.newHashMap();
        if (stack.getBlueprint() != null && StringUtils.isNotEmpty(stack.getBlueprintJsonText())) {
            LOGGER.debug("Adding dependent hostgroups with roles health to response");
            CmTemplateProcessor processor = cmTemplateProcessorFactory.get(stack.getBlueprintJsonText());
            hostGroups.forEach(hg -> dependentHostGroupsForHostGroup.put(hg,
                    dependentRolesHealthCheckService.getDependentHostGroupsForHostGroup(processor, hg)));
            hostGroups.forEach(hg -> dependentComponentsForHostGroup.put(hg,
                    dependentRolesHealthCheckService.getDependentComponentsForHostGroup(processor, hg)));
        } else {
            LOGGER.info("No blueprint for stack: '{}', returning with empty response", stack.getName());
        }
        DependentHostGroupsV4Response dependentHostGroupsResponse = new DependentHostGroupsV4Response();
        dependentHostGroupsResponse.setDependentHostGroups(dependentHostGroupsForHostGroup);
        dependentHostGroupsResponse.setDependentComponents(dependentComponentsForHostGroup);
        return dependentHostGroupsResponse;
    }
}

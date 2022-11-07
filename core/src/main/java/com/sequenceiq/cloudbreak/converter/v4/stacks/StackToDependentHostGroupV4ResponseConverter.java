package com.sequenceiq.cloudbreak.converter.v4.stacks;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.DependentHostGroupsV4Response;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.stack.DependentRolesHealthCheckService;

@Component
public class StackToDependentHostGroupV4ResponseConverter {

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Inject
    private DependentRolesHealthCheckService dependentRolesHealthCheckService;

    public DependentHostGroupsV4Response convert(StackDto stack, Set<String> hostGroups) {
        CmTemplateProcessor processor = cmTemplateProcessorFactory.get(stack.getBlueprint().getBlueprintText());
        Map<String, Set<String>> dependentHostGroupsForHostGroup = Maps.newHashMap();
        DependentHostGroupsV4Response dependentHostGroupsResponse = new DependentHostGroupsV4Response();
        hostGroups.forEach(hg -> dependentHostGroupsForHostGroup.put(hg, dependentRolesHealthCheckService.getDependentHostGroupsForHostGroup(processor, hg)));
        dependentHostGroupsResponse.setDependentHostGroups(dependentHostGroupsForHostGroup);
        return dependentHostGroupsResponse;
    }
}

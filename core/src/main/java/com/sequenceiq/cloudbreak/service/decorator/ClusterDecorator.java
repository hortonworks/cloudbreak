package com.sequenceiq.cloudbreak.service.decorator;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.model.HostGroupJson;
import com.sequenceiq.cloudbreak.controller.validation.blueprint.BlueprintValidator;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class ClusterDecorator implements Decorator<Cluster> {

    private enum DecorationData {
        STACK_ID,
        BLUEPRINT_ID,
        HOSTGROUP_JSONS,
        VALIDATE_BLUEPRINT
    }

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private BlueprintValidator blueprintValidator;

    @Inject
    private StackService stackService;

    @Inject
    private ConversionService conversionService;

    @Inject
    private HostGroupDecorator hostGroupDecorator;

    @Override
    public Cluster decorate(Cluster subject, Object... data) {
        if (null == data || data.length != DecorationData.values().length) {
            throw new IllegalArgumentException("Invalid decoration data provided. Cluster: " + subject.getName());
        }
        Long stackId = (Long) data[DecorationData.STACK_ID.ordinal()];
        Long blueprintId = (Long) data[DecorationData.BLUEPRINT_ID.ordinal()];
        Set<HostGroupJson> hostGroupsJsons = (Set<HostGroupJson>) data[DecorationData.HOSTGROUP_JSONS.ordinal()];
        subject.setBlueprint(blueprintService.get(blueprintId));
        subject.setHostGroups(convertHostGroupsFromJson(stackId, subject, hostGroupsJsons));
        boolean validate = (boolean) data[DecorationData.VALIDATE_BLUEPRINT.ordinal()];
        if (validate) {
            Blueprint blueprint = blueprintService.get(blueprintId);
            blueprintValidator.validateBlueprintForStack(blueprint, subject.getHostGroups(), stackService.get(stackId).getInstanceGroups());
        }
        return subject;
    }

    private Set<HostGroup> convertHostGroupsFromJson(Long stackId, Cluster cluster, Set<HostGroupJson> hostGroupsJsons) {
        Set<HostGroup> hostGroups = new HashSet<>();
        for (HostGroupJson json : hostGroupsJsons) {
            HostGroup hostGroup = conversionService.convert(json, HostGroup.class);
            hostGroup.setCluster(cluster);
            hostGroup = hostGroupDecorator.decorate(hostGroup, stackId, json.getInstanceGroupName(), json.getRecipeIds(), true);
            hostGroups.add(hostGroup);
        }
        return hostGroups;
    }

}

package com.sequenceiq.cloudbreak.service.decorator;

import javax.inject.Inject;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.model.HostGroupJson;
import com.sequenceiq.cloudbreak.controller.validation.blueprint.BlueprintValidator;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.SssdConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.sssdconfig.SssdConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

@Component
public class ClusterDecorator implements Decorator<Cluster> {

    private enum DecorationData {
        STACK_ID,
        USER,
        BLUEPRINT_ID,
        HOSTGROUP_JSONS,
        VALIDATE_BLUEPRINT,
        SSSDCONFIG_ID
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

    @Inject
    private SssdConfigService sssdConfigService;

    @Override
    public Cluster decorate(Cluster subject, Object... data) {
        if (null == data || data.length != DecorationData.values().length) {
            throw new IllegalArgumentException("Invalid decoration data provided. Cluster: " + subject.getName());
        }
        Long stackId = (Long) data[DecorationData.STACK_ID.ordinal()];
        CbUser user = (CbUser) data[DecorationData.USER.ordinal()];
        Long blueprintId = (Long) data[DecorationData.BLUEPRINT_ID.ordinal()];
        Set<HostGroupJson> hostGroupsJsons = (Set<HostGroupJson>) data[DecorationData.HOSTGROUP_JSONS.ordinal()];
        subject.setBlueprint(blueprintService.get(blueprintId));
        subject.setHostGroups(convertHostGroupsFromJson(stackId, user, subject, hostGroupsJsons));
        boolean validate = (boolean) data[DecorationData.VALIDATE_BLUEPRINT.ordinal()];
        if (validate) {
            Blueprint blueprint = blueprintService.get(blueprintId);
            Stack stack = stackService.getById(stackId);
            blueprintValidator.validateBlueprintForStack(blueprint, subject.getHostGroups(), stack.getInstanceGroups());
        }
        if (data[DecorationData.SSSDCONFIG_ID.ordinal()] != null) {
            SssdConfig config = sssdConfigService.get((Long) data[DecorationData.SSSDCONFIG_ID.ordinal()]);
            subject.setSssdConfig(config);
        }
        return subject;
    }

    private Set<HostGroup> convertHostGroupsFromJson(Long stackId, CbUser user, Cluster cluster, Set<HostGroupJson> hostGroupsJsons) {
        Set<HostGroup> hostGroups = new HashSet<>();
        for (HostGroupJson json : hostGroupsJsons) {
            HostGroup hostGroup = conversionService.convert(json, HostGroup.class);
            hostGroup.setCluster(cluster);
            hostGroup = hostGroupDecorator.decorate(hostGroup, stackId, user, json.getConstraint(), json.getRecipeIds(), true);
            hostGroups.add(hostGroup);
        }
        return hostGroups;
    }

}

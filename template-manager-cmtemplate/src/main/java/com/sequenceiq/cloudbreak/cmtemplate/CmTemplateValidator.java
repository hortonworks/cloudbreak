package com.sequenceiq.cloudbreak.cmtemplate;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.InstanceCount;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.template.validation.BlueprintValidator;
import com.sequenceiq.cloudbreak.template.validation.BlueprintValidatorUtil;

@Component
public class CmTemplateValidator implements BlueprintValidator {

    @Inject
    private CmTemplateProcessorFactory processorFactory;

    @Override
    public void validate(Blueprint blueprint, Set<HostGroup> hostGroups, Collection<InstanceGroup> instanceGroups,
        boolean validateServiceCardinality) {
        CmTemplateProcessor templateProcessor = processorFactory.get(blueprint.getBlueprintText());
        Map<String, InstanceCount> blueprintHostGroupCardinality = templateProcessor.getCardinalityByHostGroup();

        BlueprintValidatorUtil.validateHostGroupsMatch(hostGroups, blueprintHostGroupCardinality.keySet());
        BlueprintValidatorUtil.validateInstanceGroups(hostGroups, instanceGroups);
        if (validateServiceCardinality) {
            BlueprintValidatorUtil.validateHostGroupCardinality(hostGroups, blueprintHostGroupCardinality);
        }
    }

    @Override
    public void validateHostGroupScalingRequest(Blueprint blueprint, HostGroup hostGroup, Integer adjustment) {
        CmTemplateProcessor templateProcessor = processorFactory.get(blueprint.getBlueprintText());
        Set<String> services = templateProcessor.getComponentsByHostGroup().get(hostGroup.getName());
        for (BlackListedDownScaleRole role : BlackListedDownScaleRole.values()) {
            if (services.contains(role.name()) && adjustment < 0) {
                throw new BadRequestException(String.format("'%s' service is not enabled to scale",
                        role.name()));
            }
        }
        for (BlackListedUpScaleRole role : BlackListedUpScaleRole.values()) {
            if (services.contains(role.name()) && adjustment > 0) {
                throw new BadRequestException(String.format("'%s' service is not enabled to scale",
                        role.name()));
            }
        }
    }
}

package com.sequenceiq.cloudbreak.converter.v4.stacks;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterV4Request;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintTextProcessorFactory;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;

@Component
public class StackScaleV4RequestToUpdateClusterV4RequestConverter extends AbstractConversionServiceAwareConverter<StackScaleV4Request, UpdateClusterV4Request> {

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private BlueprintTextProcessorFactory clusterDefinitionTextProcessorFactory;

    @Override
    public UpdateClusterV4Request convert(StackScaleV4Request source) {
        UpdateClusterV4Request updateStackJson = new UpdateClusterV4Request();
        Cluster oneByStackId = clusterService.findOneByStackId(source.getStackId())
                .orElseThrow(NotFoundException.notFound("cluster", source.getStackId()));
        Optional<HostGroup> hostGroup = hostGroupService.findHostGroupInClusterByName(oneByStackId.getId(), source.getGroup());
        if (hostGroup.isPresent()) {
            String blueprintText = oneByStackId.getBlueprint().getBlueprintText();
            BlueprintTextProcessor blueprintTextProcessor =
                    clusterDefinitionTextProcessorFactory.createBlueprintTextProcessor(blueprintText);
            boolean dataNodeComponentInHostGroup = blueprintTextProcessor.isComponentExistsInHostGroup("DATANODE",
                    hostGroup.get().getName());
            HostGroupAdjustmentV4Request hostGroupAdjustmentJson = new HostGroupAdjustmentV4Request();
            hostGroupAdjustmentJson.setWithStackUpdate(true);
            hostGroupAdjustmentJson.setValidateNodeCount(dataNodeComponentInHostGroup);
            hostGroupAdjustmentJson.setHostGroup(source.getGroup());
            int scaleNumber = source.getDesiredCount() - hostGroup.get().getHostMetadata().size();
            hostGroupAdjustmentJson.setScalingAdjustment(scaleNumber);
            updateStackJson.setHostGroupAdjustment(hostGroupAdjustmentJson);
        } else {
            throw new BadRequestException(String.format("Group '%s' not available on stack", source.getGroup()));
        }
        return updateStackJson;
    }
}

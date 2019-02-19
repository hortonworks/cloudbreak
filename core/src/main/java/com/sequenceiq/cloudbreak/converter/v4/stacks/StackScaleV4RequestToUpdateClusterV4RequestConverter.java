package com.sequenceiq.cloudbreak.converter.v4.stacks;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterV4Request;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.template.processor.AmbariBlueprintTextProcessor;

@Component
public class StackScaleV4RequestToUpdateClusterV4RequestConverter extends AbstractConversionServiceAwareConverter<StackScaleV4Request, UpdateClusterV4Request> {

    @Inject
    private HostGroupRepository hostGroupRepository;

    @Inject
    private ClusterService clusterService;

    @Override
    public UpdateClusterV4Request convert(StackScaleV4Request source) {
        UpdateClusterV4Request updateStackJson = new UpdateClusterV4Request();
        Cluster oneByStackId = clusterService.findOneByStackId(source.getStackId());
        HostGroup hostGroup = hostGroupRepository.findHostGroupInClusterByName(oneByStackId.getId(), source.getGroup());
        if (hostGroup != null) {
            String clusterDefinitionText = oneByStackId.getClusterDefinition().getClusterDefinitionText();
            boolean dataNodeComponentInHostGroup = new AmbariBlueprintTextProcessor(clusterDefinitionText).isComponentExistsInHostGroup("DATANODE",
                    hostGroup.getName());
            HostGroupAdjustmentV4Request hostGroupAdjustmentJson = new HostGroupAdjustmentV4Request();
            hostGroupAdjustmentJson.setWithStackUpdate(true);
            hostGroupAdjustmentJson.setValidateNodeCount(dataNodeComponentInHostGroup);
            hostGroupAdjustmentJson.setHostGroup(source.getGroup());
            int scaleNumber = source.getDesiredCount() - hostGroup.getHostMetadata().size();
            hostGroupAdjustmentJson.setScalingAdjustment(scaleNumber);
            updateStackJson.setHostGroupAdjustment(hostGroupAdjustmentJson);
        } else {
            throw new BadRequestException(String.format("Group '%s' not available on stack", source.getGroup()));
        }
        return updateStackJson;
    }
}

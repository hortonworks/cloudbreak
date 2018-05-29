package com.sequenceiq.cloudbreak.converter.v2;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson;
import com.sequenceiq.cloudbreak.api.model.StackScaleRequestV2;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.BlueprintProcessor;

@Component
public class UpdateStackRequestV2ToUpdateClusterRequestConverter extends AbstractConversionServiceAwareConverter<StackScaleRequestV2, UpdateClusterJson> {

    @Inject
    private HostGroupRepository hostGroupRepository;

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private BlueprintProcessor blueprintProcessor;

    @Override
    public UpdateClusterJson convert(StackScaleRequestV2 source) {
        UpdateClusterJson updateStackJson = new UpdateClusterJson();
        Cluster oneByStackId = clusterRepository.findOneByStackId(source.getStackId());
        HostGroup hostGroup = hostGroupRepository.findHostGroupInClusterByName(oneByStackId.getId(), source.getGroup());
        if (hostGroup != null) {
            String blueprintText = oneByStackId.getBlueprint().getBlueprintText();
            boolean dataNodeComponentInHostGroup = blueprintProcessor.componentExistsInHostGroup("DATANODE", blueprintText, hostGroup.getName());
            HostGroupAdjustmentJson hostGroupAdjustmentJson = new HostGroupAdjustmentJson();
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

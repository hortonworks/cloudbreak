package com.sequenceiq.cloudbreak.converter.v2;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson;
import com.sequenceiq.cloudbreak.api.model.stack.StackScaleRequestV2;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.service.VaultService;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;

@Component
public class StackScaleRequestV2ToUpdateClusterRequestConverter extends AbstractConversionServiceAwareConverter<StackScaleRequestV2, UpdateClusterJson> {

    @Inject
    private HostGroupRepository hostGroupRepository;

    @Inject
    private ClusterService clusterService;

    @Inject
    private VaultService vaultService;

    @Override
    public UpdateClusterJson convert(StackScaleRequestV2 source) {
        UpdateClusterJson updateStackJson = new UpdateClusterJson();
        Cluster oneByStackId = clusterService.findOneByStackId(source.getStackId());
        HostGroup hostGroup = hostGroupRepository.findHostGroupInClusterByName(oneByStackId.getId(), source.getGroup());
        if (hostGroup != null) {
            String blueprintText = vaultService.resolveSingleValue(oneByStackId.getBlueprint().getBlueprintText());
            boolean dataNodeComponentInHostGroup = new BlueprintTextProcessor(blueprintText).componentExistsInHostGroup("DATANODE", hostGroup.getName());
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

package com.sequenceiq.cloudbreak.converter.v2;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson;
import com.sequenceiq.cloudbreak.api.model.stack.StackScaleRequestV2;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;

@Component
public class StackScaleRequestV2ToUpdateClusterRequestConverter extends AbstractConversionServiceAwareConverter<StackScaleRequestV2, UpdateClusterJson> {

    @Inject
    private HostGroupRepository hostGroupRepository;

    @Inject
    private StackService stackService;

    @Inject
    private TransactionService transactionService;

    @Override
    public UpdateClusterJson convert(StackScaleRequestV2 source) {
        try {
            return transactionService.required(() -> {
                UpdateClusterJson updateStackJson = new UpdateClusterJson();
                Stack stack = stackService.getByIdWithLists(source.getStackId());
                stack.getInstanceGroups().stream().filter(instanceGroup -> source.getGroup().equals(instanceGroup.getGroupName())).findFirst().ifPresentOrElse(
                        instanceGroup -> {
                            String blueprintText = stack.getCluster().getBlueprint().getBlueprintText();
                            boolean dataNodeComponentInHostGroup =
                                    new BlueprintTextProcessor(blueprintText).componentExistsInHostGroup("DATANODE", instanceGroup.getGroupName());
                            HostGroupAdjustmentJson hostGroupAdjustmentJson = new HostGroupAdjustmentJson();
                            hostGroupAdjustmentJson.setWithStackUpdate(true);
                            hostGroupAdjustmentJson.setValidateNodeCount(dataNodeComponentInHostGroup);
                            hostGroupAdjustmentJson.setHostGroup(source.getGroup());
                            hostGroupAdjustmentJson.setForced(source.getForced());
                            int scaleNumber = source.getDesiredCount() - instanceGroup.getNotTerminatedInstanceMetaDataSet().size();
                            hostGroupAdjustmentJson.setScalingAdjustment(scaleNumber);
                            updateStackJson.setHostGroupAdjustment(hostGroupAdjustmentJson);
                        }, () -> {
                            throw new BadRequestException(String.format("Group '%s' not available on stack", source.getGroup()));
                        }
                );
                return updateStackJson;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }
}

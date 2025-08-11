package com.sequenceiq.cloudbreak.converter.v4.stacks;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterV4Request;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintTextProcessorFactory;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;

@Component
public class StackScaleV4RequestToUpdateClusterV4RequestConverter {

    @Inject
    private StackService stackService;

    @Inject
    private BlueprintTextProcessorFactory clusterDefinitionTextProcessorFactory;

    @Inject
    private TransactionService transactionService;

    public UpdateClusterV4Request convert(StackScaleV4Request source) {
        try {
            return transactionService.required(() -> {
                UpdateClusterV4Request updateStackJson = new UpdateClusterV4Request();
                Stack stack = stackService.getByIdWithListsInTransaction(source.getStackId());
                stack.getInstanceGroups().stream().filter(instanceGroup -> source.getGroup().equals(instanceGroup.getGroupName())).findFirst().ifPresentOrElse(
                        instanceGroup -> {
                            String blueprintText = stack.getBlueprintJsonText();
                            BlueprintTextProcessor blueprintTextProcessor =
                                    clusterDefinitionTextProcessorFactory.createBlueprintTextProcessor(blueprintText);
                            boolean dataNodeComponentInHostGroup = blueprintTextProcessor.isComponentExistsInHostGroup("DATANODE",
                                    instanceGroup.getGroupName());
                            HostGroupAdjustmentV4Request hostGroupAdjustmentJson = new HostGroupAdjustmentV4Request();
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
            throw e.getCause();
        }
    }

}

package com.sequenceiq.freeipa.service.stack;

import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.cloud.azure.AzureDiskType;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.AwsDiskType;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.DiskUpdateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.UpdateRootVolumeResponse;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.flow.chain.FlowChainTriggers;
import com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.event.RootVolumeUpdateEvent;
import com.sequenceiq.freeipa.service.DefaultRootVolumeSizeProvider;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;
import com.sequenceiq.freeipa.service.stack.instance.TemplateService;

@Service
public class RootVolumeUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RootVolumeUpdateService.class);

    private static final Map<String, ResourceType> PLATFORM_VOLUME_RESOURCE_TYPE_MAP = ImmutableMap.of(
            CloudPlatform.AWS.name(), ResourceType.AWS_ROOT_DISK,
            CloudPlatform.AZURE.name(), ResourceType.AZURE_DISK);

    private static final Map<String, List<String>> PLATFORM_DISK_TYPE_MAP = ImmutableMap.of(CloudPlatform.AWS.name(), List.of(
                    AwsDiskType.Gp2.value(), AwsDiskType.Gp3.value(), AwsDiskType.Standard.value()),
            CloudPlatform.AZURE.name(), List.of(AzureDiskType.STANDARD_SSD_LRS.value(), AzureDiskType.LOCALLY_REDUNDANT.value(),
                    AzureDiskType.PREMIUM_LOCALLY_REDUNDANT.value()));

    @Inject
    private DefaultRootVolumeSizeProvider defaultRootVolumeSizeProvider;

    @Inject
    private StackService stackService;

    @Inject
    private OperationService operationService;

    @Inject
    private FreeIpaFlowManager flowManager;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private TemplateService templateService;

    public UpdateRootVolumeResponse updateRootVolume(String environmentCrn, DiskUpdateRequest request, String accountId) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(environmentCrn, accountId);
        LOGGER.debug("{} request received for vertical scaling stack {}", request, stack.getResourceCrn());
        String platform = stack.getCloudPlatform();
        if (PLATFORM_VOLUME_RESOURCE_TYPE_MAP.containsKey(platform) && checkPlatformVolumeType(request, platform)) {
            checkUpdateRequiredStackTemplate(stack, request);
            updateTemplate(stack, request);
            List<String> instanceIds = stack.getInstanceGroups().stream()
                    .flatMap(ig -> ig.getInstanceMetaDataSet().stream()).map(InstanceMetaData::getInstanceId).toList();
            UpdateRootVolumeResponse updateRootVolumeResponse = new UpdateRootVolumeResponse();
            updateRootVolumeResponse.setFlowIdentifier(triggerRootVolumeUpdate(stack, accountId, instanceIds));
            return updateRootVolumeResponse;
        }
        throw new BadRequestException("Root Volume Update for type '" + request.getVolumeType() + "'is not supported for cloud platform: "
                + stack.getCloudPlatform());
    }

    private FlowIdentifier triggerRootVolumeUpdate(Stack stack, String accountId, List<String> instanceIds) {
        String pgwInstanceId = instanceMetaDataService.getPrimaryGwInstance(stack.getNotDeletedInstanceMetaDataSet()).getInstanceId();
        int nodeCount = stack.getInstanceGroups().stream().findFirst().get().getNodeCount();
        Operation operation = operationService.startOperation(accountId, OperationType.MODIFY_ROOT_VOLUME,
                Set.of(stack.getEnvironmentCrn()), Collections.emptySet());
        try {
            return flowManager.notify(FlowChainTriggers.ROOT_VOLUME_UPDATE_TRIGGER_EVENT,
                    new RootVolumeUpdateEvent(FlowChainTriggers.ROOT_VOLUME_UPDATE_TRIGGER_EVENT, stack.getId(),
                            operation.getOperationId(), nodeCount, instanceIds, pgwInstanceId));
        } catch (Exception e) {
            LOGGER.error("Couldn't start Freeipa repair flow", e);
            operationService.failOperation(accountId, operation.getOperationId(), "Couldn't start Freeipa Root Volume Update flow: " + e.getMessage());
            throw new CloudbreakServiceException("Couldn't start Freeipa Root Volume Update flow: " + e.getMessage());
        }
    }

    private void checkUpdateRequiredStackTemplate(Stack stack, DiskUpdateRequest diskUpdateRequest) throws BadRequestException {
        Template template = getTemplate(stack);
        int updateSize = diskUpdateRequest.getSize();
        String updateVolumeType = diskUpdateRequest.getVolumeType();
        if (!updateSizeRequired(updateSize, stack.getCloudPlatform(), template) && !updateVolumeTypeRequired(updateVolumeType, template)) {
            throw new BadRequestException("No update required.");
        }
    }

    private boolean updateSizeRequired(int updateSize, String cloudPlatform, Template template) {
        int defaultRootVolumeSize = defaultRootVolumeSizeProvider.getForPlatform(cloudPlatform);
        if (updateSize != 0 && updateSize < defaultRootVolumeSize) {
            throw new BadRequestException("Requested size for root volume " + updateSize +
                    " should not be lesser than the default root volume size " + defaultRootVolumeSize + ".");
        }
        return updateSize == 0 || (updateSize > 0 && updateSize > defaultRootVolumeSize && updateSize != template.getRootVolumeSize());
    }

    private boolean updateVolumeTypeRequired(String updateVolumeType, Template template) {
        return isNotEmpty(updateVolumeType) && !updateVolumeType.equals(defaultIfEmpty(template.getRootVolumeType(), ""));
    }

    private Template getTemplate(Stack stack) {
        InstanceGroup instanceGroup = stack.getInstanceGroups().stream().findFirst().orElseThrow();
        return instanceGroup.getTemplate();
    }

    private void updateTemplate(Stack stack, DiskUpdateRequest diskUpdateRequest) {
        Template template = getTemplate(stack);
        int updateSize = diskUpdateRequest.getSize();
        String updateVolumeType = diskUpdateRequest.getVolumeType();
        if (updateSizeRequired(updateSize, stack.getCloudPlatform(), template)) {
            template.setRootVolumeSize(updateSize);
        }
        if (updateVolumeTypeRequired(updateVolumeType, template)) {
            template.setRootVolumeType(updateVolumeType);
        }
        templateService.save(template);
        LOGGER.debug("Updated template after save: {}", template);
    }

    private boolean checkPlatformVolumeType(DiskUpdateRequest updateRequest, String platform) {
        String updateVolumeType = defaultIfEmpty(updateRequest.getVolumeType(), "");
        return isEmpty(updateVolumeType) || PLATFORM_DISK_TYPE_MAP.get(platform).contains(updateVolumeType);
    }
}

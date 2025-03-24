package com.sequenceiq.freeipa.flow.freeipa.verticalscale;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.UpdateType;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceTemplateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.VolumeRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIpaVerticalScaleRequest;
import com.sequenceiq.freeipa.service.stack.instance.InstanceGroupService;
import com.sequenceiq.freeipa.service.stack.instance.TemplateService;

@Service
public class FreeIpaVerticalScaleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaVerticalScaleService.class);

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private TemplateService templateService;

    public void updateTemplateWithVerticalScaleInformation(Long stackId,
            VerticalScaleRequest request) {
        Optional<InstanceGroup> optionalGroup = instanceGroupService
                .getByStackIdAndInstanceGroupNameWithFetchTemplate(stackId, request.getGroup());
        if (optionalGroup.isPresent() && request.getTemplate() != null) {
            InstanceGroup group = optionalGroup.get();
            Template template = templateService.findById(group.getTemplate().getId()).get();
            InstanceTemplateRequest instanceTemplateRequest = request.getTemplate();
            String instanceType = instanceTemplateRequest.getInstanceType();
            if (!Strings.isNullOrEmpty(instanceType)) {
                LOGGER.info("Set instancetype to {} in group {} on stackid {}", instanceType, template.getName(), stackId);
                template.setInstanceType(instanceType);
            }
            Set<VolumeRequest> requestedAttachedVolumes = instanceTemplateRequest.getAttachedVolumes();
            if (requestedAttachedVolumes != null) {
                LOGGER.info("Set volume to {} in group {} on stackid {}", requestedAttachedVolumes, template.getName(), stackId);
                VolumeRequest requestedAttachedVolume = requestedAttachedVolumes.iterator().next();
                template.setVolumeCount(requestedAttachedVolume.getCount());
                template.setVolumeSize(requestedAttachedVolume.getSize());
                template.setVolumeType(requestedAttachedVolume.getType());
            }
            if (instanceTemplateRequest.getRootVolume() != null && instanceTemplateRequest.getRootVolume().getSize() != null) {
                template.setRootVolumeSize(instanceTemplateRequest.getRootVolume().getSize());
            }
            templateService.save(template);
        }
    }

    public List<CloudResourceStatus> verticalScale(AuthenticatedContext ac, FreeIpaVerticalScaleRequest request,
            CloudConnector connector) throws Exception {
        CloudStack cloudStack = request.getCloudStack();
        try {
            return connector.resources().update(ac, cloudStack, request.getResourceList(), UpdateType.VERTICAL_SCALE,
                    Optional.ofNullable(request.getFreeIPAVerticalScaleRequest().getGroup()));
        } catch (Exception e) {
            LOGGER.info("Exception occured on update process retrying the operation. Error was: {}", e.getMessage(), e);
            return handleExceptionAndRetryUpdate(request, connector, ac, cloudStack, UpdateType.VERTICAL_SCALE);
        }
    }

    private List<CloudResourceStatus> handleExceptionAndRetryUpdate(
            FreeIpaVerticalScaleRequest request,
            CloudConnector connector,
            AuthenticatedContext ac,
            CloudStack cloudStack,
            UpdateType type) throws Exception {
        return connector.resources().update(ac, cloudStack, request.getResourceList(), type,
                Optional.ofNullable(request.getFreeIPAVerticalScaleRequest().getGroup()));
    }
}
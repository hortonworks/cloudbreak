package com.sequenceiq.freeipa.flow.freeipa.verticalscale;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceTemplateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.VolumeRequest;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIPAVerticalScaleRequest;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIPAVerticalScaleResult;
import com.sequenceiq.freeipa.service.stack.instance.InstanceGroupService;
import com.sequenceiq.freeipa.service.stack.instance.TemplateService;

@Service
public class FreeIPAVerticalScaleService {

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private TemplateService templateService;

    public void updateTemplateWithVerticalScaleInformation(Long stackId,
            com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.FreeIPAVerticalScaleRequest request) {
        Optional<InstanceGroup> optionalGroup = instanceGroupService
                .getByStackIdAndInstanceGroupNameWithFetchTemplate(stackId, request.getGroup());
        if (optionalGroup.isPresent() && request.getTemplate() != null) {
            InstanceGroup group = optionalGroup.get();
            Template template = templateService.findById(group.getTemplate().getId()).get();
            InstanceTemplateRequest instanceTemplateRequest = request.getTemplate();
            String instanceType = instanceTemplateRequest.getInstanceType();
            if (!Strings.isNullOrEmpty(instanceType)) {
                template.setInstanceType(instanceType);
            }
            Set<VolumeRequest> requestedAttachedVolumes = instanceTemplateRequest.getAttachedVolumes();
            if (requestedAttachedVolumes != null) {
                VolumeRequest requestedAttachedVolume = requestedAttachedVolumes.iterator().next();
                template.setVolumeCount(requestedAttachedVolume.getCount());
                template.setVolumeSize(requestedAttachedVolume.getSize());
                template.setVolumeType(requestedAttachedVolume.getType());
            }
            templateService.save(template);
        }
    }

    public List<CloudResourceStatus> verticalScale(AuthenticatedContext ac, FreeIPAVerticalScaleRequest<FreeIPAVerticalScaleResult> request,
            CloudConnector connector) throws Exception {
        CloudStack cloudStack = request.getCloudStack();
        try {
            return connector.resources().update(ac, cloudStack, request.getResourceList());
        } catch (Exception e) {
            return handleExceptionAndRetryUpdate(request, connector, ac, cloudStack, e);
        }
    }

    private List<CloudResourceStatus> handleExceptionAndRetryUpdate(
            FreeIPAVerticalScaleRequest<FreeIPAVerticalScaleResult> request,
            CloudConnector connector,
            AuthenticatedContext ac,
            CloudStack cloudStack,
            Exception e) throws Exception {
        return connector.resources().update(ac, cloudStack, request.getResourceList());
    }
}

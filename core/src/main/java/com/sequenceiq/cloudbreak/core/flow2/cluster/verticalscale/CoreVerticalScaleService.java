package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_VERTICALSCALED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_VERTICALSCALING;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.VolumeV4Request;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.template.TemplateService;

@Service
public class CoreVerticalScaleService {
    @Inject
    private ClusterService clusterService;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private TemplateService templateService;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    public void verticalScale(Long stackId) {
        flowMessageService.fireEventAndLog(stackId, Status.UPDATE_IN_PROGRESS.name(), STACK_VERTICALSCALING);
    }

    public void finishVerticalScale(Long stackId, Long clusterId) {
        Cluster cluster = clusterService.getCluster(clusterId);
        finishVerticalScale(stackId, cluster);
    }

    public void updateTemplateWithVerticalScaleInformation(Long stackId, StackVerticalScaleV4Request stackVerticalScaleV4Request) {
        Optional<InstanceGroup> optionalGroup = instanceGroupService
                .findOneByStackIdAndGroupNameWithTemplate(stackId, stackVerticalScaleV4Request.getGroup());
        if (optionalGroup.isPresent() && stackVerticalScaleV4Request.getTemplate() != null) {
            InstanceGroup group = optionalGroup.get();
            Template template = templateService.get(group.getTemplate().getId());
            InstanceTemplateV4Request requestedTemplate = stackVerticalScaleV4Request.getTemplate();
            String instanceType = requestedTemplate.getInstanceType();
            if (!Strings.isNullOrEmpty(instanceType)) {
                template.setInstanceType(instanceType);
            }
            if (requestedTemplate.getRootVolume() != null) {
                Integer rootVolumeSize = requestedTemplate.getRootVolume().getSize();
                template.setRootVolumeSize(rootVolumeSize);
            }
            Set<VolumeV4Request> requestedAttachedVolumes = requestedTemplate.getAttachedVolumes();
            if (requestedAttachedVolumes != null) {
                for (VolumeTemplate volumeTemplateInTheDatabase : template.getVolumeTemplates()) {
                    for (VolumeV4Request volumeV4Request : requestedAttachedVolumes) {
                        if (volumeTemplateInTheDatabase.getVolumeType().equals(volumeV4Request.getType())) {
                            volumeTemplateInTheDatabase.setVolumeCount(volumeV4Request.getSize());
                            volumeTemplateInTheDatabase.setVolumeSize(volumeV4Request.getCount());
                        }
                    }
                }
            }
            templateService.savePure(template);
        }
    }

    private void finishVerticalScale(Long stackId, Cluster cluster) {
        clusterService.updateCluster(cluster);
        clusterService.updateClusterStatusByStackId(stackId, DetailedStackStatus.STOPPED);
        flowMessageService.fireEventAndLog(stackId, Status.STOPPED.name(), STACK_VERTICALSCALED);
    }
}

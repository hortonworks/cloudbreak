package com.sequenceiq.cloudbreak.service.stack.repair;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.handler.InstanceStateQuery;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;

@Component
public class UnhealthyInstancesFinalizer {

    @Inject
    private InstanceMetaDataToCloudInstanceConverter cloudInstanceConverter;

    @Inject
    private InstanceStateQuery instanceStateQuery;

    @Inject
    private StackUtil stackUtil;

    public Set<String> finalizeUnhealthyInstances(StackView stack, Collection<InstanceMetadataView> candidateUnhealthyInstances) {
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(stack.getId())
                .withName(stack.getName())
                .withCrn(stack.getResourceCrn())
                .withPlatform(stack.getCloudPlatform())
                .withVariant(stack.getPlatformVariant())
                .withLocation(location)
                .withWorkspaceId(stack.getWorkspaceId())
                .withAccountId(Crn.safeFromString(stack.getResourceCrn()).getAccountId())
                .withTenantId(stack.getTenantId())
                .build();
        CloudCredential cloudCredential = stackUtil.getCloudCredential(stack.getEnvironmentCrn());
        List<CloudInstance> cloudInstances = cloudInstanceConverter.convert(candidateUnhealthyInstances, stack);

        List<CloudVmInstanceStatus> cloudVmInstanceStatuses = instanceStateQuery.getCloudVmInstanceStatuses(
                cloudCredential, cloudContext, cloudInstances);
        Map<String, CloudVmInstanceStatus> cloudVmInstanceStatusesById = new HashMap<>();
        cloudVmInstanceStatuses.forEach(c -> cloudVmInstanceStatusesById.put(c.getCloudInstance().getInstanceId(), c));

        Set<String> unhealthyInstances = new HashSet<>();
        for (InstanceMetadataView i : candidateUnhealthyInstances) {
            CloudVmInstanceStatus instanceStatus = cloudVmInstanceStatusesById.get(i.getInstanceId());
            if (instanceStatus == null || instanceStatus.getStatus().equals(InstanceStatus.TERMINATED)) {
                unhealthyInstances.add(i.getInstanceId());
            }
        }
        return unhealthyInstances;
    }

}

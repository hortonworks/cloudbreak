package com.sequenceiq.cloudbreak.service.stack.repair;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.handler.InstanceStateQuery;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Component
public class UnhealthyInstancesFinalizer {

    @Inject
    private InstanceMetaDataToCloudInstanceConverter cloudInstanceConverter;

    @Inject
    private InstanceStateQuery instanceStateQuery;

    @Inject
    private StackUtil stackUtil;

    public Set<String> finalizeUnhealthyInstances(Stack stack, Iterable<InstanceMetaData> candidateUnhealthyInstances) {
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(stack.getId())
                .withName(stack.getName())
                .withCrn(stack.getResourceCrn())
                .withPlatform(stack.getCloudPlatform())
                .withVariant(stack.getPlatformVariant())
                .withLocation(location)
                .withWorkspaceId(stack.getWorkspace().getId())
                .withAccountId(stack.getTenant().getId())
                .build();
        CloudCredential cloudCredential = stackUtil.getCloudCredential(stack);
        List<CloudInstance> cloudInstances = cloudInstanceConverter.convert(candidateUnhealthyInstances, stack.getEnvironmentCrn(),
                stack.getStackAuthentication());

        List<CloudVmInstanceStatus> cloudVmInstanceStatuses = instanceStateQuery.getCloudVmInstanceStatuses(
                cloudCredential, cloudContext, cloudInstances);
        Map<String, CloudVmInstanceStatus> cloudVmInstanceStatusesById = new HashMap<>();
        cloudVmInstanceStatuses.forEach(c -> cloudVmInstanceStatusesById.put(c.getCloudInstance().getInstanceId(), c));

        Set<String> unhealthyInstances = new HashSet<>();
        for (InstanceMetaData i : candidateUnhealthyInstances) {
            CloudVmInstanceStatus instanceStatus = cloudVmInstanceStatusesById.get(i.getInstanceId());
            if ((instanceStatus == null) || (instanceStatus.getStatus().equals(InstanceStatus.TERMINATED))) {
                unhealthyInstances.add(i.getInstanceId());
            }
        }
        return unhealthyInstances;
    }

}

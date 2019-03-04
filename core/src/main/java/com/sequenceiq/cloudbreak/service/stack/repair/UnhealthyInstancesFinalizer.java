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
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

@Component
public class UnhealthyInstancesFinalizer {

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private InstanceMetaDataToCloudInstanceConverter cloudInstanceConverter;

    @Inject
    private InstanceStateQuery instanceStateQuery;

    public Set<String> finalizeUnhealthyInstances(Stack stack, Iterable<InstanceMetaData> candidateUnhealthyInstances) {
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform(),
                stack.getPlatformVariant(), location, stack.getCreator().getUserId(), stack.getWorkspace().getId());
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        List<CloudInstance> cloudInstances = cloudInstanceConverter.convert(candidateUnhealthyInstances);

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

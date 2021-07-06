package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static java.util.stream.Collectors.toList;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.handler.InstanceStateQuery;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialConverter;
import com.sequenceiq.environment.client.EnvironmentInternalCrnClient;

@Service
public class StackInstanceStatusChecker {

    @Inject
    private InstanceStateQuery instanceStateQuery;

    @Inject
    private EnvironmentInternalCrnClient environmentInternalCrnClient;

    @Inject
    private CredentialConverter credentialConverter;

    @Inject
    private CredentialToCloudCredentialConverter cloudCredentialConverter;

    public List<CloudVmInstanceStatus> queryInstanceStatuses(Stack stack, List<CloudInstance> cloudInstances) {
        List<CloudVmInstanceStatus> result = Collections.emptyList();
        if (!cloudInstances.isEmpty()) {
            cloudInstances.forEach(instance -> stack.getParameters().forEach(instance::putParameter));
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
            CloudCredential cloudCredential = getCloudCredential(stack.getEnvironmentCrn());
            result = getCloudVmInstanceStatuses(cloudInstances, cloudContext, cloudCredential);
        }
        return result;
    }

    private List<CloudVmInstanceStatus> getCloudVmInstanceStatuses(List<CloudInstance> cloudInstances,
            CloudContext cloudContext, CloudCredential cloudCredential) {
        List<CloudVmInstanceStatus> instanceStatuses;
        try {
            instanceStatuses = instanceStateQuery.getCloudVmInstanceStatusesWithoutRetry(cloudCredential, cloudContext, cloudInstances);
        } catch (RuntimeException e) {
            instanceStatuses = cloudInstances.stream()
                    .map(instance -> new CloudVmInstanceStatus(instance, InstanceStatus.UNKNOWN))
                    .collect(toList());
        }
        return instanceStatuses;
    }

    private CloudCredential getCloudCredential(String environmentCrn) {
        return cloudCredentialConverter.convert(
                credentialConverter.convert(
                        environmentInternalCrnClient.withInternalCrn().credentialV1Endpoint().getByEnvironmentCrn(environmentCrn)
                )
        );
    }
}

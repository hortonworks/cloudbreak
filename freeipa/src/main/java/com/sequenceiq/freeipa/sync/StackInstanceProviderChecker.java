package com.sequenceiq.freeipa.sync;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.util.Benchmark.checkedMeasure;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.handler.InstanceStateQuery;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.converter.cloud.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.CredentialService;

@Service
public class StackInstanceProviderChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackInstanceProviderChecker.class);

    @Inject
    private InstanceStateQuery instanceStateQuery;

    @Inject
    private CredentialService credentialService;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private InstanceMetaDataToCloudInstanceConverter metadataConverter;

    public List<CloudVmInstanceStatus> checkStatus(Stack stack, Set<InstanceMetaData> notTerminatedForStack) {
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(stack.getId())
                .withName(stack.getName())
                .withCrn(stack.getResourceCrn())
                .withPlatform(stack.getCloudPlatform())
                .withVariant(stack.getCloudPlatform())
                .withLocation(location)
                .withUserName(stack.getOwner())
                .withAccountId(stack.getAccountId())
                .build();
        CloudCredential cloudCredential = credentialConverter.convert(credentialService.getCredentialByEnvCrn(stack.getEnvironmentCrn()));
        List<CloudInstance> instances = metadataConverter.convert(notTerminatedForStack);
        try {
            return checkedMeasure(() -> instanceStateQuery.getCloudVmInstanceStatusesWithoutRetry(cloudCredential, cloudContext, instances), LOGGER,
                    ":::Auto sync::: get instance statuses in {}ms");
        } catch (Exception e) {
            LOGGER.info(":::Auto sync::: Could not fetch vm statuses: " + e.getMessage(), e);
            throw e;
        }
    }

}

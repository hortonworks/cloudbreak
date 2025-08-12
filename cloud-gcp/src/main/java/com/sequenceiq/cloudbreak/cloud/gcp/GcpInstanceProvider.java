package com.sequenceiq.cloudbreak.cloud.gcp;

import static com.sequenceiq.common.model.DefaultApplicationTag.RESOURCE_CRN;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Instance;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpComputeFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpInstanceStatusMapper;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpLabelUtil;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceCheckMetadata;

@Component
public class GcpInstanceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpInstanceProvider.class);

    @Inject
    private GcpComputeFactory gcpComputeFactory;

    @Inject
    private GcpStackUtil gcpStackUtil;

    @Inject
    private GcpLabelUtil gcpLabelUtil;

    public Optional<Instance> getInstance(AuthenticatedContext authenticatedContext, String instanceName, String zone) {
        CloudContext cloudContext = authenticatedContext.getCloudContext();
        String stackName = cloudContext.getName();
        LOGGER.debug("Collecting instances for stack: {}", stackName);
        long startTime = new Date().getTime();
        Instance instance = null;
        try {
            Compute.Instances.Get request = getRequest(authenticatedContext, zone, instanceName);
            instance = request.execute();
        } catch (IOException e) {
            LOGGER.debug("Error during instance collection", e);
        }
        logResponse(instance, startTime, stackName);
        return Optional.ofNullable(instance);
    }

    private Compute.Instances.Get getRequest(AuthenticatedContext authenticatedContext, String zone, String instanceName) throws IOException {
        CloudCredential credential = authenticatedContext.getCloudCredential();
        Compute compute = gcpComputeFactory.buildCompute(credential);
        return compute.instances()
                .get(gcpStackUtil.getProjectId(credential), zone, instanceName);
    }

    private void logResponse(Instance instance, long startTime, String stackName) {
        long endTime = new Date().getTime();
        if (instance != null) {
            LOGGER.debug("{} instance retrieved for stack {} during {}ms", instance, stackName, endTime - startTime);
        } else {
            LOGGER.debug("There are no instances found for stack {}", stackName);
        }
    }

    public List<InstanceCheckMetadata> collectCdpInstances(AuthenticatedContext ac, String resourceCrn, CloudStack cloudStack, List<String> knownInstanceIds) {
        LOGGER.info("Collecting CDP instances for stack with resource crn: '{}'", resourceCrn);
        Compute compute = gcpComputeFactory.buildCompute(ac.getCloudCredential());
        String projectId = gcpStackUtil.getProjectId(ac.getCloudCredential());
        try {
            String zone = cloudStack.getGroups().stream()
                    .filter(group -> group.getInstances().size() > 0)
                    .findFirst()
                    .map(Group::getReferenceInstanceConfiguration)
                    .map(CloudInstance::getAvailabilityZone)
                    .orElse(null);
            if (zone == null) {
                LOGGER.debug("Cannot extract availabilityZone information from groups for resource: '{}'", resourceCrn);
                return List.of();
            }
            Compute.Instances gcpComputeInstances = compute.instances();
            Map<String, Instance> instances = Optional.ofNullable(gcpComputeInstances.list(projectId, zone)
                            .setFilter("labels." + gcpLabelUtil.transformLabelKeyOrValue(RESOURCE_CRN.key()) + " eq " +
                                    gcpLabelUtil.transformLabelKeyOrValue(resourceCrn))
                            .execute()
                            .getItems()).orElseGet(List::of).stream()
                    .collect(Collectors.toMap(Instance::getName, Function.identity()));
            instances.putAll(retrieveKnownInstancesFromProviderIfAnyMissing(knownInstanceIds, instances, gcpComputeInstances, projectId, zone));

            LOGGER.info("Collected the following instances for stack with resource crn: '{}': {}", resourceCrn, instances.keySet());
            return instances.values().stream()
                    .map(i -> InstanceCheckMetadata.builder()
                            .withInstanceId(i.getName())
                            .withInstanceType(StringUtils.substringAfterLast(i.getMachineType(), "/"))
                            .withStatus(GcpInstanceStatusMapper.getInstanceStatusFromGcpStatus(i.getStatus()))
                            .build())
                    .toList();
        } catch (Exception e) {
            LOGGER.error("Error during collecting CDP instances for stack with resource crn: '{}'", resourceCrn, e);
            return List.of();
        }
    }

    private Map<String, Instance> retrieveKnownInstancesFromProviderIfAnyMissing(List<String> knownInstanceIds, Map<String, Instance> instancesRetrievedByTag,
            Compute.Instances gcpComputeInstances, String projectId, String zone) throws IOException {
        Set<String> instanceIdsRetrievedByTag = instancesRetrievedByTag.keySet();
        List<String> knownInstanceIdsNotRetrievedByTag = knownInstanceIds.stream().filter(Predicate.not(instanceIdsRetrievedByTag::contains)).toList();
        if (!knownInstanceIdsNotRetrievedByTag.isEmpty()) {
            return gcpComputeInstances.list(projectId, zone)
                    .setFilter(knownInstanceIdsNotRetrievedByTag.stream().collect(Collectors.joining("|", "name eq \"(", ")\"")))
                    .execute()
                    .getItems().stream()
                    .collect(Collectors.toMap(Instance::getName, Function.identity()));
        }
        return Map.of();
    }
}

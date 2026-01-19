package com.sequenceiq.it.cloudbreak.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupResponse;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public interface CloudFunctionality {

    int MAX_DELAY = 5000;

    int DELAY = 2000;

    int MULTIPLIER = 2;

    int ATTEMPTS = 5;

    @Retryable(
            maxAttempts = ATTEMPTS,
            backoff = @Backoff(delay = DELAY, multiplier = MULTIPLIER, maxDelay = MAX_DELAY)
    )
    List<String> listInstancesVolumeIds(String clusterName, List<String> instanceIds);

    @Retryable(
            maxAttempts = ATTEMPTS,
            backoff = @Backoff(delay = DELAY, multiplier = MULTIPLIER, maxDelay = MAX_DELAY)
    )
    default List<String> listInstancesRootVolumeIds(String clusterName, List<String> instanceIds) {
        throw new TestFailException("Not implemented for Cloud Provider!");
    }

    @Retryable(
            maxAttempts = ATTEMPTS,
            backoff = @Backoff(delay = DELAY, multiplier = MULTIPLIER, maxDelay = MAX_DELAY)
    )
    Map<String, Set<String>> listInstanceVolumeIds(String clusterName, String instanceId);

    @Retryable(
            maxAttempts = ATTEMPTS,
            backoff = @Backoff(delay = DELAY, multiplier = MULTIPLIER, maxDelay = MAX_DELAY)
    )
    List<String> listInstanceTypes(String clusterName, List<String> instanceIds);

    @Retryable(
            maxAttempts = ATTEMPTS,
            backoff = @Backoff(delay = DELAY, multiplier = MULTIPLIER, maxDelay = MAX_DELAY)
    )
    List<String> listVolumeEncryptionKeyIds(String clusterName, String resourceGroupName, List<String> instanceIds);

    @Retryable(
            maxAttempts = ATTEMPTS,
            backoff = @Backoff(delay = DELAY, multiplier = MULTIPLIER, maxDelay = MAX_DELAY)
    )
    Map<String, Map<String, String>> listTagsByInstanceId(String clusterName, List<String> instanceIds);

    @Retryable(
            maxAttempts = ATTEMPTS,
            backoff = @Backoff(delay = DELAY, multiplier = MULTIPLIER, maxDelay = MAX_DELAY)
    )
    void deleteInstances(String clusterName, List<String> instanceIds);

    @Retryable(
            maxAttempts = ATTEMPTS,
            backoff = @Backoff(delay = DELAY, multiplier = MULTIPLIER, maxDelay = MAX_DELAY)
    )
    default void deleteInstances(String clusterName, Map<String, String> instances) {

    }

    @Retryable(
            maxAttempts = ATTEMPTS,
            backoff = @Backoff(delay = DELAY, multiplier = MULTIPLIER, maxDelay = MAX_DELAY)
    )
    void stopInstances(String clusterName, List<String> instanceIds);

    @Retryable(
            maxAttempts = ATTEMPTS,
            backoff = @Backoff(delay = DELAY, multiplier = MULTIPLIER, maxDelay = MAX_DELAY)
    )
    void cloudStorageInitialize();

    @Retryable(
            maxAttempts = ATTEMPTS,
            backoff = @Backoff(delay = DELAY, multiplier = MULTIPLIER, maxDelay = MAX_DELAY)
    )
    void cloudStorageListContainer(String baseLocation, String selectedObject, boolean zeroContent);

    @Retryable(
            maxAttempts = ATTEMPTS,
            backoff = @Backoff(delay = DELAY, multiplier = MULTIPLIER, maxDelay = MAX_DELAY)
    )
    void cloudStorageListContainerFreeIpa(String baseLocation, String clusterName, String crn);

    @Retryable(
            maxAttempts = ATTEMPTS,
            backoff = @Backoff(delay = DELAY, multiplier = MULTIPLIER, maxDelay = MAX_DELAY)
    )
    void cloudStorageListContainerDataLake(String baseLocation, String clusterName, String crn);

    @Retryable(
            maxAttempts = ATTEMPTS,
            backoff = @Backoff(delay = DELAY, multiplier = MULTIPLIER, maxDelay = MAX_DELAY)
    )
    void cloudStorageDeleteContainer(String baseLocation);

    @Retryable(
            maxAttempts = ATTEMPTS,
            backoff = @Backoff(delay = DELAY, multiplier = MULTIPLIER, maxDelay = MAX_DELAY)
    )
    Map<String, Boolean> enaSupport(List<String> instanceIds);

    @Retryable(
            maxAttempts = ATTEMPTS,
            backoff = @Backoff(delay = DELAY, multiplier = MULTIPLIER, maxDelay = MAX_DELAY)
    )
    Map<String, String> getInstanceSubnetMap(List<String> instanceIds);

    default String transformTagKeyOrValue(String originalValue) {
        return originalValue;
    }

    @Retryable(
            maxAttempts = ATTEMPTS,
            backoff = @Backoff(delay = DELAY, multiplier = MULTIPLIER, maxDelay = MAX_DELAY)
    )
    default String getFreeIpaLogsUrl(String clusterName, String crn, String baseLocation) {
        return "/cluster-logs/freeipa/" + clusterName + "_" + Crn.fromString(crn).getResource();
    }

    @Retryable(
            maxAttempts = ATTEMPTS,
            backoff = @Backoff(delay = DELAY, multiplier = MULTIPLIER, maxDelay = MAX_DELAY)
    )
    default String getDataLakeLogsUrl(String clusterName, String crn, String baseLocation) {
        return "/cluster-logs/datalake/" + clusterName + "_" + Crn.fromString(crn).getResource();
    }

    @Retryable(
            maxAttempts = ATTEMPTS,
            backoff = @Backoff(delay = DELAY, multiplier = MULTIPLIER, maxDelay = MAX_DELAY)
    )
    default String getDataHubLogsUrl(String clusterName, String crn, String baseLocation) {
        return "/cluster-logs/datahub/" + clusterName + "_" + Crn.fromString(crn).getResource();
    }

    @Retryable(
            maxAttempts = ATTEMPTS,
            backoff = @Backoff(delay = DELAY, multiplier = MULTIPLIER, maxDelay = MAX_DELAY)
    )
    void checkMountedDisks(List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames);

    @Retryable(
            maxAttempts = ATTEMPTS,
            backoff = @Backoff(delay = DELAY, multiplier = MULTIPLIER, maxDelay = MAX_DELAY)
    )
    Set<String> getVolumeMountPoints(List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames);

    @Retryable(
            maxAttempts = ATTEMPTS,
            backoff = @Backoff(delay = DELAY, multiplier = MULTIPLIER, maxDelay = MAX_DELAY)
    )
    void verifyEnaDriver(StackV4Response stackV4Response, CloudbreakClient cloudbreakClient, TestContext testContext);

    @Retryable(
            maxAttempts = ATTEMPTS,
            backoff = @Backoff(delay = DELAY, multiplier = MULTIPLIER, maxDelay = MAX_DELAY)
    )
    Map<String, String> getLaunchTemplateUserData(String name);

    @Retryable(
            maxAttempts = ATTEMPTS,
            backoff = @Backoff(delay = DELAY, multiplier = MULTIPLIER, maxDelay = MAX_DELAY)
    )
    Boolean isCloudFormationExistForStack(String name);

    @Retryable(
            maxAttempts = ATTEMPTS,
            backoff = @Backoff(delay = DELAY, multiplier = MULTIPLIER, maxDelay = MAX_DELAY)
    )
    Boolean isFreeipaCfStackExistForEnvironment(String environmentCrn);

    @Retryable(
            maxAttempts = ATTEMPTS,
            backoff = @Backoff(delay = DELAY, multiplier = MULTIPLIER, maxDelay = MAX_DELAY)
    )
    default Map<String, Set<String>> listAvailabilityZonesForVms(String clusterName, List<String> instanceIds) {
        return Map.of();
    }

    default Map<String, String> listAvailabilityZonesForVms(String clusterName, Map<String, String> instanceZoneMap) {
        return listAvailabilityZonesForVms(clusterName, new ArrayList<>(instanceZoneMap.keySet())).entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey(),
                        entry -> CollectionUtils.isNotEmpty(entry.getValue()) ?  entry.getValue().iterator().next() : null));
    }

    @Retryable(
            maxAttempts = ATTEMPTS,
            backoff = @Backoff(delay = DELAY, multiplier = MULTIPLIER, maxDelay = MAX_DELAY)
    )
    default List<com.sequenceiq.cloudbreak.cloud.model.Volume> describeVolumes(List<String> volumeIds) {
        return List.of();
    }

    @Retryable(
            maxAttempts = ATTEMPTS,
            backoff = @Backoff(delay = DELAY, multiplier = MULTIPLIER, maxDelay = MAX_DELAY)
    )
    default List<String> executeSshCommandsOnInstances(List<InstanceGroupResponse> instanceGroups, List<String> hostGroupNames, String privateKeyFilePath,
            String command) {
        throw new TestFailException("Not implemented for Cloud Provider!");
    }
}

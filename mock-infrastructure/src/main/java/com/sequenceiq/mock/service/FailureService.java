package com.sequenceiq.mock.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimaps;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.mock.salt.FailureConfig;
import com.sequenceiq.mock.spi.SpiStoreService;

@Service
public class FailureService {

    public static final String CM_READ_HOSTS = "CM_READ_HOSTS";

    private static final Logger LOGGER = LoggerFactory.getLogger(FailureService.class);

    @Inject
    private SpiStoreService spiStoreService;

    private final Map<String, Set<String>> runFailures = new ConcurrentHashMap<>();

    private final Map<String, FailureConfig> scheduledFailures = new ConcurrentHashMap<>();

    public Set<String> getCommandFailures(String mockUuid) {
        return runFailures.get(mockUuid);
    }

    public void setFailure(String mockUuid, String runArg) {
        runFailures.computeIfAbsent(mockUuid, k -> new ConcurrentSkipListSet<>());
        runFailures.get(mockUuid).add(runArg);
    }

    public void deleteFailure(String mockUuid, String runArg) {
        Set<String> runArgs = runFailures.get(mockUuid);
        if (runArgs != null) {
            runArgs.remove(runArg);
        }
    }

    public void scheduleNodeFailureOnCommand(String mockUuid, String cmd, String group, int allNodeCount, int failedNodeCount) {
        scheduledFailures.computeIfAbsent(mockUuid, (i) -> new FailureConfig()).setFailedNodeConfig(cmd, group, allNodeCount, failedNodeCount);
    }

    public void applyScheduledFailure(String mockUuid, String cmd) {
        spiStoreService.getMetadata(mockUuid)
                .stream()
                .collect(Multimaps.toMultimap(this::getGroupName, i -> i, LinkedListMultimap::create))
                .asMap()
                .forEach((group, instances) -> {
                    FailureConfig.FailedNodeConfig failedNodeConfig = getFailedNodeConfig(mockUuid, cmd, group);
                    if (failedNodeConfig != null) {
                        List<CloudVmMetaDataStatus> availableInstances = instances.stream()
                                .filter(c -> Set.of(InstanceStatus.STARTED, InstanceStatus.CREATED).contains(c.getCloudVmInstanceStatus().getStatus()))
                                .toList();
                        if (availableInstances.size() == failedNodeConfig.allNodeCount()) {
                            LOGGER.info("Modifying {}/{} instances to failed state on {} command {} group.", failedNodeConfig.failedNodeCount(),
                                    failedNodeConfig.allNodeCount(), cmd, group);
                            availableInstances.stream()
                                    .sorted(Comparator.comparingLong(this::getPrivateId).reversed())
                                    .limit(failedNodeConfig.failedNodeCount())
                                    .forEach(c -> spiStoreService.modifyInstanceStatus(mockUuid, c, InstanceStatus.FAILED));
                        }
                    }
                });
    }

    private String getGroupName(CloudVmMetaDataStatus instance) {
        return instance.getCloudVmInstanceStatus().getCloudInstance().getTemplate().getGroupName();
    }

    private Long getPrivateId(CloudVmMetaDataStatus instance) {
        return instance.getCloudVmInstanceStatus().getCloudInstance().getTemplate().getPrivateId();
    }

    private FailureConfig.FailedNodeConfig getFailedNodeConfig(String mockUuid, String cmd, String group) {
        FailureConfig failureConfig = scheduledFailures.get(mockUuid);
        if (failureConfig == null) {
            return null;
        }
        return failureConfig.getFailedNodeConfig(cmd, group);
    }
}

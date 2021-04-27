package com.sequenceiq.freeipa.sync;

import static com.sequenceiq.cloudbreak.util.Benchmark.checkedMeasure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@Component
public class ProviderChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProviderChecker.class);

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private StackInstanceProviderChecker stackInstanceProviderChecker;

    @Inject
    private FlowLogService flowLogService;

    @Value("${freeipa.autosync.update.status:true}")
    private boolean updateStatus;

    public List<ProviderSyncResult> updateAndGetStatuses(Stack stack, Set<InstanceMetaData> checkableInstances,
        Map<InstanceMetaData, DetailedStackStatus> instanceHealthStatusMap, boolean updateStatusFromFlow) {
        return checkedMeasure(() -> {
            List<ProviderSyncResult> results = new ArrayList<>();
            List<CloudVmInstanceStatus> statuses = stackInstanceProviderChecker.checkStatus(stack, checkableInstances);
            if (!updateStatusFromFlow && flowLogService.isOtherFlowRunning(stack.getId())) {
                throw new InterruptSyncingException(":::Auto sync::: interrupt syncing in updateAndGetStatuses, flow is running on freeipa stack " +
                        stack.getName());
            } else {
                statuses.forEach(s -> {
                    Optional<InstanceMetaData> instanceMetaData = checkableInstances.stream()
                            .filter(i -> s.getCloudInstance().getInstanceId().equals(i.getInstanceId()))
                            .findFirst();
                    if (instanceMetaData.isPresent()) {
                        InstanceStatus instanceStatus = updateStatuses(s, instanceMetaData.get(), instanceHealthStatusMap);
                        if (instanceStatus != null) {
                            results.add(new ProviderSyncResult("", instanceStatus, false, s.getCloudInstance().getInstanceId()));
                        }
                    } else {
                        LOGGER.info(":::Auto sync::: Cannot find instanceMetaData");
                    }
                });
                checkableInstances.forEach(instanceMetaData -> {
                    if (statuses.stream().noneMatch(s -> s.getCloudInstance().getInstanceId().equals(instanceMetaData.getInstanceId()))) {
                        if (updateStatus) {
                            setStatusIfNotTheSame(instanceMetaData, InstanceStatus.DELETED_ON_PROVIDER_SIDE);
                            instanceMetaDataService.save(instanceMetaData);
                        } else {
                            LOGGER.debug("updateStatus flag is false, don't update status");
                        }
                    }
                });
                return results;
            }
        }, LOGGER, ":::Auto sync::: provider is checked in {}ms");
    }

    private InstanceStatus updateStatuses(CloudVmInstanceStatus vmInstanceStatus, InstanceMetaData instanceMetaData,
        Map<InstanceMetaData, DetailedStackStatus> instanceHealthStatusMap) {
        LOGGER.info(":::Auto sync::: {} instance metadata status update in progress, new status: {}",
                instanceMetaData.getShortHostname(), vmInstanceStatus);
        InstanceStatus status = null;
        switch (vmInstanceStatus.getStatus()) {
            case STARTED:
                if (DetailedStackStatus.UNREACHABLE == instanceHealthStatusMap.get(instanceMetaData)) {
                    setStatusIfNotTheSame(instanceMetaData, InstanceStatus.UNREACHABLE);
                    status = InstanceStatus.UNREACHABLE;
                } else if (DetailedStackStatus.UNHEALTHY == instanceHealthStatusMap.get(instanceMetaData)) {
                    setStatusIfNotTheSame(instanceMetaData, InstanceStatus.UNHEALTHY);
                    status = InstanceStatus.UNHEALTHY;
                } else {
                setStatusIfNotTheSame(instanceMetaData, InstanceStatus.CREATED);
                status = InstanceStatus.CREATED;
                }
                break;
            case STOPPED:
                setStatusIfNotTheSame(instanceMetaData, InstanceStatus.STOPPED);
                status = InstanceStatus.STOPPED;
                break;
            case FAILED:
                setStatusIfNotTheSame(instanceMetaData, InstanceStatus.FAILED);
                status = InstanceStatus.FAILED;
                break;
            case TERMINATED:
                setStatusIfNotTheSame(instanceMetaData, InstanceStatus.DELETED_ON_PROVIDER_SIDE);
                status = InstanceStatus.DELETED_ON_PROVIDER_SIDE;
                break;
            case TERMINATED_BY_PROVIDER:
                setStatusIfNotTheSame(instanceMetaData, InstanceStatus.DELETED_BY_PROVIDER);
                status = InstanceStatus.DELETED_BY_PROVIDER;
                break;
            default:
                LOGGER.info(":::Auto sync::: the '{}' status is not converted", vmInstanceStatus.getStatus());
        }
        if (updateStatus) {
            instanceMetaDataService.save(instanceMetaData);
        } else {
            LOGGER.debug("updateStatus flag is false, don't update status");
        }

        return status;
    }

    private void setStatusIfNotTheSame(InstanceMetaData instanceMetaData, InstanceStatus newStatus) {
        InstanceStatus oldStatus = instanceMetaData.getInstanceStatus();
        if (oldStatus != newStatus) {
            if (updateStatus) {
                instanceMetaData.setInstanceStatus(newStatus);
                LOGGER.info(":::Auto sync::: The instance status updated from {} to {}", oldStatus, newStatus);
            } else {
                LOGGER.info(":::Auto sync::: The instance status would be had to update from {} to {}",
                        oldStatus, newStatus);
            }
        }
    }
}

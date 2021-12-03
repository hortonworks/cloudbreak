package com.sequenceiq.freeipa.sync;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.service.stack.StackStatusService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@Component
public class FreeipaStatusInfoLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeipaStatusInfoLogger.class);

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private StackStatusService stackStatusService;

    public void logFreeipaStatus(Long stackId, Set<InstanceMetaData> checkableInstances) {
        StackStatus freeipaStatus = stackStatusService.findFirstByStackIdOrderByCreatedDesc(stackId);
        String freeipaStatusInfo = String.format("freeipa stack is %s.", freeipaStatus.getStatus());
        String allInstanceStatusInfo = createInstancesStatusInfo(stackId, checkableInstances);
        LOGGER.debug(":::Auto sync::: freeipa status from healtch check: {} {}", freeipaStatusInfo, allInstanceStatusInfo);
    }

    private String createInstancesStatusInfo(Long stackId, Set<InstanceMetaData> checkableInstances) {
        Set<InstanceMetaData> allInstanceMetaData = fetchCheckableInstancesFromDb(stackId, checkableInstances);
        String allInstanceStatusInfo = allInstanceMetaData.stream()
                .map(instance -> String.format("instance %s is %s", instance.getInstanceId(), instance.getInstanceStatus()))
                .collect(Collectors.joining("; "));
        return allInstanceStatusInfo;
    }

    private Set<InstanceMetaData> fetchCheckableInstancesFromDb(Long stackId, Set<InstanceMetaData> checkableInstances) {
        Set<String> checkableInstanceIds = collectCheckableInstanceIds(checkableInstances);
        Set<InstanceMetaData> allInstanceMetaData = instanceMetaDataService
                .getByInstanceIds(stackId, checkableInstanceIds);
        return allInstanceMetaData;
    }

    private Set<String> collectCheckableInstanceIds(Set<InstanceMetaData> checkableInstances) {
        return checkableInstances.stream()
                .map(InstanceMetaData::getInstanceId)
                .collect(Collectors.toSet());
    }
}

package com.sequenceiq.freeipa.service.stack.instance;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;

import javax.inject.Inject;

@Component
public class InstanceUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceUpdater.class);

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    public Set<InstanceMetaData> updateStatuses(Stack stack, InstanceStatus instanceStatus) {
        Set<InstanceMetaData> checkableInstances = stack.getNotDeletedInstanceMetaDataSet();
        checkableInstances.forEach(instanceMetaData -> {
            setStatusIfNotTheSame(instanceMetaData, instanceStatus);
            instanceMetaDataService.save(instanceMetaData);
        });
        return checkableInstances;
    }

    private void setStatusIfNotTheSame(InstanceMetaData instanceMetaData, InstanceStatus newStatus) {
        InstanceStatus oldStatus = instanceMetaData.getInstanceStatus();
        if (oldStatus != newStatus) {
            instanceMetaData.setInstanceStatus(newStatus);
            LOGGER.info("The instance status updated from {} to {}", oldStatus, newStatus);
        }
    }
}
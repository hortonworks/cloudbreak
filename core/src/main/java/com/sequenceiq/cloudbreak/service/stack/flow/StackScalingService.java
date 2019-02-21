package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.common.type.BillingStatus;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.event.CloudbreakEventService;

@Service
public class StackScalingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackScalingService.class);

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Inject
    private HostMetadataRepository hostMetadataRepository;

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    private enum Msg {
        STACK_SCALING_HOST_DELETED("stack.scaling.host.deleted"),
        STACK_SCALING_HOST_DELETE_FAILED("stack.scaling.host.delete.failed"),
        STACK_SCALING_HOST_NOT_FOUND("stack.scaling.host.not.found"),
        STACK_SCALING_BILLING_CHANGED("stack.scaling.billing.changed");

        private final String code;

        Msg(String msgCode) {
            code = msgCode;
        }

        public String code() {
            return code;
        }
    }

    public void removeHostmetadataIfExists(Stack stack, InstanceMetaData instanceMetaData, HostMetadata hostMetadata) {
        if (hostMetadata != null) {
            try {
                clusterApiConnectors.getConnector(stack).clusterDecomissionService().deleteHostFromCluster(hostMetadata);
                // Deleting by entity will not work because HostMetadata has a reference pointed
                // from HostGroup and per JPA, we would need to clear that up.
                // Reference: http://stackoverflow.com/a/22315188
                hostMetadataRepository.delete(hostMetadata);
                eventService.fireCloudbreakEvent(stack.getId(), Status.AVAILABLE.name(),
                        cloudbreakMessagesService.getMessage(Msg.STACK_SCALING_HOST_DELETED.code(),
                                Collections.singletonList(instanceMetaData.getInstanceId())));
            } catch (Exception e) {
                LOGGER.info("Host cannot be deleted from cluster: ", e);
                eventService.fireCloudbreakEvent(stack.getId(), Status.DELETE_FAILED.name(),
                        cloudbreakMessagesService.getMessage(Msg.STACK_SCALING_HOST_DELETE_FAILED.code(),
                                Collections.singletonList(instanceMetaData.getInstanceId())));

            }
        } else {
            LOGGER.debug("Host cannot be deleted because it does not exist: {}", instanceMetaData.getInstanceId());
            eventService.fireCloudbreakEvent(stack.getId(), Status.AVAILABLE.name(),
                    cloudbreakMessagesService.getMessage(Msg.STACK_SCALING_HOST_NOT_FOUND.code(),
                            Collections.singletonList(instanceMetaData.getInstanceId())));

        }
    }

    public int updateRemovedResourcesState(Stack stack, Collection<String> instanceIds, InstanceGroup instanceGroup) throws TransactionExecutionException {
        return transactionService.required(() -> {
            int nodesRemoved = 0;
            for (InstanceMetaData instanceMetaData : instanceGroup.getNotTerminatedInstanceMetaDataSet()) {
                if (instanceIds.contains(instanceMetaData.getInstanceId())) {
                    long timeInMillis = Calendar.getInstance().getTimeInMillis();
                    instanceMetaData.setTerminationDate(timeInMillis);
                    instanceMetaData.setInstanceStatus(InstanceStatus.TERMINATED);
                    instanceMetaDataRepository.save(instanceMetaData);
                    nodesRemoved++;
                }
            }
            int nodeCount = instanceGroup.getNodeCount() - nodesRemoved;
            LOGGER.debug("Successfully terminated metadata of instances '{}' in stack.", instanceIds);
            eventService.fireCloudbreakEvent(stack.getId(), BillingStatus.BILLING_CHANGED.name(),
                    cloudbreakMessagesService.getMessage(Msg.STACK_SCALING_BILLING_CHANGED.code()));
            return nodeCount;
        });
    }

    public Map<String, String> getUnusedInstanceIds(String instanceGroupName, Integer scalingAdjustment, Stack stack) {
        Map<String, String> instanceIds = new HashMap<>();
        int i = 0;
        List<InstanceMetaData> instanceMetaDatas = new ArrayList<>(stack.getInstanceGroupByInstanceGroupName(instanceGroupName)
                .getNotDeletedInstanceMetaDataSet());
        instanceMetaDatas.sort(Comparator.comparing(InstanceMetaData::getStartDate));
        for (InstanceMetaData metaData : instanceMetaDatas) {
            if (!metaData.getAmbariServer()
                    && (metaData.isDecommissioned() || metaData.isUnRegistered() || metaData.isCreated() || metaData.isFailed())) {
                instanceIds.put(metaData.getInstanceId(), metaData.getDiscoveryFQDN());
                if (++i >= scalingAdjustment * -1) {
                    break;
                }
            }
        }
        return instanceIds;
    }

}

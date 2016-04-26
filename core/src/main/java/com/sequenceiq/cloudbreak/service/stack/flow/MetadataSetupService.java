package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.common.type.BillingStatus;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.model.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderMetadataAdapter;

@Service
public class MetadataSetupService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataSetupService.class);

    @Inject
    private ServiceProviderMetadataAdapter metadata;

    @Inject
    private InstanceGroupRepository instanceGroupRepository;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Inject
    private StackService stackService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    private enum Msg {
        STACK_METADATA_SETUP_UPSCALING_BILLING_CHANGED("stack.metadata.setup.billing.changed");

        private String code;

        Msg(String msgCode) {
            code = msgCode;
        }

        public String code() {
            return code;
        }
    }

    public void collectMetadata(Stack stack) {
        saveInstanceMetaData(stack, collectCoreMetadata(stack), null);
    }

    public Set<String> setupNewMetadata(Stack stack, String instanceGroupName) {
        clusterService.updateClusterStatusByStackId(stack.getId(), UPDATE_IN_PROGRESS);
        List<CloudVmMetaDataStatus> coreInstanceMetaData = metadata.collectNewMetadata(stack);
        saveInstanceMetaData(stack, coreInstanceMetaData, InstanceStatus.CREATED);
        Set<String> upscaleCandidateAddresses = new HashSet<>();
        for (CloudVmMetaDataStatus cloudVmMetaDataStatus : coreInstanceMetaData) {
            upscaleCandidateAddresses.add(cloudVmMetaDataStatus.getMetaData().getPrivateIp());
        }
        InstanceGroup instanceGroup = instanceGroupRepository.findOneByGroupNameInStack(stack.getId(), instanceGroupName);
        int nodeCount = instanceGroup.getNodeCount() + coreInstanceMetaData.size();
        instanceGroup.setNodeCount(nodeCount);
        instanceGroupRepository.save(instanceGroup);
        clusterService.updateClusterStatusByStackId(stack.getId(), AVAILABLE);
        eventService.fireCloudbreakEvent(stack.getId(), BillingStatus.BILLING_CHANGED.name(),
                cloudbreakMessagesService.getMessage(Msg.STACK_METADATA_SETUP_UPSCALING_BILLING_CHANGED.code));
        return upscaleCandidateAddresses;
    }

    public Set<InstanceMetaData> saveInstanceMetaData(Stack stack, List<CloudVmMetaDataStatus>  cloudVmMetaDataStatusList, InstanceStatus status) {
        Boolean ambariServerFound = false;
        Set<InstanceMetaData> updatedInstanceMetadata = new HashSet<>();
        Set<InstanceMetaData> allInstanceMetadata = instanceMetaDataRepository.findNotTerminatedForStack(stack.getId());
        for (CloudVmMetaDataStatus cloudVmMetaDataStatus : cloudVmMetaDataStatusList) {
            CloudInstance cloudInstance = cloudVmMetaDataStatus.getCloudVmInstanceStatus().getCloudInstance();
            CloudInstanceMetaData md = cloudVmMetaDataStatus.getMetaData();
            long timeInMillis = Calendar.getInstance().getTimeInMillis();
            Long privateId = cloudInstance.getTemplate().getPrivateId();
            String instanceId = cloudInstance.getInstanceId();
            InstanceMetaData instanceMetaDataEntry = createInstanceMetadataIfAbsent(allInstanceMetadata, privateId, instanceId);
            // CB 1.0.x clusters do not have private id thus we cannot correlate them with instance groups thus keep the original one
            String group = instanceMetaDataEntry.getInstanceGroup() == null ? cloudInstance.getTemplate().getGroupName() : instanceMetaDataEntry
                    .getInstanceGroup().getGroupName();
            InstanceGroup instanceGroup = instanceGroupRepository.findOneByGroupNameInStack(stack.getId(), group);
            instanceMetaDataEntry.setPrivateIp(md.getPrivateIp());
            instanceMetaDataEntry.setPublicIp(md.getPublicIp());
            instanceMetaDataEntry.setSshPort(md.getSshPort());
            instanceMetaDataEntry.setHypervisor(md.getHypervisor());
            instanceMetaDataEntry.setInstanceGroup(instanceGroup);
            instanceMetaDataEntry.setInstanceId(instanceId);
            instanceMetaDataEntry.setPrivateId(privateId);
            instanceMetaDataEntry.setStartDate(timeInMillis);
            if (!ambariServerFound && InstanceGroupType.GATEWAY.equals(instanceGroup.getInstanceGroupType())) {
                instanceMetaDataEntry.setAmbariServer(Boolean.TRUE);
                ambariServerFound = true;
            } else {
                instanceMetaDataEntry.setAmbariServer(Boolean.FALSE);
            }
            if (status != null) {
                instanceMetaDataEntry.setInstanceStatus(status);
            }
            instanceMetaDataRepository.save(instanceMetaDataEntry);
            updatedInstanceMetadata.add(instanceMetaDataEntry);
        }
        return updatedInstanceMetadata;
    }

    private List<CloudVmMetaDataStatus> collectCoreMetadata(Stack stack) {
        List<CloudVmMetaDataStatus> coreInstanceMetaData = metadata.collectMetadata(stack);
        if (coreInstanceMetaData.size() != stack.getFullNodeCount()) {
            throw new WrongMetadataException(String.format(
                    "Size of the collected metadata set does not equal the node count of the stack. [metadata size=%s] [nodecount=%s]",
                    coreInstanceMetaData.size(), stack.getFullNodeCount()));
        }
        return coreInstanceMetaData;
    }

    private InstanceMetaData createInstanceMetadataIfAbsent(Set<InstanceMetaData> allInstanceMetadata, Long privateId, String instanceId) {
        if (privateId != null) {
            for (InstanceMetaData instanceMetaData : allInstanceMetadata) {
                if (Objects.equals(instanceMetaData.getPrivateId(), privateId)) {
                    return instanceMetaData;
                }
            }
        } else {
            for (InstanceMetaData instanceMetaData : allInstanceMetadata) {
                if (Objects.equals(instanceMetaData.getInstanceId(), instanceId)) {
                    return instanceMetaData;
                }
            }
        }
        return new InstanceMetaData();
    }

}

package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.CREATED;
import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.TERMINATED;

import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.image.ImageService;

@Service
public class MetadataSetupService {
    @Inject
    private ImageService imageService;

    @Inject
    private InstanceGroupRepository instanceGroupRepository;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    public void cleanupRequestedInstances(Stack stack, String instanceGroupName) {
        InstanceGroup ig = instanceGroupRepository.findOneByGroupNameInStackWithInstanceMetadata(stack.getId(), instanceGroupName);
        List<InstanceMetaData> requestedInstances = instanceMetaDataRepository.findAllByInstanceGroupAndInstanceStatus(ig, InstanceStatus.REQUESTED);
        for (InstanceMetaData inst : requestedInstances) {
            inst.setInstanceStatus(InstanceStatus.TERMINATED);
        }
        instanceMetaDataRepository.saveAll(requestedInstances);
    }

    public int saveInstanceMetaData(Stack stack, Iterable<CloudVmMetaDataStatus> cloudVmMetaDataStatusList, InstanceStatus status) {
        try {
            int newInstances = 0;
            Set<InstanceMetaData> allInstanceMetadata = instanceMetaDataRepository.findNotTerminatedForStack(stack.getId());
            boolean primaryIgSelected = allInstanceMetadata.stream().anyMatch(imd -> imd.getInstanceMetadataType() == InstanceMetadataType.GATEWAY_PRIMARY);
            Json imageJson = new Json(imageService.getImage(stack.getId()));

            Set<InstanceGroup> instanceGroupsForStack = instanceGroupRepository.findByStackId(stack.getId());

            for (CloudVmMetaDataStatus cloudVmMetaDataStatus : cloudVmMetaDataStatusList) {
                CloudInstance cloudInstance = cloudVmMetaDataStatus.getCloudVmInstanceStatus().getCloudInstance();
                CloudInstanceMetaData md = cloudVmMetaDataStatus.getMetaData();
                long timeInMillis = Calendar.getInstance().getTimeInMillis();
                Long privateId = cloudInstance.getTemplate().getPrivateId();
                String instanceId = cloudInstance.getInstanceId();
                InstanceMetaData instanceMetaDataEntry = createInstanceMetadataIfAbsent(allInstanceMetadata, privateId, instanceId);
                if (instanceMetaDataEntry.getInstanceId() == null && cloudVmMetaDataStatus.getCloudVmInstanceStatus().getStatus() == CREATED) {
                    newInstances++;
                }
                // CB 1.0.x clusters do not have private id thus we cannot correlate them with instance groups thus keep the original one
                InstanceGroup ig = instanceMetaDataEntry.getInstanceGroup();
                String group = ig == null ? cloudInstance.getTemplate().getGroupName() : ig.getGroupName();
                instanceGroupsForStack.stream()
                        .filter(instanceGroup -> group.equals(instanceGroup.getGroupName()))
                        .findFirst()
                        .ifPresentOrElse(instanceMetaDataEntry::setInstanceGroup, () -> {
                            throw new CloudbreakServiceException("Can not find instance group " + group + " in " + stack.getName() + "'s hostgroups");
                        });

                instanceMetaDataEntry.setPrivateIp(md.getPrivateIp());
                instanceMetaDataEntry.setPublicIp(md.getPublicIp());
                instanceMetaDataEntry.setSshPort(md.getSshPort());
                instanceMetaDataEntry.setLocalityIndicator(md.getLocalityIndicator());
                instanceMetaDataEntry.setInstanceId(instanceId);
                instanceMetaDataEntry.setPrivateId(privateId);
                instanceMetaDataEntry.setStartDate(timeInMillis);
                instanceMetaDataEntry.setSubnetId(cloudInstance.getStringParameter(CloudInstance.SUBNET_ID));
                instanceMetaDataEntry.setInstanceName(cloudInstance.getStringParameter(CloudInstance.INSTANCE_NAME));
                instanceMetaDataEntry.setAmbariServer(Boolean.FALSE);
                if (instanceMetaDataEntry.getInstanceMetadataType() == null) {
                    if (ig != null) {
                        if (InstanceGroupType.GATEWAY.equals(ig.getInstanceGroupType())) {
                            if (!primaryIgSelected) {
                                primaryIgSelected = true;
                                instanceMetaDataEntry.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
                                instanceMetaDataEntry.setAmbariServer(Boolean.TRUE);
                            } else {
                                instanceMetaDataEntry.setInstanceMetadataType(InstanceMetadataType.GATEWAY);
                            }
                        } else {
                            instanceMetaDataEntry.setInstanceMetadataType(InstanceMetadataType.CORE);
                        }
                    } else {
                        instanceMetaDataEntry.setInstanceMetadataType(InstanceMetadataType.CORE);
                    }
                }
                if (status != null) {
                    if (cloudVmMetaDataStatus.getCloudVmInstanceStatus().getStatus() == TERMINATED) {
                        instanceMetaDataEntry.setInstanceStatus(InstanceStatus.TERMINATED);
                    } else {
                        instanceMetaDataEntry.setInstanceStatus(status);
                        instanceMetaDataEntry.setImage(imageJson);
                    }
                }
                instanceMetaDataRepository.save(instanceMetaDataEntry);
            }
            return newInstances;
        } catch (CloudbreakImageNotFoundException | JsonProcessingException ex) {
            throw new CloudbreakServiceException("Instance metadata collection failed", ex);
        }
    }

    private InstanceMetaData createInstanceMetadataIfAbsent(Iterable<InstanceMetaData> allInstanceMetadata, Long privateId, String instanceId) {
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

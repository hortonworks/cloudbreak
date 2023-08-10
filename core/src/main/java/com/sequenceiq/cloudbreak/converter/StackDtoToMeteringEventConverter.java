package com.sequenceiq.cloudbreak.converter;

import static com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.ClusterStatus;
import static com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.ServiceType.Value.DATAHUB;
import static com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.Sync;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.InstanceResource;
import com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.MeteringEvent;
import com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.Resource;
import com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.StatusChange;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Component
public class StackDtoToMeteringEventConverter {

    private static final int DEFAULT_VERSION = 1;

    public MeteringEvent convertToSyncEvent(StackDtoDelegate stack) {
        MeteringEvent.Builder builder = convertBase(stack);
        return builder.setSync(convertSync(stack)).build();
    }

    public MeteringEvent convertToStatusChangeEvent(StackDtoDelegate stack, ClusterStatus.Value eventOperation) {
        MeteringEvent.Builder builder = convertBase(stack);
        return builder.setStatusChange(convertStatusChange(stack, eventOperation)).build();
    }

    private Sync convertSync(StackDtoDelegate stack) {
        return Sync.newBuilder()
                .addAllResources(convertResources(stack))
                .build();
    }

    private List<Resource> convertResources(StackDtoDelegate stack) {
        return stack.getInstanceGroupDtos().stream().flatMap(this::convertInstanceGroup).toList();
    }

    private Stream<Resource> convertInstanceGroup(InstanceGroupDto instanceGroup) {
        return instanceGroup.getNotDeletedInstanceMetaData().stream()
                .filter(InstanceMetadataView::isRunning)
                .map(instanceMetadata -> convertInstance(instanceMetadata, instanceGroup.getInstanceGroup().getTemplate().getInstanceType()));
    }

    private Resource convertInstance(InstanceMetadataView instanceMetadata, String instanceType) {
        return Resource.newBuilder()
                .setId(instanceMetadata.getInstanceId())
                .setInstanceResource(InstanceResource.newBuilder()
                        .setIpAddress(StringUtils.isNotEmpty(instanceMetadata.getPublicIp()) ? instanceMetadata.getPublicIp() : instanceMetadata.getPrivateIp())
                        .setInstanceType(instanceType)
                        .build())
                .build();
    }

    private StatusChange convertStatusChange(StackDtoDelegate stack, ClusterStatus.Value eventOperation) {
        return StatusChange.newBuilder()
                .setStatus(eventOperation)
                .addAllResources(convertResources(stack))
                .build();
    }

    private MeteringEvent.Builder convertBase(StackDtoDelegate stack) {
        return MeteringEvent.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setTimestamp(System.currentTimeMillis())
                .setVersion(DEFAULT_VERSION)
                .setServiceType(DATAHUB)
                .setResourceCrn(stack.getResourceCrn())
                .setEnvironmentCrn(stack.getEnvironmentCrn());
    }
}

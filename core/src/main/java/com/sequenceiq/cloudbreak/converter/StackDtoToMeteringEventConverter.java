package com.sequenceiq.cloudbreak.converter;

import static com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.ClusterStatus;
import static com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.ServiceType.Value.DATAHUB;
import static com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.Sync;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.InstanceResource;
import com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.MeteringEvent;
import com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.Resource;
import com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.ServiceType;
import com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.StatusChange;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.tag.ClusterTemplateApplicationTag;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.common.model.DefaultApplicationTag;

@Component
public class StackDtoToMeteringEventConverter {

    public static final String CLOUDERA_EXTERNAL_RESOURCE_NAME_TAG = "Cloudera-External-Resource-Name";

    public static final Set<InstanceStatus> REPORTING_INSTANCE_STATUSES = Set.of(InstanceStatus.SERVICES_RUNNING, InstanceStatus.SERVICES_HEALTHY,
            InstanceStatus.SERVICES_UNHEALTHY);

    private static final Logger LOGGER = LoggerFactory.getLogger(StackDtoToMeteringEventConverter.class);

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
                .filter(instanceMetadata -> REPORTING_INSTANCE_STATUSES.contains(instanceMetadata.getInstanceStatus()))
                .map(instanceMetadata -> convertInstance(instanceMetadata, instanceGroup.getInstanceGroup().getTemplate().getInstanceType()));
    }

    private Resource convertInstance(InstanceMetadataView instanceMetadata, String templateInstanceType) {
        return Resource.newBuilder()
                .setId(instanceMetadata.getInstanceId())
                .setInstanceResource(InstanceResource.newBuilder()
                        .setIpAddress(StringUtils.isNotEmpty(instanceMetadata.getPublicIp()) ? instanceMetadata.getPublicIp() : instanceMetadata.getPrivateIp())
                        .setInstanceType(getInstanceType(instanceMetadata, templateInstanceType))
                        .build())
                .build();
    }

    private String getInstanceType(InstanceMetadataView instanceMetadata, String templateInstanceType) {
        return StringUtils.isNotEmpty(instanceMetadata.getProviderInstanceType()) ? instanceMetadata.getProviderInstanceType() : templateInstanceType;
    }

    private StatusChange convertStatusChange(StackDtoDelegate stack, ClusterStatus.Value eventOperation) {
        return StatusChange.newBuilder()
                .setStatus(eventOperation)
                .addAllResources(convertResources(stack))
                .build();
    }

    private MeteringEvent.Builder convertBase(StackDtoDelegate stack) {
        StackTags stackTags = getStackTags(stack);
        Map<String, String> applicationTags = getApplicationTags(stackTags);
        Map<String, String> defaultTags = getDefaultTags(stackTags);
        return MeteringEvent.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setTimestamp(System.currentTimeMillis())
                .setVersion(DEFAULT_VERSION)
                .setServiceType(getServiceType(applicationTags, DATAHUB))
                .setServiceFeature(getServiceFeature(applicationTags))
                .setResourceCrn(getResourceCrn(stack, applicationTags, defaultTags))
                .setEnvironmentCrn(stack.getEnvironmentCrn());
    }

    private ServiceType.Value getServiceType(Map<String, String> applicationTags, ServiceType.Value defaultServiceType) {
        String serviceTypeName = applicationTags.get(ClusterTemplateApplicationTag.SERVICE_TYPE.key());
        return EnumUtils.getEnum(ServiceType.Value.class, serviceTypeName, defaultServiceType);
    }

    private String getServiceFeature(Map<String, String> applicationTags) {
        String serviceFeature = applicationTags.get(ClusterTemplateApplicationTag.SERVICE_FEATURE.key());
        return serviceFeature != null ? serviceFeature : StringUtils.EMPTY;
    }

    private String getResourceCrn(StackDtoDelegate stack, Map<String, String> applicationTags, Map<String, String> defaultTags) {
        String clouderaExternalResourceNameTag = applicationTags.get(CLOUDERA_EXTERNAL_RESOURCE_NAME_TAG);
        String clouderaExternalResourceNameTagLowerCase = applicationTags.get(CLOUDERA_EXTERNAL_RESOURCE_NAME_TAG.toLowerCase(Locale.ROOT));
        String clouderaResourceName = defaultTags.get(DefaultApplicationTag.RESOURCE_CRN.key());
        if (StringUtils.isNotEmpty(clouderaExternalResourceNameTag)) {
            return clouderaExternalResourceNameTag;
        } else if (StringUtils.isNotEmpty(clouderaExternalResourceNameTagLowerCase)) {
            return clouderaExternalResourceNameTagLowerCase;
        } else if (StringUtils.isNotEmpty(clouderaResourceName)) {
            return clouderaResourceName;
        } else {
            return stack.getResourceCrn();
        }
    }

    private Map<String, String> getApplicationTags(StackTags stackTags) {
        return stackTags != null && stackTags.getApplicationTags() != null ? stackTags.getApplicationTags() : new HashMap<>();
    }

    private Map<String, String> getDefaultTags(StackTags stackTags) {
        return stackTags != null && stackTags.getDefaultTags() != null ? stackTags.getDefaultTags() : new HashMap<>();
    }

    private StackTags getStackTags(StackDtoDelegate stack) {
        if (stack.getTags() != null) {
            try {
                return stack.getTags().get(StackTags.class);
            } catch (IOException e) {
                LOGGER.warn("Stack related tags cannot be parsed.", e);
            }
        }
        return null;
    }
}

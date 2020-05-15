package com.sequenceiq.cloudbreak.converter.spi;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.converter.InstanceMetadataToImageIdConverter;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

@Component
public class InstanceMetaDataToCloudInstanceConverter extends AbstractConversionServiceAwareConverter<InstanceMetaData, CloudInstance> {

    @Inject
    private StackToCloudStackConverter stackToCloudStackConverter;

    @Inject
    private InstanceMetadataToImageIdConverter instanceMetadataToImageIdConverter;

    @Override
    public CloudInstance convert(InstanceMetaData metaDataEntity) {
        InstanceGroup group = metaDataEntity.getInstanceGroup();
        Template template = group.getTemplate();
        Stack stack = group.getStack();
        StackAuthentication stackAuthentication = stack.getStackAuthentication();
        InstanceStatus status = getInstanceStatus(metaDataEntity);
        String imageId = instanceMetadataToImageIdConverter.convert(metaDataEntity);
        InstanceTemplate instanceTemplate = stackToCloudStackConverter.buildInstanceTemplate(template, group.getGroupName(), metaDataEntity.getPrivateId(),
                status, imageId);
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication(
                stackAuthentication.getPublicKey(),
                stackAuthentication.getPublicKeyId(),
                stackAuthentication.getLoginUserName());
        Map<String, Object> params = new HashMap<>();
        params.put(CloudInstance.SUBNET_ID, metaDataEntity.getSubnetId());
        params.put(CloudInstance.INSTANCE_NAME, metaDataEntity.getInstanceName());

        Map<String, Object> cloudInstanceParameters = stackToCloudStackConverter.buildCloudInstanceParameters(
                stack.getEnvironmentCrn(),
                metaDataEntity,
                CloudPlatform.valueOf(stack.getCloudPlatform()));
        params.putAll(cloudInstanceParameters);

        return new CloudInstance(metaDataEntity.getInstanceId(), instanceTemplate, instanceAuthentication, params);
    }

    private InstanceStatus getInstanceStatus(InstanceMetaData metaData) {
        switch (metaData.getInstanceStatus()) {

            case REQUESTED:
                return InstanceStatus.CREATE_REQUESTED;
            case CREATED:
                return InstanceStatus.CREATED;
            case SERVICES_RUNNING:
            case SERVICES_HEALTHY:
            case SERVICES_UNHEALTHY:
                return InstanceStatus.STARTED;
            case DECOMMISSIONED:
            case DELETE_REQUESTED:
                return InstanceStatus.DELETE_REQUESTED;
            case TERMINATED:
                return InstanceStatus.TERMINATED;
            case DELETED_BY_PROVIDER:
                return InstanceStatus.TERMINATED_BY_PROVIDER;
            default:
                return InstanceStatus.UNKNOWN;
        }
    }

}

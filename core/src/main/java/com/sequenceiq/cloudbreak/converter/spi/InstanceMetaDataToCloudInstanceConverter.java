package com.sequenceiq.cloudbreak.converter.spi;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.converter.InstanceMetadataToImageIdConverter;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

@Component
public class InstanceMetaDataToCloudInstanceConverter extends AbstractConversionServiceAwareConverter<InstanceMetaData, CloudInstance> {

    @Inject
    private StackToCloudStackConverter stackToCloudStackConverter;

    @Inject
    private InstanceMetadataToImageIdConverter instanceMetadataToImageIdConverter;

    @Override
    public CloudInstance convert(InstanceMetaData metaDataEnity) {
        InstanceGroup group = metaDataEnity.getInstanceGroup();
        Template template = metaDataEnity.getInstanceGroup().getTemplate();
        StackAuthentication stackAuthentication = group.getStack().getStackAuthentication();
        InstanceStatus status = getInstanceStatus(metaDataEnity);
        String imageId = instanceMetadataToImageIdConverter.convert(metaDataEnity);
        InstanceTemplate instanceTemplate = stackToCloudStackConverter.buildInstanceTemplate(template, group.getGroupName(), metaDataEnity.getPrivateId(),
                status, imageId);
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication(
                stackAuthentication.getPublicKey(),
                stackAuthentication.getPublicKeyId(),
                stackAuthentication.getLoginUserName());
        Map<String, Object> params = new HashMap<>();
        params.put(CloudInstance.SUBNET_ID, metaDataEnity.getSubnetId());
        params.put(CloudInstance.INSTANCE_NAME, metaDataEnity.getInstanceName());
        return new CloudInstance(metaDataEnity.getInstanceId(), instanceTemplate, instanceAuthentication, params);
    }

    private InstanceStatus getInstanceStatus(InstanceMetaData metaData) {
        switch (metaData.getInstanceStatus()) {

            case REQUESTED:
                return InstanceStatus.CREATE_REQUESTED;
            case CREATED:
                return InstanceStatus.CREATED;
            case UNREGISTERED:
            case REGISTERED:
                return InstanceStatus.STARTED;
            case DECOMMISSIONED:
                return InstanceStatus.DELETE_REQUESTED;
            case TERMINATED:
                return InstanceStatus.TERMINATED;
            default:
                return InstanceStatus.UNKNOWN;
        }
    }

}

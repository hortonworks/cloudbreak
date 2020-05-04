package com.sequenceiq.freeipa.converter.cloud;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.StackAuthentication;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.entity.projection.StackAuthenticationView;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@Component
public class InstanceMetaDataToCloudInstanceConverter extends AbstractConversionServiceAwareConverter<InstanceMetaData, CloudInstance> {

    @Inject
    private StackToCloudStackConverter stackToCloudStackConverter;

    @Inject
    private ImageService imageService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Override
    public CloudInstance convert(InstanceMetaData metaDataEnity) {
        InstanceGroup group = metaDataEnity.getInstanceGroup();
        Optional<StackAuthenticationView> stackAuthenticationView = instanceMetaDataService
                .getStackAuthenticationViewByInstanceMetaDataId(metaDataEnity.getId());
        Template template = metaDataEnity.getInstanceGroup().getTemplate();
        Optional<StackAuthentication> stackAuthentication = stackAuthenticationView.map(StackAuthenticationView::getStackAuthentication);
        InstanceStatus status = getInstanceStatus(metaDataEnity);
        String imageId = stackAuthenticationView
                .map(StackAuthenticationView::getStackId)
                .map(stackId -> imageService.getByStackId(stackId))
                .map(ImageEntity::getImageName)
                .orElse(null);
        InstanceTemplate instanceTemplate = stackToCloudStackConverter.buildInstanceTemplate(template, group.getGroupName(), metaDataEnity.getPrivateId(),
                status, imageId);
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication(
                stackAuthentication.map(StackAuthentication::getPublicKey).orElse(null),
                stackAuthentication.map(StackAuthentication::getPublicKeyId).orElse(null),
                stackAuthentication.map(StackAuthentication::getLoginUserName).orElse(null));
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

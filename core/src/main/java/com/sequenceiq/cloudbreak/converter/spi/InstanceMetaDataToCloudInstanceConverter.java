package com.sequenceiq.cloudbreak.converter.spi;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AZURE;

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
import com.sequenceiq.cloudbreak.converter.InstanceMetadataToImageIdConverter;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

@Component
public class InstanceMetaDataToCloudInstanceConverter extends AbstractConversionServiceAwareConverter<InstanceMetaData, CloudInstance> {

    private static final String RESOURCE_GROUP_NAME = "resourceGroupName";

    @Inject
    private StackToCloudStackConverter stackToCloudStackConverter;

    @Inject
    private InstanceMetadataToImageIdConverter instanceMetadataToImageIdConverter;

    @Override
    public CloudInstance convert(InstanceMetaData metaDataEntity) {
        InstanceGroup group = metaDataEntity.getInstanceGroup();
        Template template = metaDataEntity.getInstanceGroup().getTemplate();
        StackAuthentication stackAuthentication = group.getStack().getStackAuthentication();
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
        fillCloudSpecificParameters(group.getStack().getPlatformVariant(), params, metaDataEntity);

        return new CloudInstance(metaDataEntity.getInstanceId(), instanceTemplate, instanceAuthentication, params);
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

    private void fillCloudSpecificParameters(String cloudPlatform, Map<String, Object> parameters, InstanceMetaData instanceMetaData) {
        if (AZURE.equals(cloudPlatform)) {
            Optional.ofNullable(instanceMetaData.getInstanceGroup().getStack().getParameters())
                    .map(params -> params.get(RESOURCE_GROUP_NAME))
                    .ifPresent(rgName -> parameters.put(RESOURCE_GROUP_NAME, rgName));
        }
    }

}

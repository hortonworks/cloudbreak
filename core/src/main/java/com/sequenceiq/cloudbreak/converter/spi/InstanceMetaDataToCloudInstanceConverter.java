package com.sequenceiq.cloudbreak.converter.spi;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Template;

@Component
public class InstanceMetaDataToCloudInstanceConverter extends AbstractConversionServiceAwareConverter<InstanceMetaData, CloudInstance> {

    @Inject
    private StackToCloudStackConverter stackToCloudStackConverter;

    @Override
    public CloudInstance convert(InstanceMetaData metaDataEnity) {
        InstanceGroup group = metaDataEnity.getInstanceGroup();
        Template template = metaDataEnity.getInstanceGroup().getTemplate();
        InstanceStatus status = getInstanceStatus(metaDataEnity);
        InstanceTemplate instanceTemplate = stackToCloudStackConverter.buildInstanceTemplate(
                template, group.getGroupName(), metaDataEnity.getPrivateId(), status);
        return new CloudInstance(metaDataEnity.getInstanceId(), instanceTemplate);
    }

    private InstanceStatus getInstanceStatus(InstanceMetaData metaData) {
        switch (metaData.getInstanceStatus()) {

            case REQUESTED:
                return InstanceStatus.CREATE_REQUESTED;
            case CREATED:
                return InstanceStatus.CREATED;
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

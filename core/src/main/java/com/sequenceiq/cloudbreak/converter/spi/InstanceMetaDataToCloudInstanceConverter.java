package com.sequenceiq.cloudbreak.converter.spi;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
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
    public CloudInstance convert(InstanceMetaData metaData) {
        InstanceGroup group = metaData.getInstanceGroup();
        Template template = metaData.getInstanceGroup().getTemplate();
        InstanceStatus status = getInstanceStatus(metaData);
        InstanceTemplate instance = stackToCloudStackConverter.buildInstanceTemplate(
                template, group.getGroupName(), metaData.getPrivateId(), status);
        CloudInstanceMetaData md = new CloudInstanceMetaData(metaData.getPrivateIp(), metaData.getPublicIp());
        return new CloudInstance(metaData.getInstanceId(), md, instance);
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

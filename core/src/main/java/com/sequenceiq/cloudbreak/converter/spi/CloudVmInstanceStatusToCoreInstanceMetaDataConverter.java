package com.sequenceiq.cloudbreak.converter.spi;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.service.stack.flow.CoreInstanceMetaData;

@Component
public class CloudVmInstanceStatusToCoreInstanceMetaDataConverter extends AbstractConversionServiceAwareConverter<CloudVmInstanceStatus, CoreInstanceMetaData> {

    @Override
    public CoreInstanceMetaData convert(CloudVmInstanceStatus cloudVmInstanceStatus) {
        CloudInstance cloudInstance = cloudVmInstanceStatus.getCloudInstance();
        InstanceTemplate template = cloudInstance.getTemplate();

        return new CoreInstanceMetaData(cloudInstance.getInstanceId(),
                cloudInstance.getMetaData().getPrivateIp(), cloudInstance.getMetaData().getPublicIp(),
                template.getVolumes().size(), template.getGroupName());

    }

}
